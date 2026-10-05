/*
 * Автоматические метки ивентов: ловит сообщения сервера об ивенте с координатами
 * (аирдроп, мистический сундук, маяк, вулкан и т.д.) и ставит прямо в точке ивента
 * метку-облачко: иконка, название и расстояние. Когда ивент заканчивается
 * (истекло его время, сервер написал, что он завершён/залутан, или вышло «Время жизни»),
 * метка плавно гаснет и удаляется.
 */
package code.wiosvisual.features.impl.render;

import code.wiosvisual.common.repository.way.WayRepository;
import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.events.render.DrawEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.chat.ChatMessage;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.math.projection.Projection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import ru.mentions.utils.render.RenderUtil;
import ru.mentions.utils.render.font.Font;

public class EventWaypoints
extends Module {
    private static final int MAX_POINTS = 6;
    private static final double MERGE_DISTANCE = 16.0;
    /** Запас после объявленной длительности ивента: сервер может закрыть его чуть позже. */
    private static final long DURATION_GRACE_MS = 60_000L;
    private static final long FADE_IN_MS = 300L;
    private static final long FADE_OUT_MS = 700L;

    private static final Identifier ICON = Identifier.of("wiosvisual", "textures/waypoint/icon_home.png");
    private static final Identifier TAIL = Identifier.of("wiosvisual", "textures/waypoint/tail.png");

    /** X Y Z с подписями: «X: 120 Y: 64 Z: -300», «x=120, y=64, z=-300». Поддерживается кириллическая «Х». */
    private static final Pattern LABELED_XYZ = Pattern.compile("[xх]\\s*[:=]?\\s*(-?\\d{1,7})[^\\d-]{1,8}?[yу]\\s*[:=]?\\s*(-?\\d{1,3})[^\\d-]{1,8}?[zз]\\s*[:=]?\\s*(-?\\d{1,7})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    /** X Z с подписями, без высоты. */
    private static final Pattern LABELED_XZ = Pattern.compile("[xх]\\s*[:=]?\\s*(-?\\d{1,7})[^\\d-]{1,8}?[zз]\\s*[:=]?\\s*(-?\\d{1,7})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    /** Три числа подряд: «120 64 -300», «120, 64, -300», «[120 / 64 / -300]». */
    private static final Pattern PLAIN_XYZ = Pattern.compile("(?<![\\d.:])(-?\\d{1,7})[\\s,;/|]+(-?\\d{1,3})[\\s,;/|]+(-?\\d{1,7})(?![\\d.:%])");
    /** Длительность: «5 минут», «120 секунд», «2 мин 30 сек», «1 час». */
    private static final Pattern DURATION_UNIT = Pattern.compile("(\\d{1,4})\\s*(час|ч\\b|мин|м\\b|min|сек|с\\b|sec|s\\b)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    /** Длительность таймером: «04:30», «1:05:00». */
    private static final Pattern DURATION_CLOCK = Pattern.compile("(?<![\\d:])(?:(\\d{1,2}):)?(\\d{1,2}):(\\d{2})(?![\\d:])");
    /** Сообщения об окончании ивента. */
    private static final String[] END_WORDS = {
            "закончил", "закончен", "завершил", "завершён", "завершен", "окончен", "окончил",
            "залут", "был открыт", "открыл", "забрал", "исчез", "уничтожен", "отменён", "отменен",
            "ended", "finished", "looted", "despawn"};

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
    private final BooleanSetting removeOnArrive = new BooleanSetting("Удалять при прибытии", "Убирать метку, когда вы дошли до ивента").setValue(false);
    private final SliderSettings lifetime = new SliderSettings("Время жизни", "Сколько минут держать метку, если сервер не сообщил длительность ивента").range(1.0f, 30.0f).step(1.0f).setValue(10.0f);

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

    // ------------------------------------------------------------------ разбор сообщений

    private static String eventName(String lower) {
        for (String[] entry : EVENT_NAMES) {
            if (lower.contains(entry[0])) {
                return entry[1];
            }
        }
        return null;
    }

    private static boolean isEndMessage(String lower) {
        for (String word : END_WORDS) {
            if (lower.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /** Координаты из сообщения: {x, y, z, flat(0/1)} или null. */
    private static double[] coords(String message) {
        Matcher m = LABELED_XYZ.matcher(message);
        if (m.find()) {
            return new double[]{Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3)), 0};
        }
        if ((m = PLAIN_XYZ.matcher(message)).find() && Math.abs(Double.parseDouble(m.group(2))) <= 320.0) {
            return new double[]{Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3)), 0};
        }
        if ((m = LABELED_XZ.matcher(message)).find()) {
            return new double[]{Double.parseDouble(m.group(1)), 0, Double.parseDouble(m.group(2)), 1};
        }
        return null;
    }

    /** Объявленная длительность ивента в мс (сумма всех единиц: «2 мин 30 сек»), или −1. */
    private static long duration(String message, double[] xyz) {
        // Координаты тоже бывают «числом с единицей» не бывают, но «12:30» может оказаться временем суток —
        // таймер берём только если он не совпадает с координатами.
        long total = 0;
        Matcher m = DURATION_UNIT.matcher(message);
        while (m.find()) {
            long v = Long.parseLong(m.group(1));
            String unit = m.group(2).toLowerCase(Locale.ROOT);
            if (unit.startsWith("ч")) {
                total += v * 3_600_000L;
            } else if (unit.startsWith("м")) {
                total += v * 60_000L;
            } else {
                total += v * 1000L;
            }
        }
        if (total > 0) {
            return total;
        }
        m = DURATION_CLOCK.matcher(message);
        if (m.find()) {
            long h = m.group(1) != null ? Long.parseLong(m.group(1)) : 0;
            long mm = Long.parseLong(m.group(2));
            long ss = Long.parseLong(m.group(3));
            if (ss < 60 && (h > 0 || mm < 60)) {
                return (h * 3600 + mm * 60 + ss) * 1000L;
            }
        }
        return -1;
    }

    private void handleMessage(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        String name = eventName(lower);
        double[] xyz = coords(message);

        // Ивент закончился: убираем его метку (по координатам, а без них — по названию).
        if (isEndMessage(lower) && (name != null || xyz != null)) {
            for (EventPoint p : this.points) {
                boolean sameSpot = xyz != null && Math.hypot(p.pos.x - xyz[0], p.pos.z - xyz[2]) <= MERGE_DISTANCE;
                boolean sameName = xyz == null && name != null && p.name.equals(name);
                if (sameSpot || sameName) {
                    this.expire(p);
                }
            }
            return;
        }
        if (name == null) {
            if (this.onlyEvents.isValue()) {
                return;
            }
            name = "Точка";
        }
        if (xyz == null) {
            return;
        }
        boolean flat = xyz[3] > 0;
        this.server = WayRepository.currentServer();
        Vec3d pos = new Vec3d(Math.floor(xyz[0]) + 0.5, xyz[1], Math.floor(xyz[2]) + 0.5);
        long now = System.currentTimeMillis();
        long declared = duration(message, xyz);
        long expireAt = declared > 0
                ? now + declared + DURATION_GRACE_MS
                : now + (long)(this.lifetime.getValue() * 60_000.0f);
        boolean known = false;
        long createdAt = now;
        for (EventPoint p : this.points) {
            if (Math.hypot(p.pos.x - pos.x, p.pos.z - pos.z) <= MERGE_DISTANCE) {
                this.points.remove(p);
                createdAt = p.createdAt; // повторное объявление того же ивента — без повторного появления
                known = true;
            }
        }
        this.points.add(new EventPoint(name, pos, flat, createdAt, expireAt));
        while (this.points.size() > MAX_POINTS) {
            this.points.remove(0);
        }
        if (this.notify.isValue() && !known) {
            ChatMessage.brandmessage(name + ": " + (long)Math.floor(xyz[0]) + (flat ? "" : " " + (long)xyz[1]) + " " + (long)Math.floor(xyz[2]) + " — метка поставлена");
        }
    }

    /** Плавное исчезновение: метка доживает FADE_OUT_MS и удаляется в onDraw. */
    private void expire(EventPoint p) {
        long now = System.currentTimeMillis();
        if (p.expireAt > now + FADE_OUT_MS) {
            this.points.remove(p);
            this.points.add(new EventPoint(p.name, p.pos, p.flat, p.createdAt, now + FADE_OUT_MS));
        }
    }

    private Vec3d playerPos() {
        return new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
    }

    private double distance(EventPoint p) {
        Vec3d pos = this.playerPos();
        double dx = p.pos.x - pos.x;
        double dz = p.pos.z - pos.z;
        double dy = p.flat ? 0.0 : p.pos.y - pos.y;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private Vec3d resolved(EventPoint p) {
        return p.flat ? new Vec3d(p.pos.x, this.playerPos().y + 1.0, p.pos.z) : p.pos;
    }

    /** «1 метр», «3 метра», «25 метров», «1,2 км». */
    static String formatDistance(double distance) {
        long m = Math.round(distance);
        if (m >= 1000) {
            return String.format(Locale.ROOT, "%.1f", m / 1000.0).replace('.', ',') + " км";
        }
        long mod100 = m % 100;
        long mod10 = m % 10;
        String word = mod100 >= 11 && mod100 <= 14 ? "метров" : mod10 == 1 ? "метр" : mod10 >= 2 && mod10 <= 4 ? "метра" : "метров";
        return m + " " + word;
    }

    /** Прозрачность метки: плавное появление и исчезновение. */
    private static float alphaOf(EventPoint p, long now) {
        float in = Math.min(1.0f, (now - p.createdAt) / (float) FADE_IN_MS);
        float out = Math.min(1.0f, Math.max(0.0f, (p.expireAt - now) / (float) FADE_OUT_MS));
        float a = Math.min(in, out);
        return a * a * (3.0f - 2.0f * a);
    }

    // ------------------------------------------------------------------ отрисовка

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
            if (now >= p.expireAt) {
                this.points.remove(p);
                continue;
            }
            if (this.removeOnArrive.isValue() && d <= 4.0) {
                this.expire(p);
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
            // дальние рисуем первыми, чтобы ближняя метка была поверх
            List<EventPoint> order = new java.util.ArrayList<>(this.points);
            order.sort((a, b) -> Double.compare(this.distance(b), this.distance(a)));
            for (EventPoint p : order) {
                Vec3d point = this.resolved(p);
                double dist = this.distance(p);
                boolean main = p == nearest;
                float alpha = alphaOf(p, now);
                boolean visible = Projection.isInFrontOfCamera(point) && Projection.worldSpaceToScreenSpaceInto(point.x, point.y, point.z, screen);
                float vx = visible ? (float)screen[0] * guiScale / 2.0f : 0.0f;
                float vy = visible ? (float)screen[1] * guiScale / 2.0f : 0.0f;
                if (visible && vx > 30.0f && vx < screenW - 30.0f && vy > 50.0f && vy < screenH - 10.0f) {
                    this.drawBubble(vx, vy, p.name, formatDistance(dist), main ? 0.72f : 0.6f, alpha);
                } else {
                    // Метка за спиной или у края: компактная плашка у края экрана, чтобы точка не терялась.
                    Vec3d pos = this.playerPos();
                    double targetYaw = Math.toDegrees(Math.atan2(point.z - pos.z, point.x - pos.x)) - 90.0;
                    float diff = (float)(((targetYaw - mc.player.getYaw()) % 360.0 + 540.0) % 360.0 - 180.0);
                    this.drawEdge(screenW, screenH, diff, p, dist, main, alpha);
                }
            }
            ru.mentions.Client.get().render2D().flushAll();
        }
        finally {
            context.getMatrices().popMatrix();
            ru.mentions.utils.render.Render2D.endOverlay();
        }
    }

    private static int argb(int r, int g, int b, float a) {
        int alpha = Math.max(0, Math.min(255, Math.round(a * 255.0f)));
        return alpha << 24 | r << 16 | g << 8 | b;
    }

    private static int white(float a) {
        return argb(255, 255, 255, a);
    }

    /**
     * Метка-облачко: тёмная скруглённая плашка без обводки, иконка-плитка с домиком,
     * название и расстояние; хвостик снизу указывает ровно в точку ивента (x, y).
     */
    private void drawBubble(float x, float y, String title, String subtitle, float s, float alpha) {
        if (alpha <= 0.01f) {
            return;
        }
        Font bold = ru.mentions.utils.render.font.Fonts.sf_bold;
        Font medium = ru.mentions.utils.render.font.Fonts.sf_medium;
        float titleSize = 9.5f * s;
        float subSize = 8.5f * s;
        float icon = 22.0f * s;
        float pad = 6.0f * s;
        float gap = 7.0f * s;
        float textW = Math.max(bold.getWidth(title, titleSize), medium.getWidth(subtitle, subSize));
        float w = pad + icon + gap + textW + pad * 1.8f;
        float h = icon + pad * 2.0f;
        float radius = h * 0.34f;
        float tailW = 14.0f * s;
        float tailH = 9.0f * s;
        float border = 1.3f * s;

        float bx = x - w / 2.0f;
        float by = y - tailH - h + border;

        RenderUtil.Render2D.rect(bx, by, w, h, argb(21, 22, 28, 0.95f * alpha), radius);
        // хвостик в цвет плашки, чуть заходит на неё, чтобы не было шва
        RenderUtil.Images.texture(TAIL, x - tailW / 2.0f, by + h - border * 1.2f, tailW, tailH + border * 1.2f, white(alpha));
        // иконка
        RenderUtil.Images.texture(ICON, bx + pad, by + pad, icon, icon, white(alpha));
        // текст
        float tx = bx + pad + icon + gap;
        float block = titleSize + 2.0f * s + subSize;
        float ty = by + (h - block) / 2.0f - 0.5f * s;
        bold.draw(title, tx, ty, titleSize, white(alpha));
        medium.draw(subtitle, tx, ty + titleSize + 2.0f * s, subSize, white(0.92f * alpha));
    }

    /** Плашка у края экрана для метки за спиной или вне кадра — в том же стиле, со стрелкой. */
    private void drawEdge(float screenW, float screenH, float diff, EventPoint p, double distance, boolean main, float alpha) {
        if (alpha <= 0.01f) {
            return;
        }
        Font bold = ru.mentions.utils.render.font.Fonts.sf_bold;
        Font medium = ru.mentions.utils.render.font.Fonts.sf_medium;
        float s = main ? 0.72f : 0.6f;
        boolean right = diff > 0.0f;
        String title = p.name;
        String sub = formatDistance(distance);
        String arrow = right ? "›" : "‹";
        float titleSize = 8.0f * s;
        float subSize = 7.5f * s;
        float icon = 16.0f * s;
        float pad = 5.0f * s;
        float gap = 6.0f * s;
        float textW = bold.getWidth(title, titleSize) + 6.0f * s + medium.getWidth(sub, subSize);
        float arrowW = bold.getWidth(arrow, titleSize * 1.4f) + 6.0f * s;
        float w = pad + icon + gap + textW + pad + arrowW;
        float h = icon + pad * 2.0f;
        float radius = h * 0.36f;
        float t = Math.max(-1.0f, Math.min(1.0f, diff / 90.0f));
        float x = screenW / 2.0f + t * (screenW / 2.0f - w / 2.0f - 14.0f) - w / 2.0f;
        float y = screenH - 74.0f - (main ? 0.0f : h + 6.0f);
        RenderUtil.Render2D.rect(x, y, w, h, argb(21, 22, 28, 0.95f * alpha), radius);
        float ix = right ? x + pad : x + pad + arrowW;
        RenderUtil.Images.texture(ICON, ix, y + pad, icon, icon, white(alpha));
        float tx = ix + icon + gap;
        float ty = y + (h - titleSize) / 2.0f - 0.5f * s;
        bold.draw(title, tx, ty, titleSize, white(alpha));
        medium.draw(sub, tx + bold.getWidth(title, titleSize) + 6.0f * s, ty + (titleSize - subSize) / 2.0f, subSize, white(0.85f * alpha));
        float ax = right ? x + w - pad - arrowW + 3.0f * s : x + pad;
        bold.draw(arrow, ax, y + (h - titleSize * 1.4f) / 2.0f - 1.0f * s, titleSize * 1.4f, white(alpha));
    }

    private record EventPoint(String name, Vec3d pos, boolean flat, long createdAt, long expireAt) {
    }
}
