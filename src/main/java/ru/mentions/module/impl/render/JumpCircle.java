package ru.mentions.module.impl.render;

import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.events.player.JumpEvent;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import ru.mentions.module.api.Category;
import ru.mentions.module.api.Module;
import ru.mentions.module.api.ModuleInfo;
import ru.mentions.module.api.preview.ModulePreview;
import ru.mentions.module.api.preview.PreviewContext;
import ru.mentions.module.api.preview.PreviewSettings;
import ru.mentions.module.api.settings.impl.BooleanSetting;
import ru.mentions.module.api.settings.impl.ButtonSetting;
import ru.mentions.module.api.settings.impl.ModeSetting;
import ru.mentions.module.api.settings.impl.SliderSetting;
import ru.mentions.utils.animation.Animation;
import ru.mentions.utils.animation.Easings;
import ru.mentions.utils.colors.ColorUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
        name = "Jump Circle",
        desc = "Кружочек под тобой при прижке",
        category = Category.RENDER
)
public class JumpCircle extends Module implements ModulePreview {
    private final List<Circle> circles = new ArrayList<>();

    public ButtonSetting previewButton = PreviewSettings.button(this);

    /** Классика — прежний круг; Вихрь — покадровая анимация вращающегося кольца; Лучи — сияющее кольцо из лучей. */
    public ModeSetting mode = new ModeSetting(this, "Режим", "Классика", "Вихрь", "Лучи");

    private static final int SWIRL_FRAMES = 100;
    private static final Identifier CLASSIC_TEXTURE = Identifier.of("client", "textures/visuals/jump_new.png");
    private static final Identifier RAYS_TEXTURE = Identifier.of("wiosvisual", "textures/jumpcircle/rays.png");
    private static final Identifier[] SWIRL_TEXTURES = new Identifier[SWIRL_FRAMES];

    static {
        for (int i = 0; i < SWIRL_FRAMES; i++) {
            SWIRL_TEXTURES[i] = Identifier.of("wiosvisual", "textures/jumpcircle/frame_" + i + ".png");
        }
    }

    public SliderSetting size_c = new SliderSetting(this,"Размер",1,0.2F,3,0.1F);
    public BooleanSetting glow = new BooleanSetting(this, "Свечение", true);

    private final PreviewSettings previewSettings = PreviewSettings.of(this, 3.5F, 0F, 2F);

    BufferAllocator allocator = new BufferAllocator(1 << 18);

