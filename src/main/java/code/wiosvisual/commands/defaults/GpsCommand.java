/*
 * .gps <x> <z> | .gps <x> <y> <z> — навигация к точке: метка с дистанцией в мире
 * и стрелка над прицелом, указывающая направление. .gps off — сбросить.
 * Точка сбрасывается сама, когда игрок подходит ближе ARRIVE_DISTANCE блоков.
 */
package code.wiosvisual.commands.defaults;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.utils.display.render.pipeline.WiosRender;
import code.wiosvisual.events.render.DrawEvent;
import code.wiosvisual.utils.client.managers.api.command.Command;
import code.wiosvisual.utils.client.managers.api.command.argument.IArgConsumer;
import code.wiosvisual.utils.client.managers.api.command.exception.CommandException;
import code.wiosvisual.utils.client.managers.api.command.helpers.TabCompleteHelper;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.managers.event.EventManager;
import code.wiosvisual.display.hud.HudTheme;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import code.wiosvisual.utils.math.projection.Projection;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

public class GpsCommand
extends Command {
    private static final double ARRIVE_DISTANCE = 3.0;

    private Vec3d target;
    /** Высота не задана — метка следует за высотой игрока, дистанция считается по горизонтали. */
    private boolean flat;

    private static GpsCommand instance;

    public GpsCommand() {
        super("gps");
        instance = this;
        EventManager.register(this);
    }

    public boolean isActive() {
        return this.target != null;
    }

    public static GpsCommand getInstance() {
        return instance;
    }

    /** Включает навигацию к точке с высотой (используется командой .way gps). */
    public void navigate(Vec3d point) {
        this.flat = false;
        this.target = point;
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            this.logDirect("Использование: .gps <x> <z> | .gps <x> <y> <z> | .gps off", Formatting.YELLOW);
            return;
        }
        String first = args.peekString().toLowerCase(Locale.ROOT);
        if (first.equals("off") || first.equals("stop") || first.equals("clear")) {
            args.getString();
            if (this.target == null) {
                this.logDirect("GPS и так выключен.", Formatting.GRAY);
            } else {
                this.target = null;
                this.logDirect("GPS выключен.", Formatting.WHITE);
            }
            return;
        }
        args.requireMin(2);
        args.requireMax(3);
        double[] c = new double[args.getArgs().size()];
        for (int i = 0; i < c.length; ++i) {
            String raw = args.getString();
            try {
                c[i] = this.parseCoord(raw, i, c.length);
            } catch (NumberFormatException ex) {
                this.logDirect("Не число: " + raw, Formatting.RED);
                return;
            }
        }
        if (c.length == 2) {
            this.flat = true;
            this.target = new Vec3d(Math.floor(c[0]) + 0.5, 0.0, Math.floor(c[1]) + 0.5);
        } else {
            this.flat = false;
            this.target = new Vec3d(Math.floor(c[0]) + 0.5, c[1], Math.floor(c[2]) + 0.5);
        }
        this.logDirect("GPS: " + (long)Math.floor(this.target.x) + (this.flat ? "" : " " + (long)Math.floor(this.target.y)) + " " + (long)Math.floor(this.target.z) + " — " + Math.round(this.distance()) + "м", Formatting.GREEN);
    }

    /** Поддерживает «~» и «~N» — относительно текущей позиции игрока. */
    private double parseCoord(String raw, int index, int count) {
        if (!raw.startsWith("~")) {
            return Double.parseDouble(raw);
        }
        double offset = raw.length() == 1 ? 0.0 : Double.parseDouble(raw.substring(1));
        if (mc.player == null) {
            return offset;
        }
        Vec3d pos = new net.minecraft.util.math.Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getRandomBodyY());
        double base = count == 2 ? (index == 0 ? pos.x : pos.z) : (index == 0 ? pos.x : index == 1 ? pos.y : pos.z);
        return base + offset;
    }

    private Vec3d resolvedTarget() {
        return this.flat ? new Vec3d(this.target.x, new net.minecraft.util.math.Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getRandomBodyY()).y + 1.0, this.target.z) : this.target;
    }

    private double distance() {
        if (mc.player == null) {
            return 0.0;
        }
        Vec3d pos = new net.minecraft.util.math.Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getRandomBodyY());
        double dx = this.target.x - pos.x;
        double dz = this.target.z - pos.z;
        double dy = this.flat ? 0.0 : this.target.y - pos.y;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        if (this.target == null || mc.player == null || mc.world == null) {
            return;
        }
        double distance = this.distance();
        if (distance <= ARRIVE_DISTANCE) {
            this.target = null;
            this.logDirect("GPS: вы прибыли.", Formatting.GREEN);
            return;
        }
        DrawContext context = e.getDrawContext();
        MatrixStack matrix = GuiCompat.stack(context);

        Vec3d point = this.resolvedTarget();
        double[] screen = new double[3];
        if (Projection.isInFrontOfCamera(point) && Projection.worldSpaceToScreenSpaceInto(point.x, point.y, point.z, screen)) {
            this.drawWorldMarker(matrix, Math.round(screen[0]), Math.round(screen[1]), distance);
        }

        Vec3d pos = new net.minecraft.util.math.Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getRandomBodyY());
        double targetYaw = Math.toDegrees(Math.atan2(point.z - pos.z, point.x - pos.x)) - 90.0;
        float diff = (float)(((targetYaw - mc.player.getYaw()) % 360.0 + 540.0) % 360.0 - 180.0);
        this.drawPanel(matrix, context.getScaledWindowWidth(), context.getScaledWindowHeight(), distance, diff);
    }

    /** Большая панель над хотбаром: бейдж со стрелкой, дистанция и координаты. */
    private void drawPanel(MatrixStack matrix, int screenW, int screenH, double distance, float arrowDeg) {
        FontRenderer title = Fonts.getSize(15, Fonts.Type.SuisseIntlSemiBold);
        FontRenderer big = Fonts.getSize(26, Fonts.Type.SuisseIntlSemiBold);
        FontRenderer small = Fonts.getSize(13, Fonts.Type.SuisseIntlMedium);
        String distText = Math.round(distance) + " м";
        String coords = (long)Math.floor(this.target.x) + (this.flat ? "" : ", " + (long)Math.floor(this.target.y)) + ", " + (long)Math.floor(this.target.z);

        float height = 48.0f;
        float badge = 34.0f;
        float pad = 7.0f;
        float headerW = 12.0f + title.getStringWidth("GPS") + 6.0f + small.getStringWidth(coords);
        float textW = Math.max(big.getStringWidth(distText), Math.max(headerW, 70.0f));
        float width = (float)Math.ceil(pad + badge + 10.0f + textW + 14.0f);
        float x = Math.round((screenW - width) / 2.0f);
        float y = Math.round(screenH - 100.0f);

        // Тень, фон-градиент и акцентная обводка.
        rectangle.render(ShapeProperties.create(matrix, x - 4.0f, y - 2.0f, width + 8.0f, height + 8.0f).round(14.0f).softness(10.0f).color(new Color(0, 0, 0, 120).getRGB()).build());
        rectangle.render(ShapeProperties.create(matrix, x, y, width, height).round(11.0f).color(HudTheme.panelTop(245), HudTheme.panelTop(245), HudTheme.panelBottom(245), HudTheme.panelBottom(245)).build());
        rectangle.render(ShapeProperties.create(matrix, x, y, width, height).round(11.0f).thickness(1.0f).outlineColor(HudTheme.accent(150)).color(0).build());

        // Бейдж со стрелкой направления.
        float bx = x + pad;
        float by = y + (height - badge) / 2.0f;
        rectangle.render(ShapeProperties.create(matrix, bx, by, badge, badge).round(badge / 2.0f).color(HudTheme.accent(255), HudTheme.accentSecondary(255), HudTheme.accentSecondary(255), HudTheme.accent(255)).build());
        Rectangle.flushPending();
        GpsCommand.drawArrow(matrix, bx + badge / 2.0f, by + badge / 2.0f, badge * 0.32f, arrowDeg, 0xFFFFFFFF);

        float tx = Math.round(bx + badge + 10.0f);
        GpsCommand.drawPin(matrix, tx, y + 8.0f, 9.0f, HudTheme.accentBright(255));
        title.drawString(matrix, "GPS", Math.round(tx + 12.0f), y + 8.0f, 0xFFFFFFFF);
        small.drawString(matrix, coords, Math.round(tx + 12.0f + title.getStringWidth("GPS") + 6.0f), y + 9.0f, HudTheme.muted(255));
        big.drawString(matrix, distText, Math.round(tx), y + 21.0f, 0xFFFFFFFF);
    }

    /** Метка в мире: булавка и плашка с дистанцией под ней. */
    private void drawWorldMarker(MatrixStack matrix, float cx, float cy, double distance) {
        FontRenderer font = Fonts.getSize(16, Fonts.Type.SuisseIntlSemiBold);
        String text = Math.round(distance) + " м";
        float pin = 18.0f;
        float w = (float)Math.ceil(font.getStringWidth(text) + 14.0f);
        float h = 17.0f;
        float x = Math.round(cx - w / 2.0f);
        float y = Math.round(cy + 3.0f);
        rectangle.render(ShapeProperties.create(matrix, x - 2.0f, y - 1.0f, w + 4.0f, h + 4.0f).round(8.0f).softness(6.0f).color(new Color(0, 0, 0, 110).getRGB()).build());
        rectangle.render(ShapeProperties.create(matrix, x, y, w, h).round(6.0f).color(HudTheme.panelTop(240), HudTheme.panelTop(240), HudTheme.panelBottom(240), HudTheme.panelBottom(240)).build());
        rectangle.render(ShapeProperties.create(matrix, x, y, w, h).round(6.0f).thickness(1.0f).outlineColor(HudTheme.accent(140)).color(0).build());
        Rectangle.flushPending();
        font.drawCenteredBoth(matrix, text, Math.round(x + w / 2.0f), y, h, 0xFFFFFFFF);
        GpsCommand.drawPin(matrix, Math.round(cx - pin / 2.0f), cy - pin, pin, HudTheme.accentBright(255));
    }

    /** Гладкая стрелка-наконечник из треугольников, повёрнутая на deg (0 — вверх). */
    public static void drawArrow(MatrixStack matrix, float cx, float cy, float r, float deg, int color) {
        double rad = Math.toRadians(deg);
        float cos = (float)Math.cos(rad);
        float sin = (float)Math.sin(rad);
        float[][] pts = {{0.0f, -r}, {r * 0.8f, r * 0.85f}, {0.0f, r * 0.4f}, {-r * 0.8f, r * 0.85f}};
        float[][] p = new float[4][2];
        for (int i = 0; i < 4; ++i) {
            p[i][0] = cx + pts[i][0] * cos - pts[i][1] * sin;
            p[i][1] = cy + pts[i][0] * sin + pts[i][1] * cos;
        }
        Matrix4f m = matrix.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        GpsCommand.tri(buffer, m, p[0], p[1], p[2], color);
        GpsCommand.tri(buffer, m, p[0], p[2], p[3], color);
        WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.TRIANGLES, false, false), buffer);
    }

    /** Булавка карты: круглая головка с отверстием и гладкий треугольный хвост. */
    public static void drawPin(MatrixStack matrix, float x, float y, float size, int color) {
        float head = size * 0.78f;
        float hx = x + (size - head) / 2.0f;
        float cx = x + size / 2.0f;
        Matrix4f m = matrix.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        GpsCommand.tri(buffer, m, new float[]{cx - head * 0.42f, y + head * 0.62f}, new float[]{cx + head * 0.42f, y + head * 0.62f}, new float[]{cx, y + size}, color);
        WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.TRIANGLES, false, false), buffer);
        rectangle.render(ShapeProperties.create(matrix, hx, y, head, head).round(head / 2.0f).color(color).build());
        float hole = head * 0.36f;
        rectangle.render(ShapeProperties.create(matrix, cx - hole / 2.0f, y + (head - hole) / 2.0f, hole, hole).round(hole / 2.0f).color(HudTheme.panelBottom(255)).build());
        Rectangle.flushPending();
    }

    private static void tri(BufferBuilder b, Matrix4f m, float[] a, float[] c, float[] d, int color) {
        b.vertex(m, a[0], a[1], 0.0f).color(color);
        b.vertex(m, c[0], c[1], 0.0f).color(color);
        b.vertex(m, d[0], d[1], 0.0f).color(color);
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper().prepend("off", "~").filterPrefix(args.getString()).stream();
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Навигация к координатам";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Показывает метку точки в мире и стрелку-направление над прицелом.",
                "Выключается сам, когда вы подходите к точке.",
                "",
                "Использование:",
                "> gps <x> <z> - вести к точке по горизонтали",
                "> gps <x> <y> <z> - вести к точке с высотой (поддерживается ~)",
                "> gps off - выключить GPS");
    }
}
