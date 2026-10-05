package code.wiosvisual.features.impl.render;

import code.wiosvisual.utils.display.render.pipeline.WiosRender;
import code.wiosvisual.events.player.EntityDeathEvent;
import code.wiosvisual.events.render.WorldLoadEvent;
import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.geometry.Render3D;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import com.mojang.blaze3d.vertex.VertexFormat;

/**
 * Soft glowing beam over a killed target (ported from Nightix).
 * Rendered with a custom POSITION_COLOR quad buffer so per-vertex alpha
 * gradients work on the 1.21.4 render system.
 */
public class KillEffect
extends Module {
    private static final float TAU = (float)(Math.PI * 2.0);

    private final SelectSetting typeColor = new SelectSetting("Режим цвета", "Color mode").value("Тема", "Свой").selected("Тема");
    private final ColorSetting tintColor = new ColorSetting("Цвет", "Color").value(0xFF00FFFF).visible(() -> this.typeColor.isSelected("Свой"));
    private final BooleanSetting mobs = new BooleanSetting("Мобы", "Mobs").setValue(false);
    private final SliderSettings duration = new SliderSettings("Длительность", "Duration").setValue(2.2f).range(1.0f, 5.0f).step(0.1f);
    private final SliderSettings beamHeight = new SliderSettings("Высота луча", "Beam height").setValue(5.2f).range(1.5f, 12.0f).step(0.5f);
    private final SliderSettings beamWidth = new SliderSettings("Ширина луча", "Beam width").setValue(0.42f).range(0.08f, 1.2f).step(0.02f);

    private final List<KillBeam> beams = new ArrayList<KillBeam>();

    public KillEffect() {
        super("KillEffect", "Kill Effect", ModuleCategory.RENDER);
        this.setup(this.typeColor, this.tintColor, this.mobs, this.duration, this.beamHeight, this.beamWidth);
    }

    @Override
    public void deactivate() {
        this.beams.clear();
        super.deactivate();
    }

    public int getColor() {
        if (this.typeColor.isSelected("Тема")) {
            return ColorAssist.getClientColor();
        }
        return this.tintColor.getColor();
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        this.beams.clear();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (KillEffect.mc.world == null || KillEffect.mc.player == null) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity) || entity == KillEffect.mc.player) {
            return;
        }
        if (!this.mobs.isValue() && !(entity instanceof PlayerEntity)) {
            return;
        }
        Vec3d pos = new net.minecraft.util.math.Vec3d(entity.getX(), entity.getY(), entity.getZ());
        float radius = Math.max(0.65f, entity.getWidth());
        this.beams.add(new KillBeam(pos.x, pos.y, pos.z, radius));
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (this.beams.isEmpty() || KillEffect.mc.player == null || KillEffect.mc.world == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long durMs = (long)(this.duration.getValue() * 1000.0f);

        int color = this.getColor();
        int cr = color >> 16 & 0xFF;
        int cg = color >> 8 & 0xFF;
        int cb = color & 0xFF;

        Iterator<KillBeam> it = this.beams.iterator();
        while (it.hasNext()) {
            KillBeam beam = it.next();
            float progress = (float)(now - beam.start) / (float)durMs;
            if (progress >= 1.0f) {
                it.remove();
                continue;
            }
            float fadeIn = MathHelper.clamp(progress * 5.5f, 0.0f, 1.0f);
            float fadeOut = progress > 0.5f ? 1.0f - (progress - 0.5f) / 0.5f : 1.0f;
            beam.alpha = KillEffect.smooth(fadeIn) * KillEffect.smooth(fadeOut);
            beam.progress = progress;
        }
        if (this.beams.isEmpty()) {
            return;
        }

        MatrixStack.Entry entry = Render3D.lastWorldSpaceMatrix;
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float pulse = 0.9f + 0.1f * MathHelper.sin((float)(now % 1600L) / 1600.0f * TAU);
        for (KillBeam beam : this.beams) {
            this.drawBeam(buf, entry, beam, cr, cg, cb, pulse);
        }
        BuiltBuffer built = buf.endNullable();
        if (built != null) {
            WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.QUADS, true, false), built);
        }

    }

    private void drawBeam(BufferBuilder buf, MatrixStack.Entry entry, KillBeam beam, int r, int g, int b, float pulse) {
        if (beam.alpha <= 0.01f) {
            return;
        }
        float grow = KillEffect.smooth(Math.min(beam.progress * 3.5f, 1.0f));
        float h = this.beamHeight.getValue() * grow;
        float base = this.beamWidth.getValue() * beam.radius;
        float x = (float)beam.x;
        float y = (float)beam.y + 0.035f;
        float z = (float)beam.z;

        float spin = (float)(System.currentTimeMillis() - beam.start) / 1000.0f * 0.85f;
        float coreAlpha = 150.0f * beam.alpha * pulse;
        float softAlpha = 54.0f * beam.alpha;

        KillEffect.drawVerticalPetals(buf, entry, x, y, z, h, base * 0.72f, spin, r, g, b, (int)coreAlpha, 6);
        KillEffect.drawVerticalPetals(buf, entry, x, y, z, h * 0.82f, base * 1.28f, -spin * 0.55f, r, g, b, (int)softAlpha, 8);

        float bloom = 0.55f + 0.45f * grow;
        KillEffect.drawSoftDisc(buf, entry, x, y + 0.01f, z, base * 2.25f * bloom, r, g, b, (int)(42.0f * beam.alpha));
        KillEffect.drawSoftDisc(buf, entry, x, y + h * 0.98f, z, base * 0.92f * beam.alpha, r, g, b, (int)(28.0f * beam.alpha));

        for (int i = 0; i < 3; ++i) {
            float wave = (beam.progress + (float)i * 0.22f) % 1.0f;
            float radius = base * (1.05f + wave * 2.25f);
            int alpha = (int)(52.0f * beam.alpha * (1.0f - wave));
            KillEffect.drawRing(buf, entry, x, y + (float)i * 0.025f, z, radius, 0.035f + wave * 0.045f, r, g, b, alpha);
        }
    }

    private static void vertex(BufferBuilder buf, MatrixStack.Entry entry, float x, float y, float z, int r, int g, int b, int a) {
        buf.vertex(entry, x, y, z).color(KillEffect.argb(r, g, b, a));
    }

    private static int argb(int r, int g, int b, int a) {
        a = MathHelper.clamp(a, 0, 255);
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF;
    }

    private static void drawVerticalPetals(BufferBuilder buf, MatrixStack.Entry entry, float x, float y, float z, float h, float halfWidth, float spin, int r, int g, int b, int alpha, int petals) {
        if (alpha <= 0 || h <= 0.01f) {
            return;
        }
        for (int i = 0; i < petals; ++i) {
            float ang = spin + (float)i / (float)petals * TAU;
            float dx = MathHelper.cos(ang) * halfWidth;
            float dz = MathHelper.sin(ang) * halfWidth;
            int edgeAlpha = Math.max(0, alpha / 3);
            KillEffect.vertex(buf, entry, x - dx, y, z - dz, r, g, b, edgeAlpha);
            KillEffect.vertex(buf, entry, x, y + h * 0.08f, z, r, g, b, alpha);
            KillEffect.vertex(buf, entry, x, y + h, z, r, g, b, 0);
            KillEffect.vertex(buf, entry, x + dx, y, z + dz, r, g, b, edgeAlpha);
        }
    }

    private static void drawSoftDisc(BufferBuilder buf, MatrixStack.Entry entry, float x, float y, float z, float radius, int r, int g, int b, int alpha) {
        if (alpha <= 0 || radius <= 0.01f) {
            return;
        }
        int segments = 56;
        for (int i = 0; i < segments; ++i) {
            float a0 = (float)i / (float)segments * TAU;
            float a1 = (float)(i + 1) / (float)segments * TAU;
            KillEffect.vertex(buf, entry, x, y, z, r, g, b, alpha);
            KillEffect.vertex(buf, entry, x + MathHelper.cos(a0) * radius, y, z + MathHelper.sin(a0) * radius, r, g, b, 0);
            KillEffect.vertex(buf, entry, x + MathHelper.cos(a1) * radius, y, z + MathHelper.sin(a1) * radius, r, g, b, 0);
            KillEffect.vertex(buf, entry, x, y, z, r, g, b, alpha);
        }
    }

    private static void drawRing(BufferBuilder buf, MatrixStack.Entry entry, float x, float y, float z, float radius, float width, int r, int g, int b, int alpha) {
        if (alpha <= 0 || radius <= width) {
            return;
        }
        int segments = 64;
        for (int i = 0; i < segments; ++i) {
            float a0 = (float)i / (float)segments * TAU;
            float a1 = (float)(i + 1) / (float)segments * TAU;
            float x0 = MathHelper.cos(a0);
            float z0 = MathHelper.sin(a0);
            float x1 = MathHelper.cos(a1);
            float z1 = MathHelper.sin(a1);
            KillEffect.vertex(buf, entry, x + x0 * (radius - width), y, z + z0 * (radius - width), r, g, b, 0);
            KillEffect.vertex(buf, entry, x + x0 * radius, y, z + z0 * radius, r, g, b, alpha);
            KillEffect.vertex(buf, entry, x + x1 * radius, y, z + z1 * radius, r, g, b, alpha);
            KillEffect.vertex(buf, entry, x + x1 * (radius - width), y, z + z1 * (radius - width), r, g, b, 0);
        }
    }

    private static float smooth(float t) {
        t = MathHelper.clamp(t, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static class KillBeam {
        final double x;
        final double y;
        final double z;
        final float radius;
        final long start;
        float alpha;
        float progress;

        KillBeam(double x, double y, double z, float radius) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.start = System.currentTimeMillis();
        }
    }
}
