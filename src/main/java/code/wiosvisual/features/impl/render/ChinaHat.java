package code.wiosvisual.features.impl.render;

import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.math.calc.CalcVector;
import code.wiosvisual.utils.display.render.pipeline.WiosRender;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.awt.Color;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 *
 *
 *
 */
public class ChinaHat
extends Module {
    private static final int SEGMENTS = 60;
    /** Конус над макушкой, а не на уровне глаз. */
    private static final double ABOVE_EYES = 0.10;
    /** Насколько опускается шляпа, когда игрок приседает. */
    private static final double SNEAK_DROP = 0.18;

    private final SliderSettings height = new SliderSettings("Высота", "Высота конуса шляпы").setValue(0.3f).range(0.1f, 0.6f).step(0.05f);
    private final SliderSettings radius = new SliderSettings("Радиус", "Радиус полей шляпы").setValue(0.5f).range(0.1f, 1.0f).step(0.05f);
    private final BooleanSetting followSneaking = new BooleanSetting("Учитывать приседание", "Опускать шляпу, когда игрок приседает").setValue(true);
    private final BooleanSetting useClientColor = new BooleanSetting("Цвет клиента", "Брать цвет из темы клиента").setValue(true);
    private final ColorSetting color = new ColorSetting("Кастомный цвет", "Цвет шляпы, если цвет клиента выключен").setColor(new Color(120, 80, 255).getRGB()).visible(() -> !this.useClientColor.isValue());

    public ChinaHat() {
        super("ChinaHat", "China Hat", ModuleCategory.RENDER);
        this.setup(this.height, this.radius, this.followSneaking, this.useClientColor, this.color);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        ClientPlayerEntity player = ChinaHat.mc.player;
        if (player == null || ChinaHat.mc.world == null || player.isInvisible()) {
            return;
        }
        if (ChinaHat.mc.options.getPerspective().isFirstPerson()) {
            return;
        }

        int rgb = (this.useClientColor.isValue() ? ColorAssist.getHudColor() : this.color.getColor()) & 0xFFFFFF;
        int topArgb = 180 << 24 | rgb;
        int edgeArgb = 80 << 24 | rgb;
        int outlineArgb = 255 << 24 | rgb;

        Vec3d pos = CalcVector.lerpPosition((Entity)player);
        double x = pos.x;
        double y = pos.y + player.getEyeHeight(player.getPose()) + ABOVE_EYES;
        double z = pos.z;
        if (this.followSneaking.isValue() && player.isSneaking()) {
            y -= SNEAK_DROP;
        }
        float hatRadius = this.radius.getValue();
        float coneHeight = this.height.getValue();

        // Конус симметричен вокруг вертикали, поэтому поворот за головой не нужен:
        // любая ориентация выглядит одинаково.
        Matrix4f matrix = e.getStack().peek().getPositionMatrix();
        // Состояние (blend/no-cull/depth-test без записи глубины) запечено в слой WiosRender.

        BufferBuilder cone = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < SEGMENTS; ++i) {
            double a1 = Math.PI * 2 * (double)i / (double)SEGMENTS;
            double a2 = Math.PI * 2 * (double)(i + 1) / (double)SEGMENTS;
            cone.vertex(matrix, (float)x, (float)(y + coneHeight), (float)z).color(topArgb);
            cone.vertex(matrix, (float)(x - Math.sin(a2) * hatRadius), (float)y, (float)(z + Math.cos(a2) * hatRadius)).color(edgeArgb);
            cone.vertex(matrix, (float)(x - Math.sin(a1) * hatRadius), (float)y, (float)(z + Math.cos(a1) * hatRadius)).color(edgeArgb);
        }
        WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.TRIANGLES, true, false), cone);

        // Второй буфер — только после отправки первого: Tessellator у игры один.
        BufferBuilder ring = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < SEGMENTS; ++i) {
            double a1 = Math.PI * 2 * (double)i / (double)SEGMENTS;
            double a2 = Math.PI * 2 * (double)(i + 1) / (double)SEGMENTS;
            ring.vertex(matrix, (float)(x - Math.sin(a1) * hatRadius), (float)y, (float)(z + Math.cos(a1) * hatRadius)).color(outlineArgb);
            ring.vertex(matrix, (float)(x - Math.sin(a2) * hatRadius), (float)y, (float)(z + Math.cos(a2) * hatRadius)).color(outlineArgb);
        }
        WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.DEBUG_LINES, true, false), ring);
    }
}
