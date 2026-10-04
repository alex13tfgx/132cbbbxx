/*
 * Автоматические вейпоинты ивентов: ловит сообщения сервера об ивенте с координатами
 * (аирдроп, мистический сундук, маяк, вулкан и т.д.), ставит большую метку в мире
 * и стрелку-направление над хотбаром — как у .gps.
 */
package code.wiosvisual.features.impl.render;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.commands.defaults.GpsCommand;
import code.wiosvisual.common.repository.way.WayRepository;
import code.wiosvisual.display.hud.HudTheme;
import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.events.render.DrawEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.chat.ChatMessage;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import code.wiosvisual.utils.math.projection.Projection;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.math.Vec3d;

public class EventWaypoints
extends Module {
    private static final int MAX_POINTS = 6;
    private static final double MERGE_DISTANCE = 16.0;

    /** X Y Z с подписями: «X: 120 Y: 64 Z: -300», «x=120, y=64, z=-300». Поддерживается кириллическая «Х». */
    private static final Pattern LABELED_XYZ = Pattern.compile("[xх]\\s*[:=]?\\s*(-?\\d{1,7})[^\\d-]{1,8}?[yу]\\s*[:=]?\\s*(-?\\d{1,3})[^\\d-]{1,8}?[zз]\\s*[:=]?\\s*(-?\\d{1,7})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    /** X Z с подписями, без высоты. */
    private static final Pattern LABELED_XZ = Pattern.compile("[xх]\\s*[:=]?\\s*(-?\\d{1,7})[^\\d-]{1,8}?[zз]\\s*[:=]?\\s*(-?\\d{1,7})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    /** Три числа подряд: «120 64 -300», «120, 64, -300», «[120 / 64 / -300]». */
    private static final Pattern PLAIN_XYZ = Pattern.compile("(?<![\\d.:])(-?\\d{1,7})[\\s,;/|]+(-?\\d{1,3})[\\s,;/|]+(-?\\d{1,7})(?![\\d.:%])");

    /** Ключевые слова → название метки. Порядок важен: более конкретные выше. */
    private static final String[][] EVENT_NAMES = {
            {"мистическ", "Мистический сундук"},
            {"аирдроп", "Аирдроп"}, {"эйрдроп", "Аирдроп"}, {"airdrop", "Аирдроп"},
            {"осколк", "Осколки"}, {"shard", "Осколки"},
            {"талисман", "Талисман"}, {"talisman", "Талисман"},
            {"маяк", "Маяк"}, {"вулкан", "Вулкан"}, {"метеор", "Метеорит"},
            {"босс", "Босс"}, {"boss", "Босс"}, {"шахт", "Шахта"},
            {"сундук", "Сундук"}, {"chest", "Сундук"}, {"клад", "Клад"},
            {"ивент", "Ивент"}, {"эвент", "Ивент"}, {"event", "Ивент"},
            {"координат", "Точка"}, {"корд", "Точка"}};

    private final BooleanSetting onlyEvents = new BooleanSetting("Только ивенты", "Ставить метку только если в сообщении есть слово про ивент").setValue(true);
    private final BooleanSetting notify = new BooleanSetting("Сообщение в чат", "Писать в чат, когда найдена новая точка ивента").setValue(true);
    private final BooleanSetting removeOnArrive = new BooleanSetting("Удалять при прибытии", "Убирать метку, когда вы дошли до ивента").setValue(true);
    private final SliderSettings lifetime = new SliderSettings("Время жизни", "Через сколько минут метка пропадёт сама").range(1.0f, 30.0f).step(1.0f).setValue(10.0f);

    private final List<EventPoint> points = new CopyOnWriteArrayList<>();
    private String server;

    public EventWaypoints() {
        super("EventWaypoints", "Event Waypoints", ModuleCategory.RENDER);
        this.setup(this.onlyEvents, this.notify, this.removeOnArrive, this.lifetime);
    }

    @Override
    public void deactivate() {
        this.points.clear();
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (!(e.getPacket() instanceof GameMessageS2CPacket packet)) {
            return;
        }
        String message = packet.content().getString().replaceAll("§.", "");
        this.handleMessage(message);
    }

    private void handleMessage(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        String name = null;
        for (String[] entry : EVENT_NAMES) {
            if (lower.contains(entry[0])) {
                name = entry[1];
                break;
            }
        }
        if (name == null) {
            if (this.onlyEvents.isValue()) {
                return;
            }
            name = "Точка";
        }
        double x;
        double y = 0.0;
        double z;
        boolean flat = false;
        Matcher m = LABELED_XYZ.matcher(message);
        if (m.find()) {
            x = Double.parseDouble(m.group(1));
            y = Double.parseDouble(m.group(2));
            z = Double.parseDouble(m.group(3));
        } else if ((m = PLAIN_XYZ.matcher(message)).find() && Math.abs(Double.parseDouble(m.group(2))) <= 320.0) {
            x = Double.parseDouble(m.group(1));
            y = Double.parseDouble(m.group(2));
            z = Double.parseDouble(m.group(3));
        } else if ((m = LABELED_XZ.matcher(message)).find()) {
            x = Double.parseDouble(m.group(1));
            z = Double.parseDouble(m.group(2));
            flat = true;
        } else {
            return;
        }
        this.server = WayRepository.currentServer();
        Vec3d pos = new Vec3d(Math.floor(x) + 0.5, y, Math.floor(z) + 0.5);
        long expireAt = System.currentTimeMillis() + (long)(this.lifetime.getValue() * 60_000.0f);
        boolean known = false;
        for (EventPoint p : this.points) {
            if (Math.hypot(p.pos.x - pos.x, p.pos.z - pos.z) <= MERGE_DISTANCE) {
                this.points.remove(p);
                known = true;
            }
        }
        this.points.add(new EventPoint(name, pos, flat, expireAt));
        while (this.points.size() > MAX_POINTS) {
            this.points.remove(0);
        }
        if (this.notify.isValue() && !known) {
            ChatMessage.brandmessage(name + ": " + (long)Math.floor(x) + (flat ? "" : " " + (long)y) + " " + (long)Math.floor(z) + " — метка поставлена");
        }
    }

    private double distance(EventPoint p) {
        Vec3d pos = new net.minecraft.util.math.Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getRandomBodyY());
        double dx = p.pos.x - pos.x;
        double dz = p.pos.z - pos.z;
        double dy = p.flat ? 0.0 : p.pos.y - pos.y;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private Vec3d resolved(EventPoint p) {
        return p.flat ? new Vec3d(p.pos.x, new net.minecraft.util.math.Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getRandomBodyY()).y + 1.0, p.pos.z) : p.pos;
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        String current = WayRepository.currentServer();
        if (!current.equals(this.server)) {
            this.server = current;
            this.points.clear();
        }
        long now = System.currentTimeMillis();
        EventPoint nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (EventPoint p : this.points) {
            double d = this.distance(p);
            if (now > p.expireAt || this.removeOnArrive.isValue() && d <= 4.0) {
                this.points.remove(p);
                continue;
            }
            if (d < nearestDist) {
                nearestDist = d;
                nearest = p;
            }
        }
        if (nearest == null) {
            return;
        }
        DrawContext context = e.getDrawContext();
        double[] screen = new double[3];
        float guiScale = (float)mc.getWindow().getScaleFactor();
        // Виртуальные координаты оверлея mentions: кадр делится на 2 (как у InterFace).
        float screenW = mc.getWindow().getFramebufferWidth() / 2.0f;
        float screenH = mc.getWindow().getFramebufferHeight() / 2.0f;
        context.createNewRootLayer();
        ru.mentions.utils.render.Render2D.beginOverlay();
        context.getMatrices().pushMatrix();
        try {
            for (EventPoint p : this.points) {
                Vec3d point = this.resolved(p);
                double dist = this.distance(p);
                boolean main = p == nearest;
                boolean visible = Projection.isInFrontOfCamera(point) && Projection.worldSpaceToScreenSpaceInto(point.x, point.y, point.z, screen);
                float vx = visible ? (float)screen[0] * guiScale / 2.0f : 0.0f;
                float vy = visible ? (float)screen[1] * guiScale / 2.0f : 0.0f;
                if (visible && vx > 12.0f && vx < screenW - 12.0f && vy > 40.0f && vy < screenH - 12.0f) {
                    this.drawFlag(vx, vy, p, dist, main);
                } else {
                    // Метка за спиной или у края: показываем указатель на краю, чтобы она не «мигала» и не пропадала.
                    Vec3d pos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getRandomBodyY());
                    double targetYaw = Math.toDegrees(Math.atan2(point.z - pos.z, point.x - pos.x)) - 90.0;
                    float diff = (float)(((targetYaw - mc.player.getYaw()) % 360.0 + 540.0) % 360.0 - 180.0);
                    this.drawEdge(screenW, screenH, diff, p, dist, main);
                }
            }
            ru.mentions.Client.get().render2D().flushAll();
        }
        finally {
            context.getMatrices().popMatrix();
            ru.mentions.utils.render.Render2D.endOverlay();
        }
    }

    /** Метка-флажок: точка на земле, тонкий шест и плашка с названием и дистанцией на его вершине. */
    private void drawFlag(float x, float y, EventPoint p, double distance, boolean main) {
        ru.mentions.utils.render.font.Font font = ru.mentions.utils.render.font.Fonts.sf_medium;
        String dist = Math.round(distance) + " м";
        float size = main ? 7.0f : 6.0f;
        int accent = HudTheme.accent(255);
        int alphaFull = 255;
        float t = (System.currentTimeMillis() % 1800L) / 1800.0f;

        // Пульс на земле и сама точка.
        float ring = 3.0f + t * 9.0f;
        ru.mentions.utils.render.RenderUtil.Render2D.outline(x - ring, y - ring * 0.5f, ring * 2.0f, ring, 0.6f, ru.mentions.utils.colors.ColorUtil.replAlpha(accent, 0.55f * (1.0f - t)), ring * 0.5f);
        ru.mentions.utils.render.RenderUtil.Render2D.rect(x - 3.0f, y - 3.0f, 6.0f, 6.0f, ru.mentions.utils.colors.ColorUtil.replAlpha(accent, 1.0f), 3.0f);
        ru.mentions.utils.render.RenderUtil.Render2D.rect(x - 1.2f, y - 1.2f, 2.4f, 2.4f, ru.mentions.utils.colors.ColorUtil.getColor(255, 1.0f), 1.2f);

        // Шест и плашка.
        float pole = main ? 26.0f : 20.0f;
        ru.mentions.utils.render.RenderUtil.Render2D.rect(x - 0.6f, y - pole, 1.2f, pole - 3.0f, ru.mentions.utils.colors.ColorUtil.replAlpha(accent, 0.85f), 0.6f);

        float pad = 6.0f;
        float nameW = font.getWidth(p.name, size);
        float distW = font.getWidth(dist, size);
        float w = pad + nameW + 6.0f + distW + pad + 3.0f;
        float h = size + 9.0f;
        float px = x - 1.0f;
        float py = y - pole - h + 2.0f;
        ru.mentions.utils.render.RenderUtil.Render2D.glow(px, py, w, h, ru.mentions.utils.colors.ColorUtil.getColor(0, 0.30f), 5.0f, 8, 1);
        ru.mentions.utils.render.RenderUtil.Render2D.rect(px, py, w, h, ru.mentions.utils.colors.ColorUtil.getColor(12, 13, 18, 0.88f), 4.0f);
        ru.mentions.utils.render.RenderUtil.Render2D.rect(px, py + 2.0f, 2.0f, h - 4.0f, ru.mentions.utils.colors.ColorUtil.replAlpha(accent, 1.0f), 1.0f);
        font.draw(p.name, px + pad + 1.0f, py + (h - size) / 2.0f - 0.5f, size, ru.mentions.utils.colors.ColorUtil.getColor(255, 1.0f));
        font.draw(dist, px + pad + 1.0f + nameW + 6.0f, py + (h - size) / 2.0f - 0.5f, size, ru.mentions.utils.colors.ColorUtil.getColor(170, 1.0f));
    }

    /** Указатель на краю экрана для метки за спиной или вне кадра. */
    private void drawEdge(float screenW, float screenH, float diff, EventPoint p, double distance, boolean main) {
        ru.mentions.utils.render.font.Font font = ru.mentions.utils.render.font.Fonts.sf_medium;
        float size = 6.0f;
        String dist = Math.round(distance) + " м";
        boolean right = diff > 0.0f;
        String label = (right ? "" : "< ") + p.name + "  " + dist + (right ? " >" : "");
        float w = font.getWidth(label, size) + 14.0f;
        float h = size + 9.0f;
        float t = Math.max(-1.0f, Math.min(1.0f, diff / 90.0f));
        float x = screenW / 2.0f + t * (screenW / 2.0f - w / 2.0f - 14.0f) - w / 2.0f;
        float y = screenH - 70.0f - (main ? 0.0f : 20.0f);
        int accent = HudTheme.accent(255);
        ru.mentions.utils.render.RenderUtil.Render2D.glow(x, y, w, h, ru.mentions.utils.colors.ColorUtil.getColor(0, 0.30f), 5.0f, 8, 1);
        ru.mentions.utils.render.RenderUtil.Render2D.rect(x, y, w, h, ru.mentions.utils.colors.ColorUtil.getColor(12, 13, 18, 0.88f), 4.0f);
        ru.mentions.utils.render.RenderUtil.Render2D.outline(x, y, w, h, 0.6f, ru.mentions.utils.colors.ColorUtil.replAlpha(accent, main ? 0.8f : 0.4f), 4.0f);
        font.draw(label, x + 7.0f, y + (h - size) / 2.0f - 0.5f, size, ru.mentions.utils.colors.ColorUtil.getColor(255, 1.0f));
    }

    private record EventPoint(String name, Vec3d pos, boolean flat, long expireAt) {
    }
}
