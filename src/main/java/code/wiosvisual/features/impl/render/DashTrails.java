package code.wiosvisual.features.impl.render;

import code.wiosvisual.utils.display.render.pipeline.WiosRender;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.color.ColorAssist;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

/**
 * Шлейф рывка: при быстром перемещении за игроком остаётся россыпь светящихся
 * спрайтов, которые сносит назад и постепенно гасит.
 */
public class DashTrails
extends Module {
    private static final Identifier BLOOM = Identifier.of((String)"textures/bloom.png");
    private static final Identifier DASH = Identifier.of((String)"textures/particles/spark.png");
    private static final Random RANDOM = new Random();

    private final SliderSettings lifeTime = new SliderSettings("Время жизни", "Сколько секунд живёт спрайт").range(0.35f, 2.5f).setValue(0.9f).step(0.05f);
    private final SliderSettings density = new SliderSettings("Плотность", "Сколько спрайтов сыпать на блок пути").range(4.0f, 20.0f).setValue(12.0f).step(0.5f);
    private final SliderSettings scale = new SliderSettings("Масштаб", "Размер спрайтов").range(0.55f, 1.8f).setValue(1.0f).step(0.05f);
    private final SliderSettings threshold = new SliderSettings("Порог скорости", "С какой скорости за тик начинается шлейф").range(0.02f, 0.4f).setValue(0.08f).step(0.01f);
    private final BooleanSetting selfOnly = new BooleanSetting("Только от себя", "Не рисовать в первом лице").setValue(true);

    private final List<DashSprite> sprites = new ArrayList<DashSprite>();
    private Vec3d tickStartPos;

    public static DashTrails getInstance() {
        return Instance.get(DashTrails.class);
    }

    public DashTrails() {
        super("DashTrails", "Dash Trails", ModuleCategory.RENDER);
        this.setup(this.lifeTime, this.density, this.scale, this.threshold, this.selfOnly);
    }

    @Override
    public void deactivate() {
        this.sprites.clear();
        this.tickStartPos = null;
        super.deactivate();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (DashTrails.mc.player == null || DashTrails.mc.world == null) {
            this.sprites.clear();
            this.tickStartPos = null;
            return;
        }
        Vec3d current = new net.minecraft.util.math.Vec3d(DashTrails.mc.player.getX(), DashTrails.mc.player.getY(), DashTrails.mc.player.getRandomBodyY());
        if (this.tickStartPos != null) {
            this.handleMovement(this.tickStartPos, current);
        }
        this.tickStartPos = current;
        for (DashSprite sprite : this.sprites) {
            sprite.update();
        }
        this.sprites.removeIf(DashSprite::shouldRemove);
    }

    private void handleMovement(Vec3d from, Vec3d to) {
        Vec3d delta = to.subtract(from);
        double distance = delta.length();
        if (distance < (double)this.threshold.getValue() || DashTrails.mc.player.hasVehicle()) {
            return;
        }
        Vec3d direction = delta.multiply(1.0 / distance);
        Vec3d back = direction.multiply(-1.0);
        Vec3d side = new Vec3d(-direction.z, 0.0, direction.x);
        side = side.lengthSquared() < 1.0E-4 ? new Vec3d(1.0, 0.0, 0.0) : side.normalize();
        float height = DashTrails.mc.player.getHeight() - (DashTrails.mc.player.isInSneakingPose() ? 0.15f : 0.0f);
        int bursts = MathHelper.clamp((int)((int)Math.ceil(distance * (double)(this.density.getValue() * 1.35f))), (int)1, (int)5);
        for (int i = 0; i < bursts; ++i) {
            double t = bursts == 1 ? 1.0 : (double)i / (double)(bursts - 1);
            Vec3d base = from.add(delta.multiply(t)).add(0.0, (double)height * 0.18, 0.0);
            this.spawnBurst(base, back, side, height, distance);
        }
    }

    private void spawnBurst(Vec3d base, Vec3d back, Vec3d side, float height, double distance) {
        int lifeTicks = Math.max(8, (int)(this.lifeTime.getValue() * 20.0f));
        float scaleValue = this.scale.getValue();
        float factor = MathHelper.clamp((float)((float)(distance * 6.0)), (float)0.0f, (float)1.3f);
        int count = 3 + RANDOM.nextInt(2) + (factor > 0.75f ? 1 : 0);
        for (int i = 0; i < count; ++i) {
            double up = (RANDOM.nextDouble() - 0.1) * (double)height * 0.85;
            double sideOffset = (RANDOM.nextDouble() - 0.5) * (0.28 + (double)factor * 0.15);
            double backOffset = RANDOM.nextDouble() * (0.2 + (double)factor * 0.18);
            Vec3d position = base.add(0.0, up, 0.0).add(side.multiply(sideOffset)).add(back.multiply(backOffset));
            Vec3d velocity = back.multiply(0.012 + (double)factor * 0.02 + RANDOM.nextDouble() * 0.01).add(side.multiply((RANDOM.nextDouble() - 0.5) * 0.008)).add(0.0, (RANDOM.nextDouble() - 0.5) * 0.008 + 0.002, 0.0);
            float width = scaleValue * (0.34f + RANDOM.nextFloat() * 0.18f + factor * 0.08f);
            float spriteHeight = width * (0.24f + RANDOM.nextFloat() * 0.08f);
            float roll = (float)((RANDOM.nextDouble() - 0.5) * 18.0);
            this.sprites.add(new DashSprite(position, velocity, width, spriteHeight, roll, lifeTicks));
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (DashTrails.mc.player == null || DashTrails.mc.world == null || this.sprites.isEmpty()) {
            return;
        }
        if (this.selfOnly.isValue() && DashTrails.mc.options.getPerspective().isFirstPerson()) {
            return;
        }
        float partialTicks = event.getPartialTicks();
        MatrixStack matrices = event.getStack();
        Camera camera = DashTrails.mc.getEntityRenderDispatcher().camera;
        int tint = ColorAssist.getClientColor();
        int baseR = tint >> 16 & 0xFF;
        int baseG = tint >> 8 & 0xFF;
        int baseB = tint & 0xFF;


        // три прохода, каждый со своей текстурой: свечение, тело, ядро
        this.drawPass(matrices, camera, partialTicks, BLOOM, Pass.BLOOM, baseR, baseG, baseB);
        this.drawPass(matrices, camera, partialTicks, DASH, Pass.BODY, baseR, baseG, baseB);
        this.drawPass(matrices, camera, partialTicks, DASH, Pass.CORE, baseR, baseG, baseB);

    }

    private void drawPass(MatrixStack matrices, Camera camera, float partialTicks, Identifier texture, Pass pass, int baseR, int baseG, int baseB) {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        boolean drawn = false;
        for (DashSprite sprite : this.sprites) {
            float alpha = sprite.getAlpha(partialTicks);
            if (alpha <= 0.01f) continue;
            int alphaInt = MathHelper.clamp((int)((int)(255.0f * alpha)), (int)0, (int)255);
            if (alphaInt <= 1) continue;
            float progress = sprite.getProgress(partialTicks);
            Vec3d position = sprite.interpolate(partialTicks);
            float sizeFactor = 1.0f - progress * 0.18f;
            float width = sprite.width * sizeFactor;
            float height = sprite.height * (1.08f - progress * 0.15f);
            switch (pass) {
                case BLOOM -> {
                    float bloom = width * (1.85f - progress * 0.2f);
                    int color = this.pack(baseR, baseG, baseB, (int)((float)alphaInt * 0.55f));
                    drawn |= this.appendSprite(buffer, matrices, camera, position, bloom, bloom, sprite.roll, color);
                }
                case BODY -> {
                    int color = this.pack(baseR, baseG, baseB, alphaInt);
                    drawn |= this.appendSprite(buffer, matrices, camera, position, width, height, sprite.roll, color);
                }
                case CORE -> {
                    int color = this.pack(255, 255, 255, (int)((float)alphaInt * 0.95f));
                    drawn |= this.appendSprite(buffer, matrices, camera, position, width * 0.78f, height * 0.78f, sprite.roll, color);
                }
            }
        }
        if (drawn) {
            BuiltBuffer built = buffer.endNullable();
            if (built != null) {
                WiosRender.draw(WiosRender.posTexColor(texture, VertexFormat.DrawMode.QUADS, true, false), built);
            }
        }
    }

    private boolean appendSprite(BufferBuilder buffer, MatrixStack matrices, Camera camera, Vec3d position, float width, float height, float roll, int color) {
        if ((color >> 24 & 0xFF) <= 0) {
            return false;
        }
        matrices.push();
        matrices.translate(position.x, position.y, position.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        if (roll != 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        }
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float halfW = width * 0.5f;
        float halfH = height * 0.5f;
        buffer.vertex(matrix, -halfW, -halfH, 0.0f).texture(0.0f, 1.0f).color(color);
        buffer.vertex(matrix, -halfW, halfH, 0.0f).texture(0.0f, 0.0f).color(color);
        buffer.vertex(matrix, halfW, halfH, 0.0f).texture(1.0f, 0.0f).color(color);
        buffer.vertex(matrix, halfW, -halfH, 0.0f).texture(1.0f, 1.0f).color(color);
        matrices.pop();
        return true;
    }

    private int pack(int r, int g, int b, int alpha) {
        return MathHelper.clamp((int)alpha, (int)0, (int)255) << 24
                | MathHelper.clamp((int)r, (int)0, (int)255) << 16
                | MathHelper.clamp((int)g, (int)0, (int)255) << 8
                | MathHelper.clamp((int)b, (int)0, (int)255);
    }

    private static enum Pass {
        BLOOM,
        BODY,
        CORE;
    }

    private static class DashSprite {
        private Vec3d previousPosition;
        private Vec3d position;
        private Vec3d velocity;
        private final float width;
        private final float height;
        private final float roll;
        private final int maxAge;
        private int age;

        private DashSprite(Vec3d position, Vec3d velocity, float width, float height, float roll, int maxAge) {
            this.previousPosition = position;
            this.position = position;
            this.velocity = velocity;
            this.width = width;
            this.height = height;
            this.roll = roll;
            this.maxAge = maxAge;
        }

        private void update() {
            this.previousPosition = this.position;
            this.position = this.position.add(this.velocity);
            this.velocity = this.velocity.multiply(0.9).add(0.0, 0.0015, 0.0);
            ++this.age;
        }

        private Vec3d interpolate(float partialTicks) {
            return new Vec3d(MathHelper.lerp((double)partialTicks, (double)this.previousPosition.x, (double)this.position.x), MathHelper.lerp((double)partialTicks, (double)this.previousPosition.y, (double)this.position.y), MathHelper.lerp((double)partialTicks, (double)this.previousPosition.z, (double)this.position.z));
        }

        private float getProgress(float partialTicks) {
            return MathHelper.clamp((float)(((float)this.age + partialTicks) / (float)this.maxAge), (float)0.0f, (float)1.0f);
        }

        private float getAlpha(float partialTicks) {
            float fade = 1.0f - this.getProgress(partialTicks);
            return MathHelper.clamp((float)((float)Math.pow(Math.sin((double)fade * Math.PI * 0.5), 0.85)), (float)0.0f, (float)1.0f);
        }

        private boolean shouldRemove() {
            return this.age >= this.maxAge;
        }
    }
}