    private static final int QUAD_BUFFER_SIZE_BYTES = 1 << 10;
    private static final RenderPipeline TEXTURED_QUADS_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
                    .withLocation(Identifier.of("client", "pipeline/world/textured_quads"))
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build()
    );

    @EventHandler
    public void onJump(JumpEvent e) {
        circles.add(new Circle(mc.player.getEntityPos().add(0, 0.05, 0)));
    }

    @Override
    public PreviewSettings previewSettings() {
        return previewSettings;
    }

    @Override
    public void previewSpawn(PreviewContext ctx) {
        circles.add(new Circle(ctx.anchor().add(0, 0.05, 0)));
    }

    @Override
    public void previewStop() {
        circles.clear();
    }

    @EventHandler
    public void onRender(WorldRenderEvent e) {
        if (circles.isEmpty()) {
            return;
        }

        circles.removeIf(c -> System.currentTimeMillis() - c.time > 2500);

        if (circles.isEmpty()) {
            return;
        }
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);
        MatrixStack pose = ru.mentions.utils.other.WorldStack.create();

        int color = ColorUtil.fade(1);
        int alpha = 255;

        for (Circle c : circles) {
            if (System.currentTimeMillis() - c.time > 800 && !c.isBack) {
                c.animation.run(0, 0.5F, Easings.SINE_OUT);
                c.animation2.run(2, 0.5F, Easings.SINE_OUT);
                c.isBack = true;
            }

            c.animation.update();
            c.animation2.update();
            float rad = (float) c.animation2.getValue();

            double posX = c.vector3d.x - mc.gameRenderer.getCamera().getCameraPos().x;
            double posY = c.vector3d.y - mc.gameRenderer.getCamera().getCameraPos().y;
            double posZ = c.vector3d.z - mc.gameRenderer.getCamera().getCameraPos().z;

            float size = size_c.getValue() * rad;

            long age = System.currentTimeMillis() - c.time;
            Identifier texture = CLASSIC_TEXTURE;
            float spin = 0.0F;
            if (mode.is("Вихрь")) {
                // Кадры проигрываются за первые 1.8 секунды, потом круг гаснет на последнем кадре.
                float t = Math.min(1.0F, age / 1800.0F);
                texture = SWIRL_TEXTURES[Math.min(SWIRL_FRAMES - 1, (int) (t * (SWIRL_FRAMES - 1)))];
            } else if (mode.is("Лучи")) {
                texture = RAYS_TEXTURE;
                spin = age * 0.06F;
                size *= 1.0F + 0.06F * (float) Math.sin(age / 140.0);
            }

            pose.push();
            pose.translate(posX, posY, posZ);
            pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90 ));
            if (spin != 0.0F) {
                pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spin));
            }

            MatrixStack.Entry entry = pose.peek();
            Matrix4f matrix4f = entry.getPositionMatrix();
            Matrix3f normalMatrix = entry.getNormalMatrix();
            VertexConsumer buffer = immediate.getBuffer(ROMB_ESP.apply(texture));

            drawTexturedQuad(buffer, matrix4f, normalMatrix, -size / 2f, -size / 2f, size, size, new int[]{
                    ColorUtil.fade(0),
                    ColorUtil.fade(90),
                    ColorUtil.fade(180)
                    ,ColorUtil.fade(360)
            }, (int) (alpha * c.animation.get()));

            pose.pop();

            if (glow.getValue()) {
                pose.push();
                pose.translate(posX, posY, posZ);

                drawGlowLayers(immediate, pose, size , (float) c.animation.get(), texture);
                pose.pop();
            }
        }

        immediate.draw();

    }

    public static final Function<Identifier, RenderLayer> ROMB_ESP =
            Util.memoize(texture -> {
                RenderSetup setup = RenderSetup.builder(TEXTURED_QUADS_PIPELINE)
                        .texture("Sampler0", texture)
                        .translucent()
                        .expectedBufferSize(1536)
                        .build();
                return RenderLayer.of("wtex", setup);
            });

    private void drawGlowLayers(VertexConsumerProvider.Immediate immediate, MatrixStack pose, float radius, float alpha, Identifier texture) {
        VertexConsumer buffer = immediate.getBuffer(ROMB_ESP.apply(texture));
        int layers = 20;
        float maxHeight = radius * 0.15f;
        float expand   = radius * 0.25f;

        Vector3f normal = new Vector3f(0, 1, 0);
        pose.peek().getNormalMatrix().transform(normal);
        normal.normalize();

        for (int i = 0; i < layers; i++) {
            float progress   = i / (float) layers;
            float yOff       = maxHeight * progress;
            float layerAlpha = alpha * (1f - progress) * 0.15f;
            if (layerAlpha <= 0.004f) continue;
            float r    = radius + expand * progress;
            float half = r / 2f;
            int   a    = (int) (layerAlpha * 255);

            Matrix4f matrix = pose.peek().getPositionMatrix();

            int c0   = ColorUtil.fade(0);
            int c90  = ColorUtil.fade(90);
            int c180 = ColorUtil.fade(180);
            int c270 = ColorUtil.fade(270);

            buffer.vertex(matrix, -half, yOff, -half).color((c0   >> 16) & 0xFF, (c0   >> 8) & 0xFF, c0   & 0xFF, a).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(normal.x, normal.y, normal.z);
            buffer.vertex(matrix, -half, yOff,  half).color((c90  >> 16) & 0xFF, (c90  >> 8) & 0xFF, c90  & 0xFF, a).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(normal.x, normal.y, normal.z);
            buffer.vertex(matrix,  half, yOff,  half).color((c180 >> 16) & 0xFF, (c180 >> 8) & 0xFF, c180 & 0xFF, a).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(normal.x, normal.y, normal.z);
            buffer.vertex(matrix,  half, yOff, -half).color((c270 >> 16) & 0xFF, (c270 >> 8) & 0xFF, c270 & 0xFF, a).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(normal.x, normal.y, normal.z);
        }
    }

    private void drawTexturedQuad(VertexConsumer buffer, Matrix4f matrix, Matrix3f normalMatrix, float x, float y,
                                  float width, float height,  int[] ints, int alpha) {

        Vector3f normal = new Vector3f(0, 0, 1);
        normalMatrix.transform(normal);
        normal.normalize();

        float x1 = x;
        float y1 = y;
        float x2 = x + width;
        float y2 = y + height;

        buffer.vertex(matrix, x1, y1, 0.0f).color(ColorUtil.replAlpha(ints[0],alpha)).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(normal.x, normal.y, normal.z);
        buffer.vertex(matrix, x2, y1, 0.0f).color(ColorUtil.replAlpha(ints[1],alpha)).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(normal.x, normal.y, normal.z);
        buffer.vertex(matrix, x2, y2, 0.0f).color(ColorUtil.replAlpha(ints[2],alpha)).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(normal.x, normal.y, normal.z);
        buffer.vertex(matrix, x1, y2, 0.0f).color(ColorUtil.replAlpha(ints[3],alpha)).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(normal.x, normal.y, normal.z);
    }

    private class Circle {

        private final Vec3d vector3d;

        private final long time;
        private final Animation animation = new Animation();
        private final Animation animation2 = new Animation();
        private boolean isBack;

        public Circle(Vec3d vector3d) {
            this.vector3d = vector3d;
            time = System.currentTimeMillis();
            animation.run(1, 0.5F, Easings.SINE_OUT);
            animation2.run(1, 0.5F, Easings.SINE_OUT);
        }

    }
}
