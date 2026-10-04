package code.wiosvisual.features.impl.render;

import code.wiosvisual.utils.display.render.pipeline.WiosRender;
import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.color.ColorAssist;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

/**
 * Мягкая аура вокруг игроков: круг с радиальным градиентом, всегда развёрнутый к камере.
 * Плотная середина за телом плавно гаснет к краю.
 */
public class BodyGlow
extends Module {
    private static final int SEGMENTS = 36;

    private final SliderSettings size = new SliderSettings("Размер", "Насколько далеко расходится свечение").range(0.5f, 5.0f).step(0.1f).setValue(2.3f);
    private final SliderSettings transparency = new SliderSettings("Прозрачность", "Насколько плотное свечение").range(0.0f, 1.0f).step(0.05f).setValue(1.0f);
    private final BooleanSetting clientColor = new BooleanSetting("Цвет клиента", "Брать цвет из темы клиента").setValue(true);
    private final ColorSetting color = new ColorSetting("Цвет", "Свой цвет свечения").value(0xFFFFFFFF).visible(() -> !this.clientColor.isValue());

    public static BodyGlow getInstance() {
        return Instance.get(BodyGlow.class);
    }

    public BodyGlow() {
        super("BodyGlow", "Body Glow", ModuleCategory.RENDER);
        this.setup(this.size, this.transparency, this.clientColor, this.color);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.player == null || mc.world == null || mc.getEntityRenderDispatcher().camera == null) {
            return;
        }
        float opacity = Math.max(0.0f, Math.min(1.0f, this.transparency.getValue()));
        if (opacity <= 0.01f) {
            return;
        }
        Camera camera = mc.getEntityRenderDispatcher().camera;
        MatrixStack stack = e.getStack();
        float tickDelta = e.getPartialTicks();
        int rgb = (this.clientColor.isValue() ? ColorAssist.getHudColor() : this.color.getColor()) & 0xFFFFFF;
        int center = (int)(opacity * 200.0f) << 24 | rgb;
        int middle = (int)(opacity * 100.0f) << 24 | rgb;
        float outer = 0.8f + this.size.getValue() * 0.35f;
        float inner = outer * 0.45f;

        List<PlayerEntity> targets = new ArrayList<PlayerEntity>();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.isInvisible() || !player.isAlive()) continue;
            // От первого лица своя аура закрыла бы весь экран.
            if (player == mc.player && mc.options.getPerspective().isFirstPerson()) continue;
            targets.add(player);
        }
        if (targets.isEmpty()) {
            return;
        }

        // Полупрозрачный слой не пишет глубину, иначе ауры соседних игроков вырезают друг друга.
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (PlayerEntity player : targets) {
            Vec3d pos = player.getLerpedPos(tickDelta);
            float height = player.getHeight() - (player.isSneaking() ? 0.18f : 0.0f);

            stack.push();
            stack.translate(pos.x, pos.y + (double)(height * 0.5f), pos.z);
            stack.multiply(camera.getRotation());
            Matrix4f m = stack.peek().getPositionMatrix();
            for (int i = 0; i < SEGMENTS; ++i) {
                double a1 = (double)i * Math.PI * 2.0 / SEGMENTS;
                double a2 = (double)(i + 1) * Math.PI * 2.0 / SEGMENTS;
                float s1 = (float)-Math.sin(a1);
                float c1 = (float)Math.cos(a1);
                float s2 = (float)-Math.sin(a2);
                float c2 = (float)Math.cos(a2);
                // Середина: вырожденный четырёхугольник-треугольник от центра к внутреннему кольцу.
                buffer.vertex(m, 0.0f, 0.0f, 0.0f).color(center);
                buffer.vertex(m, s1 * inner, c1 * inner, 0.0f).color(middle);
                buffer.vertex(m, s2 * inner, c2 * inner, 0.0f).color(middle);
                buffer.vertex(m, 0.0f, 0.0f, 0.0f).color(center);
                // Край: от внутреннего кольца к полностью прозрачному внешнему.
                buffer.vertex(m, s1 * inner, c1 * inner, 0.0f).color(middle);
                buffer.vertex(m, s1 * outer, c1 * outer, 0.0f).color(rgb);
                buffer.vertex(m, s2 * outer, c2 * outer, 0.0f).color(rgb);
                buffer.vertex(m, s2 * inner, c2 * inner, 0.0f).color(middle);
            }
            stack.pop();
        }
        WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.QUADS, true, false), buffer);
    }
}
