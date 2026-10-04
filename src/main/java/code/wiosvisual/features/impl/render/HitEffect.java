package code.wiosvisual.features.impl.render;

import code.wiosvisual.events.player.AttackEvent;
import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.pipeline.WiosRender;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Эффект попадания: в точке удара вспыхивает светящееся кольцо-вихрь («пузырь») и/или крестик-маркер.
 * Текстуры лежат в assets/wiosvisual/textures/hiteffect (bubble.png, marker.png).
 */
public class HitEffect
extends Module {
    private static final Identifier BUBBLE = Identifier.of("wiosvisual", "textures/hiteffect/bubble.png");
    private static final Identifier MARKER = Identifier.of("wiosvisual", "textures/hiteffect/marker.png");
    private static final long BUBBLE_MS = 700L;
    private static final long MARKER_MS = 500L;
    private static final int MAX_EFFECTS = 16;

    public final SelectSetting style = new SelectSetting("Эффект", "Что показывать при попадании").value("Пузырь", "Маркер", "Оба").selected("Оба");
    public final SliderSettings size = new SliderSettings("Размер", "Размер эффекта в блоках").range(0.3f, 2.5f).setValue(1.0f).step(0.05f);
    public final BooleanSetting themeColor = new BooleanSetting("Цвет темы", "Красить эффект цветом клиента").setValue(true);
    public final ColorSetting customColor = new ColorSetting("Свой цвет", "Цвет эффекта, если цвет темы выключен").value(new Color(255, 255, 255).getRGB()).visible(() -> !this.themeColor.isValue());

    private final List<Hit> hits = new ArrayList<Hit>();

    public static HitEffect getInstance() {
        return Instance.get(HitEffect.class);
    }

    public HitEffect() {
        super("HitEffect", "Hit Effect", ModuleCategory.RENDER);
        this.setup(this.style, this.size, this.themeColor, this.customColor);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        this.hits.clear();
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        if (e.isCancelled() || HitEffect.mc.player == null) {
            return;
        }
        Entity entity = e.getEntity();
        if (!(entity instanceof LivingEntity) || entity == HitEffect.mc.player || !entity.isAlive()) {
            return;
        }
        // Точка удара: чуть выше середины тела цели, сдвинутая к игроку, чтобы эффект был виден, а не утопал в модели.
        Vec3d center = entity.getLerpedPos(1.0f).add(0.0, entity.getHeight() * 0.62, 0.0);
        Vec3d toPlayer = HitEffect.mc.player.getEyePos().subtract(center);
        double length = toPlayer.length();
        Vec3d position = length > 1.0E-4 ? center.add(toPlayer.multiply(Math.min(0.35, length * 0.3) / length)) : center;
        this.hits.add(new Hit(position));
        while (this.hits.size() > MAX_EFFECTS) {
            this.hits.remove(0);
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (this.hits.isEmpty() || HitEffect.mc.world == null) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean bubble = !this.style.isSelected("Маркер");
        boolean marker = !this.style.isSelected("Пузырь");
        int base = this.themeColor.isValue() ? ColorAssist.getClientColor() : this.customColor.getColor();
        int red = base >> 16 & 0xFF;
        int green = base >> 8 & 0xFF;
        int blue = base & 0xFF;
        Camera camera = HitEffect.mc.getEntityRenderDispatcher().camera;
        MatrixStack stack = e.getStack();
        float scale = this.size.getValue();

        BufferBuilder bubbleBuffer = bubble ? Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR) : null;
        BufferBuilder markerBuffer = marker ? Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR) : null;
        boolean bubbleUsed = false;
        boolean markerUsed = false;

        Iterator<Hit> iterator = this.hits.iterator();
        while (iterator.hasNext()) {
            Hit hit = iterator.next();
            long age = now - hit.time;
            if (age > Math.max(BUBBLE_MS, MARKER_MS)) {
                iterator.remove();
                continue;
            }
            if (bubble && age <= BUBBLE_MS) {
                float t = age / (float)BUBBLE_MS;
                float ease = 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
                float radius = scale * (0.35f + 0.85f * ease);
                int alpha = (int)(255.0f * (1.0f - t) * Math.min(1.0f, t * 8.0f + 0.35f));
                this.quad(bubbleBuffer, stack, camera, hit.position, radius, 160.0f * t * (hit.clockwise ? 1.0f : -1.0f), red, green, blue, alpha);
                bubbleUsed = true;
            }
            if (marker && age <= MARKER_MS) {
                float t = age / (float)MARKER_MS;
                float pop = t < 0.25f ? 0.55f + 0.6f * (t / 0.25f) : 1.15f - 0.15f * Math.min(1.0f, (t - 0.25f) / 0.75f);
                float radius = scale * 0.32f * pop;
                int alpha = (int)(255.0f * Math.min(1.0f, (1.0f - t) * 1.6f));
                this.quad(markerBuffer, stack, camera, hit.position, radius, 0.0f, red, green, blue, alpha);
                markerUsed = true;
            }
        }
        if (bubbleUsed) {
            WiosRender.draw(WiosRender.posTexColor(BUBBLE, VertexFormat.DrawMode.QUADS, false, false, true), bubbleBuffer);
        }
        if (markerUsed) {
            WiosRender.draw(WiosRender.posTexColor(MARKER, VertexFormat.DrawMode.QUADS, false, false, false), markerBuffer);
        }
    }

    /** Билборд: квадрат, повёрнутый к камере, с дополнительным вращением в своей плоскости. */
    private void quad(BufferBuilder buffer, MatrixStack stack, Camera camera, Vec3d position, float radius, float roll, int r, int g, int b, int a) {
        stack.push();
        stack.translate((float)position.x, (float)position.y, (float)position.z);
        stack.multiply(camera.getRotation());
        if (roll != 0.0f) {
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        }
        Matrix4f m = stack.peek().getPositionMatrix();
        buffer.vertex(m, -radius, -radius, 0.0f).texture(0.0f, 1.0f).color(r, g, b, a);
        buffer.vertex(m, -radius, radius, 0.0f).texture(0.0f, 0.0f).color(r, g, b, a);
        buffer.vertex(m, radius, radius, 0.0f).texture(1.0f, 0.0f).color(r, g, b, a);
        buffer.vertex(m, radius, -radius, 0.0f).texture(1.0f, 1.0f).color(r, g, b, a);
        stack.pop();
    }

    private static final class Hit {
        final Vec3d position;
        final long time = System.currentTimeMillis();
        final boolean clockwise = Math.random() < 0.5;

        Hit(Vec3d position) {
            this.position = position;
        }
    }
}
