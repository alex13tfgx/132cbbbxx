package code.wiosvisual.features.impl.render;

import code.wiosvisual.common.repository.friend.FriendUtils;
import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.geometry.Render3D;
import code.wiosvisual.utils.display.render.pipeline.WiosRender;
import code.wiosvisual.utils.math.calc.Calculate;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Шлейф за игроком (как Trails в Pulse): вертикальная светящаяся лента по пройденному пути,
 * переливается цветами темы от головы к хвосту и плавно гаснет; по верхнему и нижнему краю — яркие линии.
 */
public class Trails
extends Module {
    private final SliderSettings length = new SliderSettings("Длина", "Сколько секунд держится шлейф").setValue(1.0f).range(0.2f, 4.0f).step(0.1f);
    private final SliderSettings height = new SliderSettings("Высота", "Высота ленты в долях роста игрока").setValue(0.5f).range(0.05f, 1.0f).step(0.05f);
    private final SliderSettings opacity = new SliderSettings("Прозрачность", "Яркость заливки ленты").setValue(0.45f).range(0.05f, 1.0f).step(0.05f);
    private final BooleanSetting edges = new BooleanSetting("Края", "Яркие линии по краям ленты").setValue(true);
    private final BooleanSetting glow = new BooleanSetting("Свечение", "Аддитивное смешивание — лента светится").setValue(true);
    private final BooleanSetting clientColor = new BooleanSetting("Цвет клиента", "Брать цвета из темы").setValue(true);
    private final ColorSetting customColor = new ColorSetting("Цвет", "Свой цвет шлейфа").value(new Color(150, 110, 255).getRGB()).visible(() -> !this.clientColor.isValue());
    private final BooleanSetting self = new BooleanSetting("Себя", "Шлейф за собой (виден от третьего лица)").setValue(true);
    private final BooleanSetting others = new BooleanSetting("Других игроков", "Шлейф за остальными игроками").setValue(false);
    private final BooleanSetting friendsOnly = new BooleanSetting("Только друзей", "Из чужих — только друзья из .friend").setValue(false).visible(() -> this.others.isValue());

    /** Шаг записи точек: чаще — плавнее изгибы, но больше геометрии. */
    private static final double MIN_STEP = 0.05;

    private final Map<Integer, ArrayDeque<Point>> paths = new HashMap<>();

    public static Trails getInstance() {
        return Instance.get(Trails.class);
    }

    public Trails() {
        super("Trails", "Trails", ModuleCategory.RENDER);
        this.setup(this.length, this.height, this.opacity, this.edges, this.glow, this.clientColor, this.customColor, this.self, this.others, this.friendsOnly);
    }

    @Override
    public void deactivate() {
        this.paths.clear();
        super.deactivate();
    }

    private record Point(Vec3d pos, float h, long time) {
    }

    private boolean tracked(PlayerEntity player) {
        if (player == mc.player) {
            return this.self.isValue();
        }
        if (!this.others.isValue() || player.isInvisible()) {
            return false;
        }
        return !this.friendsOnly.isValue() || FriendUtils.isFriend(player);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.world == null || mc.player == null) {
            this.paths.clear();
            return;
        }
        long now = System.currentTimeMillis();
        long life = (long)(this.length.getValue() * 1000.0f);

        // запись пути: позиция берётся интерполированной на кадр — шлейф без ступенек на 20 тиках
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!this.tracked(player)) {
                this.paths.remove(player.getId());
                continue;
            }
            ArrayDeque<Point> path = this.paths.computeIfAbsent(player.getId(), id -> new ArrayDeque<>());
            Vec3d pos = Calculate.interpolate(player);
            Point last = path.peekLast();
            if (last != null && last.pos.squaredDistanceTo(pos) > 64.0) {
                path.clear(); // телепорт — не тянуть ленту через весь мир
                last = null;
            }
            if (last == null || last.pos.squaredDistanceTo(pos) > MIN_STEP * MIN_STEP) {
                path.addLast(new Point(pos, player.getHeight() * this.height.getValue(), now));
            }
        }
        Iterator<Map.Entry<Integer, ArrayDeque<Point>>> it = this.paths.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ArrayDeque<Point>> entry = it.next();
            ArrayDeque<Point> path = entry.getValue();
            while (!path.isEmpty() && now - path.peekFirst().time > life) {
                path.pollFirst();
            }
            if (path.isEmpty() || mc.world.getEntityById(entry.getKey()) == null) {
                it.remove();
            }
        }

        boolean firstPerson = mc.options.getPerspective().isFirstPerson();
        int c1 = this.clientColor.isValue() ? ColorAssist.getClientColor() : this.customColor.getColor();
        int c2 = this.clientColor.isValue() ? ColorAssist.getClientColor2() : ColorAssist.interpolateColor(this.customColor.getColor(), -1, 0.35f);
        float fillAlpha = this.opacity.getValue();
        MatrixStack.Entry entry = e.getStack().peek();
        Matrix4f matrix = entry.getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        boolean any = false;

        for (Map.Entry<Integer, ArrayDeque<Point>> pe : this.paths.entrySet()) {
            if (pe.getKey() == mc.player.getId() && firstPerson) {
                continue;
            }
            ArrayDeque<Point> path = pe.getValue();
            if (path.size() < 2) {
                continue;
            }
            // голова ленты пристёгнута к текущей позиции игрока, чтобы не отставать на шаг записи
            Point[] pts = path.toArray(new Point[0]);
            Point prev = null;
            int prevColor = 0;
            for (int i = 0; i < pts.length; i++) {
                Point p = pts[i];
                float age = Math.min(1.0f, (now - p.time) / (float)life);
                float fade = 1.0f - age;
                fade *= fade;
                // градиент темы вдоль ленты + лёгкое бегущее переливание
                float t = (float)i / (pts.length - 1);
                float shimmer = 0.5f + 0.5f * (float)Math.sin(now / 350.0 + i * 0.25);
                int color = ColorAssist.interpolateColor(c2, c1, Math.min(1.0f, t * 0.8f + shimmer * 0.2f));
                if (prev != null) {
                    int aPrev = ColorAssist.multAlpha(prevColor, fillAlpha * prevFade(prev, now, life));
                    int aCur = ColorAssist.multAlpha(color, fillAlpha * fade);
                    int topPrev = ColorAssist.multAlpha(aPrev, 0.35f);
                    int topCur = ColorAssist.multAlpha(aCur, 0.35f);
                    float x0 = (float)prev.pos.x, y0 = (float)prev.pos.y, z0 = (float)prev.pos.z;
                    float x1 = (float)p.pos.x, y1 = (float)p.pos.y, z1 = (float)p.pos.z;
                    // низ ярче верха — лента «растворяется» кверху
                    buffer.vertex(matrix, x0, y0, z0).color(aPrev);
                    buffer.vertex(matrix, x0, y0 + prev.h, z0).color(topPrev);
                    buffer.vertex(matrix, x1, y1 + p.h, z1).color(topCur);
                    buffer.vertex(matrix, x1, y1, z1).color(aCur);
                    any = true;
                    if (this.edges.isValue()) {
                        int lPrev = ColorAssist.multAlpha(prevColor, prevFade(prev, now, life));
                        int lCur = ColorAssist.multAlpha(color, fade);
                        Render3D.drawLine(entry, new Vec3d(x0, y0, z0), new Vec3d(x1, y1, z1), lPrev, lCur, 2.0f, true);
                        Render3D.drawLine(entry, new Vec3d(x0, y0 + prev.h, z0), new Vec3d(x1, y1 + p.h, z1),
                                ColorAssist.multAlpha(lPrev, 0.5f), ColorAssist.multAlpha(lCur, 0.5f), 1.5f, true);
                    }
                }
                prev = p;
                prevColor = color;
            }
        }
        if (any) {
            WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.QUADS, true, false, this.glow.isValue()), buffer);
        } else {
            buffer.endNullable();
        }
    }

    private static float prevFade(Point p, long now, long life) {
        float f = 1.0f - Math.min(1.0f, (now - p.time) / (float)life);
        return f * f;
    }
}
