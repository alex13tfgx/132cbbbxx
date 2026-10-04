package code.wiosvisual.features.impl.render;

import code.wiosvisual.utils.display.render.pipeline.WiosRender;
import code.wiosvisual.common.animation.Animation;
import code.wiosvisual.common.animation.Direction;
import code.wiosvisual.common.animation.implement.OutBack;
import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class TargetESP
extends Module {
    private static final Identifier GHOST_GLOW = Identifier.of((String)"wiosvisual", (String)"images/particle/ghost-glow.png");
    private static final Identifier CHAIN_TEXTURE = Identifier.of((String)"wiosvisual", (String)"images/world/chain.png");
    private static final Identifier CUBE_TEXTURE = Identifier.of((String)"wiosvisual", (String)"images/world/cube.png");
    private static final Identifier BLOOM_GLOW = Identifier.of((String)"wiosvisual", (String)"images/particle/glow.png");
    private static final float TARGET_FRAME_TIME = 1000.0f / 60.0f;

    private final Animation espAnim = new OutBack().setMs(300).setValue(1.0);

    public final SelectSetting mode = new SelectSetting("Режим", "Тип TargetESP").value("Rhomb", "Души", "Circle").selected("Rhomb");
    public final SelectSetting rhombAnim = new SelectSetting("Анимация ромба", "Ромб стоит на месте или крутится").value("Стоит", "Крутится").selected("Крутится").visible(() -> this.mode.isSelected("Rhomb"));
    public final SliderSettings planeSize = new SliderSettings("Размер плоскостей", "Размер плоскостей Vortex / Brackets").range(0.3f, 3.0f).setValue(1.2f).step(0.05f).visible(() -> this.mode.isSelected("Vortex") || this.mode.isSelected("Brackets"));
    public final SliderSettings planeSpeed = new SliderSettings("Скорость плоскостей", "Скорость вращения Vortex / Brackets").range(0.0f, 4.0f).setValue(1.0f).step(0.05f).visible(() -> this.mode.isSelected("Vortex") || this.mode.isSelected("Brackets"));
    public final BooleanSetting planeHurtRed = new BooleanSetting("Красный при ударе", "Vortex / Brackets краснеют при попадании по цели").setValue(true).visible(() -> this.mode.isSelected("Vortex") || this.mode.isSelected("Brackets"));
    public final SelectSetting ghostStyle = new SelectSetting("Ghost Style", "Ghost visual style").value("Trails", "Souls").selected("Trails").visible(() -> this.mode.isSelected("Ghost"));
    public final SliderSettings crystalRotationSpeed = new SliderSettings("Скорость вращения кристаллов", "Скорость вращения кристаллов").range(0.1f, 2.0f).setValue(0.5f).visible(() -> this.mode.isSelected("Crystals"));
    public final ColorSetting color1 = new ColorSetting("Цвет 1", "Первый цвет градиента").value(new Color(150, 130, 255).getRGB());
    public final ColorSetting color2 = new ColorSetting("Цвет 2", "Второй цвет градиента").value(new Color(120, 180, 255).getRGB());
    public final SliderSettings scale = new SliderSettings("\u0420\u0430\u0437\u043c\u0435\u0440", "\u0420\u0430\u0437\u043c\u0435\u0440 \u0438\u043d\u0434\u0438\u043a\u0430\u0442\u043e\u0440\u0430").range(0.3f, 3.0f).setValue(1.2f).step(0.05f);
    public final SliderSettings range = new SliderSettings("\u0414\u0430\u043b\u044c\u043d\u043e\u0441\u0442\u044c", "\u041d\u0430 \u043a\u0430\u043a\u043e\u043c \u0440\u0430\u0441\u0441\u0442\u043e\u044f\u043d\u0438\u0438 \u043b\u043e\u0432\u0438\u0442\u044c \u0446\u0435\u043b\u044c").range(3.0f, 60.0f).setValue(30.0f).step(1.0f);
    public final BooleanSetting playersOnly = new BooleanSetting("\u0411\u0435\u0437 \u0441\u043f\u0435\u043a\u0442\u0430\u0442\u043e\u0440\u043e\u0432", "\u0418\u0433\u043d\u043e\u0440\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u0438\u0433\u0440\u043e\u043a\u043e\u0432 \u0432 \u0440\u0435\u0436\u0438\u043c\u0435 \u043d\u0430\u0431\u043b\u044e\u0434\u0430\u0442\u0435\u043b\u044f").setValue(true);
    public final ColorSetting color3 = new ColorSetting("Цвет 3", "Третий цвет для Ghost").value(new Color(235, 120, 220).getRGB()).visible(() -> this.mode.isSelected("Ghost"));
    public final SliderSettings shardSize = new SliderSettings("Размер осколков", "Размер кристаллов вокруг цели").range(0.5f, 1.5f).setValue(1.0f).step(0.1f).visible(() -> this.mode.isSelected("Осколки"));
    public final SliderSettings shardAlpha = new SliderSettings("Прозрачность осколков", "Яркость осколков").range(65.0f, 255.0f).setValue(255.0f).step(1.0f).visible(() -> this.mode.isSelected("Осколки"));

    private Vec3d smoothedPos;
    private LivingEntity lastTarget;
    private Entity lastRenderedTarget;
    private float movingValue;
    private float hurtProgress;
    private float rotationAngle;
    private float soulsRotation;
    private float circleStep;
    private double smoothY;
    private double smoothY2;
    private long ghostLastUpdateTime = System.currentTimeMillis();
    private long lastFrameTime = System.currentTimeMillis();
    private final List<Crystal> crystalList = new ArrayList<Crystal>();
    private final List<GhostTrail> ghostTrails = new ArrayList<GhostTrail>();
    private final List<Vector3f> shardPoints = new ArrayList<Vector3f>();
    private LivingEntity shardTarget;

    private static final int SHARD_ATTEMPTS = 15;
    private static final Vector3f[] SHARD_VERTICES = new Vector3f[]{
        new Vector3f(0.0f, 1.5f, 0.0f), new Vector3f(0.0f, -1.5f, 0.0f),
        new Vector3f(1.0f, 0.0f, 0.0f), new Vector3f(-1.0f, 0.0f, 0.0f),
        new Vector3f(0.0f, 0.0f, 1.0f), new Vector3f(0.0f, 0.0f, -1.0f)};
    private static final int[][] SHARD_FACES = new int[][]{
        {0, 2, 4}, {0, 4, 3}, {0, 3, 5}, {0, 5, 2},
        {1, 4, 2}, {1, 3, 4}, {1, 5, 3}, {1, 2, 5}};
    private static final float[] SHARD_SHADES = new float[]{
        1.0f, 0.8f, 0.6f, 0.9f, 0.7f, 0.5f, 0.4f, 0.6f};

    public TargetESP() {
        super("TargetEsp", "Target Esp", ModuleCategory.RENDER);
        this.setup(this.mode, this.rhombAnim, this.ghostStyle, this.crystalRotationSpeed, this.shardSize, this.shardAlpha, this.planeSize, this.planeSpeed, this.planeHurtRed, this.scale, this.range, this.playersOnly, this.color1, this.color2, this.color3);
    }

    public static TargetESP getInstance() {
        return Instance.get(TargetESP.class);
    }

    private LivingEntity resolveTarget() {
        if (mc.player == null || mc.world == null) {
            return null;
        }
        Entity targeted = this.raycastPlayer(this.range.getValue());
        if (!(targeted instanceof PlayerEntity)) {
            return null;
        }
        PlayerEntity player = (PlayerEntity)targeted;
        if (player == mc.player || !player.isAlive()) {
            return null;
        }
        if (this.playersOnly.isValue() && player.isSpectator()) {
            return null;
        }
        if (!mc.player.canSee((Entity)player)) {
            return null;
        }
        if (player.isInvisible()) {
            return null;
        }
        return player;
    }

    /**
     * Crosshair raycast over the configured distance. {@code mc.targetedEntity} only covers melee
     * reach, which made the esp appear just a couple of blocks away from the target.
     */
    private Entity raycastPlayer(float distance) {
        ClientPlayerEntity self = mc.player;
        if (self == null || mc.world == null) {
            return null;
        }
        float tickDelta = tickCounter.getTickProgress(false);
        Vec3d eyes = self.getCameraPosVec(tickDelta);
        Vec3d look = self.getRotationVec(tickDelta);
        Vec3d reach = eyes.add(look.multiply((double)distance));
        Box box = self.getBoundingBox().stretch(look.multiply((double)distance)).expand(1.0);
        EntityHitResult hit = ProjectileUtil.raycast((Entity)self, (Vec3d)eyes, (Vec3d)reach, (Box)box, entity -> entity instanceof PlayerEntity && entity != self && entity.isAlive(), (double)(distance * distance));
        return hit == null ? null : hit.getEntity();
    }

    private float getDeltaTime() {
        long currentTime = System.currentTimeMillis();
        float deltaMs = currentTime - this.lastFrameTime;
        this.lastFrameTime = currentTime;
        deltaMs = Math.max(1.0f, Math.min(deltaMs, 100.0f));
        return deltaMs / TARGET_FRAME_TIME;
    }

    @EventHandler
    public void onRender3D(WorldRenderEvent e) {
        float deltaTime = this.getDeltaTime();
        LivingEntity target = this.resolveTarget();
        if (target == null) {
            this.smoothedPos = null;
            this.lastTarget = null;
            this.sizeRefTarget = null;
            this.ghostTrails.clear();
            this.ghostLastUpdateTime = System.currentTimeMillis();
            this.shardPoints.clear();
            this.shardTarget = null;
            this.espAnim.setDirection(Direction.BACKWARDS);
            this.smoothY = 0.0;
            this.smoothY2 = 0.0;
            return;
        }
        this.espAnim.setDirection(Direction.FORWARDS);
        float alpha = this.espAnim.getOutput().floatValue();
        if (alpha <= 0.01f) {
            return;
        }
        this.movingValue += 2.0f * deltaTime;
        if (this.movingValue > 360000.0f) {
            this.movingValue = 0.0f;
        }
        float hurtDecay = 0.1f * deltaTime;
        this.hurtProgress = target.hurtTime > 0 ? (float)target.hurtTime / 10.0f : Math.max(0.0f, this.hurtProgress - hurtDecay);
        this.circleStep += 0.06f * deltaTime;

        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d camPos = camera.pos;
        float partialTicks = e.getPartialTicks();
        Vec3d targetPos = target.getLerpedPos(partialTicks);
        if (this.lastTarget != target) {
            this.lastTarget = target;
            this.ghostTrails.clear();
            this.ghostLastUpdateTime = System.currentTimeMillis();
        }
        // Без сглаживания: индикатор стоит ровно на цели и не отстаёт при её движении.
        this.smoothedPos = targetPos;

        MatrixStack stack = e.getStack();

        // Остались три режима; режим из старого конфига заменяем на Rhomb.
        if (!this.mode.isSelected("Rhomb") && !this.mode.isSelected("\u0414\u0443\u0448\u0438") && !this.mode.isSelected("Circle")) {
            this.mode.setSelected("Rhomb");
        }
        if (this.mode.isSelected("Circle")) {
            this.renderCircle(stack, target, camPos, alpha);
        } else {
            stack.push();
            stack.translate((float)this.smoothedPos.x, (float)this.smoothedPos.y, (float)this.smoothedPos.z);
            if (this.mode.isSelected("Rhomb")) {
                this.renderRhomb(stack, camera, target, alpha);
            } else if (this.mode.isSelected("Ghost")) {
                this.renderGhost(stack, camera, target, alpha);
            } else if (this.mode.isSelected("Души")) {
                this.renderSouls(stack, camera, target, alpha);
            } else if (this.mode.isSelected("Chain")) {
                this.renderChain(stack, target, alpha);
            } else if (this.mode.isSelected("Crystals")) {
                if (this.crystalList.isEmpty() || this.lastRenderedTarget != target) {
                    this.createCrystals();
                    this.lastRenderedTarget = target;
                }
                this.renderCrystals(stack, camera, alpha, deltaTime);
            } else if (this.mode.isSelected("Осколки")) {
                this.renderShards(stack, camera, target, alpha);
            } else if (this.mode.isSelected("Rhomb 2")) {
                this.renderMarker(stack, camera, target, alpha);
            } else if (this.mode.isSelected("Skull")) {
                this.renderSkull(stack, camera, target, alpha, deltaTime);
            } else if (this.mode.isSelected("Jello")) {
                this.renderJello(stack, camera, target, alpha);
            } else if (this.mode.isSelected("Vortex") || this.mode.isSelected("Brackets")) {
                this.renderPlanes(stack, target, alpha, this.mode.isSelected("Vortex") ? VORTEX_TEXTURE : BRACKETS_TEXTURE);
            }
            stack.pop();
        }

    }

    // ── режимы из Polairis: Rhomb 2, Skull, Jello, Vortex, Brackets ──────────

    private static final Identifier MARKER_TEXTURE = Identifier.of("wiosvisual", "images/targetesp/marker.png");
    private static final Identifier SKULL_TEXTURE = Identifier.of("wiosvisual", "images/targetesp/skull.png");
    private static final Identifier VORTEX_TEXTURE = Identifier.of("wiosvisual", "images/targetesp/vortex.png");
    private static final Identifier BRACKETS_TEXTURE = Identifier.of("wiosvisual", "images/targetesp/brackets.png");
    private static final int PLANE_LAYERS = 3;
    private static final long PLANES_START_MS = System.currentTimeMillis();
    private final float[] planeRotSpeed = new float[PLANE_LAYERS];
    private final float[] planePitchSpeed = new float[PLANE_LAYERS];
    private final float[] planePitchPhase = new float[PLANE_LAYERS];
    private final float[] planeYawBase = new float[PLANE_LAYERS];
    private final Random planeRandom = new Random();
    private LivingEntity planesSeededFor;
    private float skullImpact;
    private float jelloMoving;

    /**
     * Центр цели, вынесенный к камере на половину ширины хитбокса: плоский маркер
     * лежит на поверхности игрока, а не утопает в модели, где его режет depth-test.
     */
    private void translateToFront(MatrixStack stack, Camera camera, LivingEntity target) {
        float halfHeight = target.getHeight() / 2.0f;
        Vec3d center = this.smoothedPos.add(0.0, halfHeight, 0.0);
        Vec3d toCamera = camera.pos.subtract(center);
        double len = toCamera.length();
        double push = Math.min(target.getWidth() * 0.75 + 0.05, len * 0.5);
        if (len < 1.0E-4) {
            stack.translate(0.0f, halfHeight, 0.0f);
            return;
        }
        Vec3d off = toCamera.multiply(push / len);
        stack.translate((float)off.x, (float)(halfHeight + off.y), (float)off.z);
        // Размер маркера постоянный: при приближении к цели он больше не уменьшается.
    }

    private LivingEntity sizeRefTarget;
    private float sizeRefDistance = 1.0f;

    /** Rhomb 2: вращающийся и пульсирующий маркер, развёрнутый к камере. */
    private void renderMarker(MatrixStack stack, Camera camera, LivingEntity target, float alpha) {
        long now = System.currentTimeMillis();
        stack.push();
        this.translateToFront(stack, camera, target);
        stack.multiply(camera.getRotation());
        float rotation = (float)(now % 3600L) / 3600.0f * 360.0f;
        float pulse = 1.0f + MathHelper.sin((float)(now % 1400L) / 1400.0f * ((float)Math.PI * 2.0f)) * 0.08f;
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        float s = 0.65f * pulse;
        stack.scale(s, s, 1.0f);
        Matrix4f m = stack.peek().getPositionMatrix();
        BufferBuilder buffer = TargetESP.beginTextured(MARKER_TEXTURE);
        TargetESP.vertexTex(buffer, m, -1.0f, -1.0f, 0.0f, 0.0f, 0.0f, TargetESP.withAlpha(this.color1.getColor(), (int)(255.0f * alpha)));
        TargetESP.vertexTex(buffer, m, -1.0f, 1.0f, 0.0f, 0.0f, 1.0f, TargetESP.withAlpha(this.color2.getColor(), (int)(255.0f * alpha)));
        TargetESP.vertexTex(buffer, m, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, TargetESP.withAlpha(this.color1.getColor(), (int)(215.0f * alpha)));
        TargetESP.vertexTex(buffer, m, 1.0f, -1.0f, 0.0f, 1.0f, 0.0f, TargetESP.withAlpha(this.color2.getColor(), (int)(215.0f * alpha)));
        TargetESP.end(buffer);
        stack.pop();
    }

    /** Skull: череп над центром цели, при попадании на миг сжимается. */
    private void renderSkull(MatrixStack stack, Camera camera, LivingEntity target, float alpha, float deltaTime) {
        float impactTarget = target.hurtTime > 0 ? 1.0f : 0.0f;
        float step = (impactTarget > this.skullImpact ? 0.30f : 0.10f) * deltaTime;
        this.skullImpact = this.skullImpact < impactTarget ? Math.min(this.skullImpact + step, impactTarget) : Math.max(this.skullImpact - step, impactTarget);
        float impact = TargetESP.easeOutCubic(this.skullImpact);
        float s = 0.48f * (1.0f - 0.14f * impact);
        float a = Math.min(1.0f, alpha * 1.85f);
        stack.push();
        this.translateToFront(stack, camera, target);
        stack.multiply(camera.getRotation());
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
        stack.scale(s, s, 1.0f);
        Matrix4f m = stack.peek().getPositionMatrix();
        BufferBuilder buffer = TargetESP.beginTextured(SKULL_TEXTURE);
        TargetESP.vertexTex(buffer, m, -0.8f, -0.8f, 0.0f, 0.0f, 0.0f, TargetESP.withAlpha(this.color1.getColor(), (int)(255.0f * a)));
        TargetESP.vertexTex(buffer, m, -0.8f, 0.8f, 0.0f, 0.0f, 1.0f, TargetESP.withAlpha(this.color2.getColor(), (int)(255.0f * a)));
        TargetESP.vertexTex(buffer, m, 0.8f, 0.8f, 0.0f, 1.0f, 1.0f, TargetESP.withAlpha(this.color1.getColor(), (int)(250.0f * a)));
        TargetESP.vertexTex(buffer, m, 0.8f, -0.8f, 0.0f, 1.0f, 0.0f, TargetESP.withAlpha(this.color2.getColor(), (int)(250.0f * a)));
        TargetESP.end(buffer);
        stack.pop();
    }

    /** Jello: толстое колышущееся кольцо из светящихся точек; при уроне краснеет. */
    private void renderJello(MatrixStack stack, Camera camera, LivingEntity target, float alpha) {
        this.jelloMoving += 4.0f;
        float width = target.getWidth() * 1.65f;
        float height = target.getHeight() - 0.15f;
        float anim = TargetESP.easeOutCubic(alpha);
        float ringScale = Math.max(0.5f, 0.7f - 0.2f * anim);
        int color = this.color1.getColor() | 0xFF000000;
        if (this.hurtProgress > 0.0f) {
            color = TargetESP.lerpColor(color, 0xFFFF3232, TargetESP.easeOutCubic(MathHelper.clamp(this.hurtProgress, 0.0f, 1.0f)));
        }
        Quaternionf rotation = camera.getRotation();
        BufferBuilder buffer = TargetESP.beginTextured(BLOOM_GLOW);
        for (int i = 0; i < 360; i += 2) {
            double rad = Math.toRadians(i + this.jelloMoving);
            float x = (float)(Math.cos(rad) * width * ringScale);
            float z = (float)(Math.sin(rad) * width * ringScale);
            for (int j = 0; j < 15; ++j) {
                float y = height / 1.7f + height / 2.0f * (float)Math.cos(Math.toRadians(this.jelloMoving / 1.5f + j * 2.0f));
                this.glowSprite(stack, buffer, rotation, x, y, z, 0.2f, TargetESP.withAlpha(color, (int)(255.0f * anim * ((float)j / 15.0f) * 0.05f)));
            }
        }
        for (int i = 0; i < 360; i += 2) {
            double rad = Math.toRadians(i + this.jelloMoving);
            float y = height / 1.75f + height / 2.0f * (float)Math.cos(Math.toRadians(this.jelloMoving / 1.5f + 30.0f));
            this.glowSprite(stack, buffer, rotation, (float)(Math.cos(rad) * width * ringScale), y, (float)(Math.sin(rad) * width * ringScale), 0.2f,
                    TargetESP.withAlpha(color, (int)(255.0f * anim * 0.2f)));
        }
        TargetESP.end(buffer);
    }

    private void glowSprite(MatrixStack stack, BufferBuilder buffer, Quaternionf rotation, float x, float y, float z, float size, int color) {
        if ((color >>> 24) == 0) {
            return;
        }
        stack.push();
        stack.translate(x, y, z);
        stack.multiply(rotation);
        Matrix4f m = stack.peek().getPositionMatrix();
        float h = size / 2.0f;
        TargetESP.vertexTex(buffer, m, -h, -h, 0.0f, 0.0f, 0.0f, color);
        TargetESP.vertexTex(buffer, m, h, -h, 0.0f, 1.0f, 0.0f, color);
        TargetESP.vertexTex(buffer, m, h, h, 0.0f, 1.0f, 1.0f, color);
        TargetESP.vertexTex(buffer, m, -h, h, 0.0f, 0.0f, 1.0f, color);
        stack.pop();
    }

    /** Vortex / Brackets: три текстурные плоскости вокруг цели, каждая крутится и качается по-своему. */
    private void renderPlanes(MatrixStack stack, LivingEntity target, float alpha, Identifier texture) {
        if (this.planesSeededFor != target) {
            this.reseedPlanes(target);
        }
        float time = (float)(System.currentTimeMillis() - PLANES_START_MS) / 1000.0f;
        float speed = MathHelper.clamp(this.planeSpeed.getValue(), 0.0f, 4.0f);
        float half = target.getWidth() * 2.5f * MathHelper.clamp(this.planeSize.getValue(), 0.3f, 3.0f);
        float baseY = target.getHeight() * 0.62f - 0.006f;
        float hurt = this.planeHurtRed.isValue() ? MathHelper.clamp(this.hurtProgress, 0.0f, 1.0f) : 0.0f;
        int baseColor = TargetESP.lerpColor(this.color1.getColor() | 0xFF000000, 0xFFFF3A3A, hurt);
        float twoPi = (float)Math.PI * 2.0f;
        BufferBuilder buffer = TargetESP.beginTextured(texture);
        for (int pass = 0; pass < 2; ++pass) {
            for (int k = 0; k < PLANE_LAYERS; ++k) {
                float pulse = (MathHelper.sin(time * speed * 3.4f + k * (twoPi / 3.0f)) + 1.0f) * 0.5f;
                float layerAlpha = 0.35f + 0.65f * pulse;
                float yaw = -time * 140.0f * speed * this.planeRotSpeed[k] + this.planeYawBase[k];
                float pitch = MathHelper.sin(time * this.planePitchSpeed[k] + this.planePitchPhase[k]) * 30.0f;
                int color = TargetESP.withAlpha(baseColor, (int)(255.0f * alpha * layerAlpha * (pass == 0 ? 1.0f : 0.85f)));
                stack.push();
                stack.translate(0.0f, baseY + 0.006f * k, 0.0f);
                stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0f + yaw));
                stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
                Matrix4f m = stack.peek().getPositionMatrix();
                TargetESP.vertexTex(buffer, m, -half, 0.0f, -half, 0.0f, 0.0f, color);
                TargetESP.vertexTex(buffer, m, -half, 0.0f, half, 0.0f, 1.0f, color);
                TargetESP.vertexTex(buffer, m, half, 0.0f, half, 1.0f, 1.0f, color);
                TargetESP.vertexTex(buffer, m, half, 0.0f, -half, 1.0f, 0.0f, color);
                stack.pop();
            }
        }
        TargetESP.end(buffer);
    }

    /** Случайные скорости и наклоны плоскостей; стартовые углы разводятся минимум на 22°. */
    private void reseedPlanes(LivingEntity target) {
        this.planesSeededFor = target;
        for (int i = 0; i < PLANE_LAYERS; ++i) {
            this.planeRotSpeed[i] = 0.75f + this.planeRandom.nextFloat() * 0.5f;
            this.planePitchSpeed[i] = 0.8f + this.planeRandom.nextFloat() * 0.8f;
            this.planePitchPhase[i] = this.planeRandom.nextFloat() * (float)Math.PI * 2.0f;
        }
        for (int attempt = 0; attempt < 64; ++attempt) {
            for (int j = 0; j < PLANE_LAYERS; ++j) {
                this.planeYawBase[j] = -30.0f + this.planeRandom.nextFloat() * 60.0f;
            }
            boolean ok = true;
            for (int a = 0; a < PLANE_LAYERS && ok; ++a) {
                for (int b = a + 1; b < PLANE_LAYERS; ++b) {
                    if (Math.abs(this.planeYawBase[a] - this.planeYawBase[b]) < 22.0f) {
                        ok = false;
                        break;
                    }
                }
            }
            if (ok) {
                return;
            }
        }
        this.planeYawBase[0] = -30.0f;
        this.planeYawBase[1] = 0.0f;
        this.planeYawBase[2] = 30.0f;
    }

    private static float easeOutCubic(float value) {
        float inv = 1.0f - MathHelper.clamp(value, 0.0f, 1.0f);
        return 1.0f - inv * inv * inv;
    }

    /** Текстура текущего буфера: null — буфер без UV (POSITION_COLOR). Форматы буфера и пайплайна обязаны совпадать. */
    private static Identifier pendingTexture;

    private static BufferBuilder beginTextured(Identifier texture) {
        pendingTexture = texture;
        return Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
    }

    private static BufferBuilder beginColored() {
        pendingTexture = null;
        return Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
    }

    private static void end(BufferBuilder buffer) {
        Identifier texture = pendingTexture;
        pendingTexture = null;
        if (texture != null) {
            // Светящиеся спрайты рисуются аддитивно (SRC_ALPHA, ONE), как GL-версия до 1.21.6.
            WiosRender.draw(WiosRender.posTexColor(texture, VertexFormat.DrawMode.QUADS, true, false, true), buffer);
        } else {
            WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.QUADS, true, false), buffer);
        }
    }

    private static void vertexTex(VertexConsumer b, Matrix4f m, float x, float y, float z, float u, float v, int color) {
        b.vertex(m, x, y, z).texture(u, v).color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >>> 24 & 0xFF);
    }

    private static void vertexColor(VertexConsumer b, Matrix4f m, float x, float y, float z, int color) {
        b.vertex(m, x, y, z).color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >>> 24 & 0xFF);
    }

    private void renderRhomb(MatrixStack stack, Camera camera, LivingEntity target, float alpha) {
        stack.push();
        this.translateToFront(stack, camera, target);
        stack.multiply(camera.getRotation());
        if (this.rhombAnim.isSelected("Крутится")) {
            float angle = (float)(System.currentTimeMillis() % 3600000L) / 1000.0f * 120.0f;
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle % 360.0f));
        }
        float s = this.scale.getValue() * 0.5f;
        stack.scale(s, s, 1.0f);
        int c1 = TargetESP.withAlpha(this.color1.getColor(), (int)(255.0f * alpha));
        int c2 = TargetESP.withAlpha(this.color2.getColor(), (int)(255.0f * alpha));
        Matrix4f m = stack.peek().getPositionMatrix();
        BufferBuilder buffer = TargetESP.beginTextured(CUBE_TEXTURE);
        TargetESP.vertexTex(buffer, m, -1.0f, -1.0f, 0.0f, 0.0f, 0.0f, c2);
        TargetESP.vertexTex(buffer, m, -1.0f, 1.0f, 0.0f, 0.0f, 1.0f, c1);
        TargetESP.vertexTex(buffer, m, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, c2);
        TargetESP.vertexTex(buffer, m, 1.0f, -1.0f, 0.0f, 1.0f, 0.0f, c1);
        TargetESP.end(buffer);
        stack.pop();
    }

    private void renderChain(MatrixStack stack, LivingEntity target, float alpha) {
        float animValue = (float)(System.currentTimeMillis() % 360000L) / 1000.0f * 60.0f;
        float gradusX = (float)(20.0 * Math.min(1.0 + Math.sin(Math.toRadians(animValue)), 1.0));
        float gradusZ = (float)(20.0 * (Math.min(1.0 + Math.sin(Math.toRadians(animValue)), 2.0) - 1.0));
        float width = target.getWidth() * 3.0f;
        int linksStep = 18;
        int totalAngle = 720;
        float chainSizeVal = 8.0f;
        float down = 1.5f;
        float chainScale = 0.5f;
        int alphaVal = MathHelper.clamp((int)((int)(alpha * 128.0f)), (int)0, (int)128);
        int baseColor1 = this.color1.getColor();
        int baseColor2 = this.color2.getColor();
        if (this.hurtProgress > 0.0f) {
            baseColor1 = TargetESP.lerpColor(baseColor1, 0xFFFF0000, this.hurtProgress);
            baseColor2 = TargetESP.lerpColor(baseColor2, 0xFFFF0000, this.hurtProgress);
        }
        int c1 = TargetESP.withAlpha(baseColor1, alphaVal);
        int c2 = TargetESP.withAlpha(baseColor2, alphaVal);
        float rotationValue = (float)(System.currentTimeMillis() % 720000L) / 1000.0f * 30.0f;
        BufferBuilder buffer = TargetESP.beginTextured(CHAIN_TEXTURE);
        for (int chain = 0; chain < 2; ++chain) {
            float val = 1.2f - 0.5f * (chain == 0 ? 1.0f : 0.9f);
            stack.push();
            stack.translate(0.0f, target.getHeight() / 2.0f, 0.0f);
            stack.scale(chainScale, chainScale, chainScale);
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(chain == 0 ? gradusX : -gradusX));
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(chain == 0 ? gradusZ : -gradusZ));
            float y = -0.5f;
            Matrix4f matrix = stack.peek().getPositionMatrix();
            int modif = linksStep / 2;
            for (int i = 0; i < totalAngle; i += modif) {
                float offsetX = (chain == 0 ? gradusX : -gradusX) / 100.0f;
                float offsetZ = (chain == 0 ? -gradusZ : gradusZ) / 100.0f;
                float prevSin = (float)((double)offsetX + Math.sin(Math.toRadians(i - modif + rotationValue)) * (double)width * (double)val);
                float prevCos = (float)((double)offsetZ + Math.cos(Math.toRadians(i - modif + rotationValue)) * (double)width * (double)val);
                float sin = (float)((double)offsetX + Math.sin(Math.toRadians(i + rotationValue)) * (double)width * (double)val);
                float cos = (float)((double)offsetZ + Math.cos(Math.toRadians(i + rotationValue)) * (double)width * (double)val);
                float u0 = 1.0f / 360.0f * (float)(i - modif) * chainSizeVal;
                float u1 = 1.0f / 360.0f * (float)i * chainSizeVal;
                TargetESP.vertexTex(buffer, matrix, prevSin, y, prevCos, u0, 0.0f, c1);
                TargetESP.vertexTex(buffer, matrix, sin, y, cos, u1, 0.0f, c1);
                TargetESP.vertexTex(buffer, matrix, sin, y + down, cos, u1, 0.99f, c2);
                TargetESP.vertexTex(buffer, matrix, prevSin, y + down, prevCos, u0, 0.99f, c2);
            }
            stack.pop();
        }
        TargetESP.end(buffer);
    }

    private void renderGhost(MatrixStack stack, Camera camera, LivingEntity target, float alpha) {
        stack.push();
        stack.translate(0.0f, target.getHeight() * 0.5f, 0.0f);
        BufferBuilder buffer = TargetESP.beginTextured(GHOST_GLOW);
        if (this.ghostStyle.isSelected("Souls")) {
            this.renderSoulGhosts(stack, camera, buffer, alpha);
        } else {
            this.renderTrailGhosts(stack, camera, buffer, target, alpha);
        }
        TargetESP.end(buffer);
        stack.pop();
    }

    /**
     * «Души»: рой светящихся точек по орбите вокруг цели. Дуга рисуется не сплошной,
     * а червячками — участок из точек, потом пропуск, потом снова, поэтому рой живой.
     */
    private void renderSouls(MatrixStack stack, Camera camera, LivingEntity target, float alpha) {
        long now = System.currentTimeMillis();
        this.soulsRotation += 120.0f * (float)(now - this.ghostLastUpdateTime) / 1000.0f;
        this.ghostLastUpdateTime = now;
        float width = target.getWidth() * 1.5f;
        float height = target.getHeight();
        float val = Math.max(0.5f, 1.2f - 0.5f * alpha);
        int color = this.color1.getColor();
        BufferBuilder buffer = TargetESP.beginTextured(GHOST_GLOW);
        int step = 2;
        int wormTick = 0;
        int wormCooldown = 0;
        for (int angle = 0; angle < 360; angle += step) {
            float size = 0.13f + 0.005f * (float)wormTick;
            float glowSize = 0.7f + 0.005f * (float)wormTick;
            if (wormCooldown > 0) {
                wormCooldown -= step;
                continue;
            }
            if ((wormTick += step) > 50) {
                wormCooldown = 100;
                wormTick = 0;
                continue;
            }
            double phase = Math.toRadians((double)((float)angle + this.soulsRotation));
            double sin = Math.sin(phase) * (double)width * (double)val;
            double cos = Math.cos(phase) * (double)width * (double)val;
            double y = (double)(height / 1.5f) + (double)(height / 3.0f) * Math.sin(Math.toRadians((double)((float)angle / 2.0f + this.soulsRotation / 5.0f)));
            Vec3d local = new Vec3d(sin, y, cos);
            int core = TargetESP.withAlpha(color, (int)(255.0f * alpha));
            int glow = TargetESP.withAlpha(color, (int)(255.0f * alpha * 0.05f));
            this.drawGhostBillboard(stack, camera, buffer, local, glowSize, 0.0f, glow, glow);
            this.drawGhostBillboard(stack, camera, buffer, local, size, 0.0f, core, core);
        }
        TargetESP.end(buffer);
    }

    private void renderTrailGhosts(MatrixStack stack, Camera camera, BufferBuilder buffer, LivingEntity target, float alpha) {
        if (this.smoothedPos == null) {
            return;
        }
        long elapsed = System.currentTimeMillis();
        Vec3d center = this.smoothedPos.add(0.0, target.getHeight() * 0.5f, 0.0);
        double radius = 0.33;
        double angle = 0.25f * (double)(elapsed % 67000L) / 45.0;
        double sin = Math.sin(angle) * radius;
        double cos = Math.cos(angle) * radius;
        Vec3d orb1 = center.add(sin, cos, -cos);
        Vec3d orb2 = center.add(-sin, sin, -cos);
        Vec3d orb3 = center.add(-sin, -sin, cos);
        if (this.ghostTrails.isEmpty() || this.ghostTrails.get(this.ghostTrails.size() - 1).position.distanceTo(orb1) > 0.02) {
            this.ghostTrails.add(new GhostTrail(orb1, 0));
            this.ghostTrails.add(new GhostTrail(orb2, 1));
            this.ghostTrails.add(new GhostTrail(orb3, 2));
        }
        while (this.ghostTrails.size() > 160) {
            this.ghostTrails.remove(0);
        }
        int trailSize = this.ghostTrails.size();
        if (trailSize <= 1) {
            return;
        }
        for (int i = 0; i < trailSize; ++i) {
            GhostTrail trail = this.ghostTrails.get(i);
            float progress = (float)i / (float)(trailSize - 1);
            if (progress <= 0.0f) continue;
            Vec3d local = trail.position.subtract(center);
            float size = 0.72f * progress;
            int baseColor = TargetESP.lerpColor(this.ghostColor(trail.colorIndex), this.getNextColor(trail.colorIndex), progress);
            int color = TargetESP.withAlpha(baseColor, (int)(progress * 235.0f * alpha));
            this.drawGhostBillboard(stack, camera, buffer, local, size, (float)elapsed * 0.08f + (float)i * 7.0f, color, color);
        }
    }

    private void renderSoulGhosts(MatrixStack stack, Camera camera, BufferBuilder buffer, float alpha) {
        long now = System.currentTimeMillis();
        this.soulsRotation += 1.65f * (float)(now - this.ghostLastUpdateTime) / 600.0f;
        this.ghostLastUpdateTime = now;
        int step = 3;
        int particles = 12;
        int layers = 9;
        for (int layer = 0; layer < layers; layer += step) {
            float layerProgress = (float)layer / (float)(layers - step);
            float layerY = -0.45f + layerProgress * 0.82f;
            for (int point = 0; point < particles; ++point) {
                float gradient = (float)point / (float)(particles - 1);
                int colorA = TargetESP.lerpColor(this.ghostColor(0), this.ghostColor(1), Math.min(gradient * 2.0f, 1.0f));
                int colorB = TargetESP.lerpColor(this.ghostColor(1), this.ghostColor(0), MathHelper.clamp((float)((gradient - 0.5f) * 2.0f), (float)0.0f, (float)1.0f));
                int finalColor = gradient < 0.66f ? colorA : colorB;
                float phase = this.soulsRotation + (float)point * 0.1f;
                float radius = 0.72f;
                int layerOffset = layer * layer;
                Vec3d local = new Vec3d(radius * MathHelper.sin((float)(phase + (float)layerOffset)), layerY + 0.16f * MathHelper.sin((float)(this.soulsRotation * 0.75f + (float)point * 0.17f)), radius * MathHelper.cos((float)(phase - (float)layerOffset)));
                float coreSize = alpha * (0.08f + (float)point / 170.0f) * 6.7f;
                float glowSize = alpha * (0.12f + (float)point / 155.0f) * 8.9f;
                int core = TargetESP.withAlpha(finalColor, (int)(235.0f * alpha));
                int glow = TargetESP.withAlpha(finalColor, (int)(70.0f * alpha));
                this.drawGhostBillboard(stack, camera, buffer, local, glowSize, (float)now * 0.018f + (float)point * 8.0f, glow, glow);
                this.drawGhostBillboard(stack, camera, buffer, local, coreSize, (float)now * 0.026f + (float)point * 12.0f, core, core);
            }
        }
    }

    private void drawGhostBillboard(MatrixStack stack, Camera camera, BufferBuilder buffer, Vec3d local, float size, float rotation, int c1, int c2) {
        stack.push();
        stack.translate((float)local.x, (float)local.y, (float)local.z);
        stack.multiply(camera.getRotation());
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        float half = size * 0.5f;
        Matrix4f matrix = stack.peek().getPositionMatrix();
        TargetESP.vertexTex(buffer, matrix, -half, -half, 0.0f, 0.0f, 1.0f, c2);
        TargetESP.vertexTex(buffer, matrix, half, -half, 0.0f, 1.0f, 1.0f, c1);
        TargetESP.vertexTex(buffer, matrix, half, half, 0.0f, 1.0f, 0.0f, c2);
        TargetESP.vertexTex(buffer, matrix, -half, half, 0.0f, 0.0f, 0.0f, c1);
        stack.pop();
    }

    private int ghostColor(int index) {
        int baseColor = switch (index % 3) {
            case 0 -> this.color1.getColor();
            case 1 -> this.color2.getColor();
            default -> this.color3.getColor();
        };
        return this.hurtProgress > 0.0f ? TargetESP.lerpColor(baseColor, 0xFFFF0000, this.hurtProgress) : baseColor;
    }

    private void createCrystals() {
        this.crystalList.clear();
        this.crystalList.add(new Crystal(new Vec3d(0.0, 0.85, 0.8), new Vec3d(-49.0, 0.0, 40.0)));
        this.crystalList.add(new Crystal(new Vec3d(0.2, 0.85, -0.675), new Vec3d(35.0, 0.0, -30.0)));
        this.crystalList.add(new Crystal(new Vec3d(0.6, 1.35, 0.6), new Vec3d(-30.0, 0.0, 35.0)));
        this.crystalList.add(new Crystal(new Vec3d(-0.74, 1.05, 0.4), new Vec3d(-25.0, 0.0, -30.0)));
        this.crystalList.add(new Crystal(new Vec3d(0.74, 0.95, -0.4), new Vec3d(0.0, 0.0, 0.0)));
        this.crystalList.add(new Crystal(new Vec3d(-0.475, 0.85, -0.375), new Vec3d(30.0, 0.0, -25.0)));
        this.crystalList.add(new Crystal(new Vec3d(0.0, 1.35, -0.6), new Vec3d(45.0, 0.0, 0.0)));
        this.crystalList.add(new Crystal(new Vec3d(0.85, 0.7, 0.1), new Vec3d(-30.0, 0.0, 30.0)));
        this.crystalList.add(new Crystal(new Vec3d(-0.7, 1.35, -0.3), new Vec3d(0.0, 0.0, 0.0)));
        this.crystalList.add(new Crystal(new Vec3d(-0.3, 1.35, 0.55), new Vec3d(0.0, 0.0, 0.0)));
        this.crystalList.add(new Crystal(new Vec3d(-0.5, 0.7, 0.7), new Vec3d(0.0, 0.0, 0.0)));
        this.crystalList.add(new Crystal(new Vec3d(0.5, 0.7, 0.7), new Vec3d(0.0, 0.0, 0.0)));
        this.crystalList.add(new Crystal(new Vec3d(-0.7, 0.75, 0.0), new Vec3d(0.0, 0.0, 0.0)));
        this.crystalList.add(new Crystal(new Vec3d(-0.2, 0.65, -0.7), new Vec3d(0.0, 0.0, 0.0)));
    }

    private void renderCrystals(MatrixStack stack, Camera camera, float alpha, float deltaTime) {
        if (this.crystalList.isEmpty()) {
            return;
        }
        this.rotationAngle += this.crystalRotationSpeed.getValue() * deltaTime;
        this.rotationAngle %= 360.0f;
        int baseColor = this.color1.getColor();
        if (this.hurtProgress > 0.0f) {
            baseColor = TargetESP.lerpColor(baseColor, 0xFFFF0000, this.hurtProgress);
        }
        float userAlpha = 0.3f;

        stack.push();
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotationAngle));

        BufferBuilder filled = TargetESP.beginColored();
        for (Crystal crystal : this.crystalList) {
            crystal.pushTransform(stack);
            TargetESP.drawFilledCrystal(stack, filled, baseColor, userAlpha * 0.85f, alpha);
            stack.pop();
        }
        TargetESP.end(filled);

        BufferBuilder glow = TargetESP.beginColored();
        for (Crystal crystal : this.crystalList) {
            crystal.pushTransform(stack);
            stack.push();
            stack.scale(1.15f, 1.15f, 1.15f);
            TargetESP.drawFilledCrystal(stack, glow, baseColor, userAlpha * 0.25f, alpha);
            stack.pop();
            stack.push();
            stack.scale(1.3f, 1.3f, 1.3f);
            TargetESP.drawFilledCrystal(stack, glow, baseColor, userAlpha * 0.1f, alpha);
            stack.pop();
            stack.pop();
        }
        TargetESP.end(glow);

        BufferBuilder bloom = TargetESP.beginTextured(BLOOM_GLOW);
        for (Crystal crystal : this.crystalList) {
            crystal.pushTransform(stack);
            TargetESP.drawBloomEffect(stack, camera, bloom, baseColor, alpha);
            stack.pop();
        }
        TargetESP.end(bloom);

        stack.pop();
    }

    private static void drawFilledCrystal(MatrixStack stack, BufferBuilder consumer, int baseColor, float alphaMultiplier, float anim) {
        float s = 0.05f;
        float hPrism = s;
        float hPyramid = s * 1.5f;
        int numSides = 8;
        ArrayList<Vector3f> topVertices = new ArrayList<Vector3f>();
        ArrayList<Vector3f> bottomVertices = new ArrayList<Vector3f>();
        for (int i = 0; i < numSides; ++i) {
            float angle = (float)(Math.PI * 2 * (double)i / (double)numSides);
            float x = (float)((double)s * Math.cos(angle));
            float z = (float)((double)s * Math.sin(angle));
            topVertices.add(new Vector3f(x, hPrism / 2.0f, z));
            bottomVertices.add(new Vector3f(x, -hPrism / 2.0f, z));
        }
        Vector3f vTop = new Vector3f(0.0f, hPrism / 2.0f + hPyramid, 0.0f);
        Vector3f vBottom = new Vector3f(0.0f, -hPrism / 2.0f - hPyramid, 0.0f);
        int finalAlpha = (int)(alphaMultiplier * 255.0f * anim);
        int finalColor = TargetESP.withAlpha(baseColor, finalAlpha);
        int darkerColor = TargetESP.withAlpha(TargetESP.darkenColor(baseColor, 0.7f), finalAlpha);
        int lighterColor = TargetESP.withAlpha(TargetESP.lightenColor(baseColor, 1.2f), finalAlpha);
        Matrix4f matrix = stack.peek().getPositionMatrix();
        for (int i = 0; i < numSides; ++i) {
            Vector3f v1 = bottomVertices.get(i);
            Vector3f v2 = bottomVertices.get((i + 1) % numSides);
            Vector3f v3 = topVertices.get((i + 1) % numSides);
            Vector3f v4 = topVertices.get(i);
            int sideColor = i % 2 == 0 ? finalColor : darkerColor;
            TargetESP.vertexColor(consumer, matrix, v1.x, v1.y, v1.z, sideColor);
            TargetESP.vertexColor(consumer, matrix, v2.x, v2.y, v2.z, sideColor);
            TargetESP.vertexColor(consumer, matrix, v3.x, v3.y, v3.z, sideColor);
            TargetESP.vertexColor(consumer, matrix, v4.x, v4.y, v4.z, sideColor);
        }
        for (int i = 0; i < numSides; ++i) {
            Vector3f v1 = topVertices.get(i);
            Vector3f v2 = topVertices.get((i + 1) % numSides);
            int pyramidColor = i % 2 == 0 ? lighterColor : finalColor;
            TargetESP.vertexColor(consumer, matrix, vTop.x, vTop.y, vTop.z, pyramidColor);
            TargetESP.vertexColor(consumer, matrix, v2.x, v2.y, v2.z, pyramidColor);
            TargetESP.vertexColor(consumer, matrix, v1.x, v1.y, v1.z, pyramidColor);
            TargetESP.vertexColor(consumer, matrix, v1.x, v1.y, v1.z, pyramidColor);
        }
        for (int i = 0; i < numSides; ++i) {
            Vector3f v1 = bottomVertices.get(i);
            Vector3f v2 = bottomVertices.get((i + 1) % numSides);
            int pyramidColor = i % 2 == 0 ? darkerColor : finalColor;
            TargetESP.vertexColor(consumer, matrix, vBottom.x, vBottom.y, vBottom.z, pyramidColor);
            TargetESP.vertexColor(consumer, matrix, v1.x, v1.y, v1.z, pyramidColor);
            TargetESP.vertexColor(consumer, matrix, v2.x, v2.y, v2.z, pyramidColor);
            TargetESP.vertexColor(consumer, matrix, v2.x, v2.y, v2.z, pyramidColor);
        }
    }

    private static void drawBloomEffect(MatrixStack stack, Camera camera, BufferBuilder bloomConsumer, int baseColor, float anim) {
        int bloomAlpha = (int)(0.3f * 60.0f * anim);
        int bloomColor = TargetESP.withAlpha(baseColor, bloomAlpha);
        float bloomSize = 0.75f;
        Quaternionf camRot = camera.getRotation();
        int segments = 6;
        for (int i = 0; i < segments; ++i) {
            stack.push();
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(360.0f / (float)segments * (float)i));
            stack.multiply(camRot);
            Matrix4f matrix = stack.peek().getPositionMatrix();
            TargetESP.vertexTex(bloomConsumer, matrix, -bloomSize / 2.0f, -bloomSize / 2.0f, 0.0f, 0.0f, 1.0f, bloomColor);
            TargetESP.vertexTex(bloomConsumer, matrix, bloomSize / 2.0f, -bloomSize / 2.0f, 0.0f, 1.0f, 1.0f, bloomColor);
            TargetESP.vertexTex(bloomConsumer, matrix, bloomSize / 2.0f, bloomSize / 2.0f, 0.0f, 1.0f, 0.0f, bloomColor);
            TargetESP.vertexTex(bloomConsumer, matrix, -bloomSize / 2.0f, bloomSize / 2.0f, 0.0f, 0.0f, 0.0f, bloomColor);
            stack.pop();
        }
        for (int i = 0; i < segments; ++i) {
            stack.push();
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(360.0f / (float)segments * (float)i));
            stack.multiply(camRot);
            Matrix4f matrix = stack.peek().getPositionMatrix();
            TargetESP.vertexTex(bloomConsumer, matrix, -bloomSize / 2.0f, -bloomSize / 2.0f, 0.0f, 0.0f, 1.0f, bloomColor);
            TargetESP.vertexTex(bloomConsumer, matrix, bloomSize / 2.0f, -bloomSize / 2.0f, 0.0f, 1.0f, 1.0f, bloomColor);
            TargetESP.vertexTex(bloomConsumer, matrix, bloomSize / 2.0f, bloomSize / 2.0f, 0.0f, 1.0f, 0.0f, bloomColor);
            TargetESP.vertexTex(bloomConsumer, matrix, -bloomSize / 2.0f, bloomSize / 2.0f, 0.0f, 0.0f, 0.0f, bloomColor);
            stack.pop();
        }
    }

    private void renderCircle(MatrixStack stack, LivingEntity target, Vec3d camPos, float alpha) {
        double cs = this.circleStep;
        Vec3d base = this.smoothedPos;
        float hitEffect = Math.min(this.hurtProgress * 2.0f, 1.0f);
        float distanceMultiplier = 1.0f + (float)Math.sin((double)hitEffect * Math.PI) * 0.18f;
        int size = 64;
        float entityWidth = target.getWidth() * distanceMultiplier;
        float entityHeight = target.getHeight();
        double targetY = TargetESP.smoothSinAnimation(cs) * (double)entityHeight;
        double targetY2 = TargetESP.smoothSinAnimation(cs - 0.35) * (double)entityHeight;
        this.smoothY += (targetY - this.smoothY) * 0.12;
        this.smoothY2 += (targetY2 - this.smoothY2) * 0.1;
        int baseColor1 = this.color1.getColor();
        int baseColor2 = this.color2.getColor();
        if (this.hurtProgress > 0.0f) {
            baseColor1 = TargetESP.lerpColor(baseColor1, 0xFFFF0000, this.hurtProgress);
            baseColor2 = TargetESP.lerpColor(baseColor2, 0xFFFF0000, this.hurtProgress);
        }
        Matrix4f matrix = stack.peek().getPositionMatrix();
        BufferBuilder buffer = TargetESP.beginColored();
        for (int i = 0; i < size; ++i) {
            float t = (float)i / (float)size;
            float tNext = (float)((i + 1) % size) / (float)size;
            float gradientT = (float)(0.5 - 0.5 * Math.cos((double)t * Math.PI * 2.0));
            float gradientTNext = (float)(0.5 - 0.5 * Math.cos((double)tNext * Math.PI * 2.0));
            int currentColor = TargetESP.lerpColor(baseColor1, baseColor2, gradientT);
            int nextColor = TargetESP.lerpColor(baseColor1, baseColor2, gradientTNext);
            int brightColor = TargetESP.withAlpha(currentColor, (int)(204.0f * alpha));
            int brightColorNext = TargetESP.withAlpha(nextColor, (int)(204.0f * alpha));
            int fadeColor = TargetESP.withAlpha(currentColor, 0);
            int fadeColorNext = TargetESP.withAlpha(nextColor, 0);
            double angle = Math.PI * 2 * (double)i / (double)size;
            double angleNext = Math.PI * 2 * (double)((i + 1) % size) / (double)size;
            float x = (float)(base.x + Math.cos(angle) * (double)entityWidth);
            float z = (float)(base.z + Math.sin(angle) * (double)entityWidth);
            float xNext = (float)(base.x + Math.cos(angleNext) * (double)entityWidth);
            float zNext = (float)(base.z + Math.sin(angleNext) * (double)entityWidth);
            float yTop = (float)(base.y + this.smoothY);
            float yBottom = (float)(base.y + this.smoothY2);
            TargetESP.vertexColor(buffer, matrix, x, yTop, z, brightColor);
            TargetESP.vertexColor(buffer, matrix, xNext, yTop, zNext, brightColorNext);
            TargetESP.vertexColor(buffer, matrix, xNext, yBottom, zNext, fadeColorNext);
            TargetESP.vertexColor(buffer, matrix, x, yBottom, z, fadeColor);
        }
        TargetESP.end(buffer);
    }

    /**
     * «Осколки»: облако кристаллов вокруг цели. Точки раскидываются по боковой
     * поверхности цилиндра выборкой best-candidate, чтобы не слипались в кучу.
     * Сид берётся от id сущности, поэтому у конкретного игрока рисунок всегда один и тот же.
     */
    private void generateShards(LivingEntity target, float width, float height) {
        this.shardPoints.clear();
        Random random = new Random((long)target.getId() * 133769420L);
        int count = (int)(width + height * 12.0f);
        for (int i = 0; i < count; ++i) {
            Vector3f best = null;
            float bestDistance = -1.0f;
            for (int attempt = 0; attempt < SHARD_ATTEMPTS; ++attempt) {
                float angle = (float)Math.toRadians((double)(random.nextFloat() * 360.0f));
                float px = (float)(Math.sin((double)angle) * (double)width);
                float pz = (float)(Math.cos((double)angle) * (double)width);
                float py = random.nextFloat() * height;
                if (this.shardPoints.isEmpty()) {
                    best = new Vector3f(px, py, pz);
                    break;
                }
                float minDistance = Float.MAX_VALUE;
                for (Vector3f point : this.shardPoints) {
                    float dx = point.x - px;
                    float dy = point.y - py;
                    float dz = point.z - pz;
                    float distance = dx * dx + dy * dy + dz * dz;
                    if (!(distance < minDistance)) continue;
                    minDistance = distance;
                }
                if (!(minDistance > bestDistance)) continue;
                bestDistance = minDistance;
                best = new Vector3f(px, py, pz);
            }
            if (best == null) continue;
            this.shardPoints.add(best);
        }
    }

    private Vector3f shardPosition(int index, float sin, float cos, float spread, float time) {
        Vector3f point = this.shardPoints.get(index);
        float x = (point.x * cos - point.z * sin) * spread;
        float z = (point.x * sin + point.z * cos) * spread;
        float y = point.y + (float)(0.05 * Math.sin((double)(time * 2.0f + (float)index * 1337.0f)));
        return new Vector3f(x, y, z);
    }

    private void renderShards(MatrixStack stack, Camera camera, LivingEntity target, float alpha) {
        float width = target.getWidth() * 1.5f;
        float height = target.getHeight();
        float finalAlpha = alpha * (this.shardAlpha.getValue() / 255.0f);
        if (finalAlpha <= 0.01f) {
            return;
        }
        if (this.shardTarget != target || this.shardPoints.isEmpty()) {
            this.generateShards(target, width, height);
            this.shardTarget = target;
        }
        if (this.shardPoints.isEmpty()) {
            return;
        }
        int baseColor = this.color1.getColor();
        if (this.hurtProgress > 0.0f) {
            baseColor = TargetESP.lerpColor(baseColor, 0xFFFF0000, this.hurtProgress);
        }
        float spin = (float)Math.toRadians((double)((float)(System.currentTimeMillis() % 360000L) / 7.25f));
        float sin = (float)Math.sin((double)spin);
        float cos = (float)Math.cos((double)spin);
        float time = (float)(System.currentTimeMillis() % 1000000L) * 0.001f;
        float spread = 1.25f - 0.5f * alpha;
        float size = 0.1f * this.shardSize.getValue();
        float centerY = height * 0.5f;
        int count = this.shardPoints.size();

        int solid = TargetESP.withAlpha(baseColor, (int)(255.0f * finalAlpha));
        BufferBuilder body = TargetESP.beginColored();
        for (int i = 0; i < count; ++i) {
            Vector3f pos = this.shardPosition(i, sin, cos, spread, time);
            stack.push();
            stack.translate(pos.x, pos.y, pos.z);
            float dx = -pos.x;
            float dy = centerY - pos.y;
            float dz = -pos.z;
            if (dx * dx + dy * dy + dz * dz > 1.0E-6f) {
                stack.multiply(new Quaternionf().rotateTo(0.0f, 1.0f, 0.0f, dx, dy, dz));
            }
            stack.scale(size, size, size);
            TargetESP.drawShardBody(stack, body, solid);
            stack.pop();
        }
        TargetESP.end(body);

        int halo = TargetESP.withAlpha(baseColor, (int)(255.0f * finalAlpha * 0.2f));
        int core = TargetESP.withAlpha(baseColor, (int)(255.0f * finalAlpha));
        float haloSize = 0.4675f * this.shardSize.getValue();
        float coreSize = 0.153f * this.shardSize.getValue();
        BufferBuilder glow = TargetESP.beginTextured(BLOOM_GLOW);
        for (int i = 0; i < count; ++i) {
            Vector3f pos = this.shardPosition(i, sin, cos, spread, time);
            Vec3d local = new Vec3d((double)pos.x, (double)pos.y, (double)pos.z);
            this.drawGhostBillboard(stack, camera, glow, local, haloSize, 0.0f, halo, halo);
            this.drawGhostBillboard(stack, camera, glow, local, coreSize, 0.0f, core, core);
        }
        TargetESP.end(glow);
    }

    /**
     * Октаэдр из восьми треугольников. Буфер работает в режиме QUADS,
     * поэтому третья вершина дублируется — так же, как в drawFilledCrystal.
     */
    private static void drawShardBody(MatrixStack stack, BufferBuilder consumer, int color) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        for (int face = 0; face < SHARD_FACES.length; ++face) {
            int faceColor = TargetESP.darkenColor(color, SHARD_SHADES[face]);
            Vector3f v1 = SHARD_VERTICES[SHARD_FACES[face][0]];
            Vector3f v2 = SHARD_VERTICES[SHARD_FACES[face][1]];
            Vector3f v3 = SHARD_VERTICES[SHARD_FACES[face][2]];
            TargetESP.vertexColor(consumer, matrix, v1.x, v1.y, v1.z, faceColor);
            TargetESP.vertexColor(consumer, matrix, v2.x, v2.y, v2.z, faceColor);
            TargetESP.vertexColor(consumer, matrix, v3.x, v3.y, v3.z, faceColor);
            TargetESP.vertexColor(consumer, matrix, v3.x, v3.y, v3.z, faceColor);
        }
    }

    private static double smoothSinAnimation(double input) {
        double sin = (Math.sin(input) + 1.0) / 2.0;
        return -(Math.cos(Math.PI * sin) - 1.0) / 2.0;
    }

    private static int darkenColor(int color, float factor) {
        int a = color >> 24 & 0xFF;
        int r = (int)((float)(color >> 16 & 0xFF) * factor);
        int g = (int)((float)(color >> 8 & 0xFF) * factor);
        int b = (int)((float)(color & 0xFF) * factor);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int lightenColor(int color, float factor) {
        int a = color >> 24 & 0xFF;
        int r = Math.min(255, (int)((float)(color >> 16 & 0xFF) * factor));
        int g = Math.min(255, (int)((float)(color >> 8 & 0xFF) * factor));
        int b = Math.min(255, (int)((float)(color & 0xFF) * factor));
        return a << 24 | r << 16 | g << 8 | b;
    }

    private int getNextColor(int colorIndex) {
        return switch (colorIndex) {
            case 0 -> this.color2.getColor();
            case 1 -> this.color3.getColor();
            default -> this.color1.getColor();
        };
    }

    private static int lerpColor(int c1, int c2, float t) {
        int a1 = c1 >> 24 & 0xFF;
        int r1 = c1 >> 16 & 0xFF;
        int g1 = c1 >> 8 & 0xFF;
        int b1 = c1 & 0xFF;
        int a2 = c2 >> 24 & 0xFF;
        int r2 = c2 >> 16 & 0xFF;
        int g2 = c2 >> 8 & 0xFF;
        int b2 = c2 & 0xFF;
        int a = (int)((float)a1 + (float)(a2 - a1) * t);
        int r = (int)((float)r1 + (float)(r2 - r1) * t);
        int g = (int)((float)g1 + (float)(g2 - g1) * t);
        int b = (int)((float)b1 + (float)(b2 - b1) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int withAlpha(int color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return color & 0xFFFFFF | alpha << 24;
    }

    private static class Crystal {
        private final Vec3d position;
        private final Vec3d rotation;
        private final float rotationSpeed;

        Crystal(Vec3d position, Vec3d rotation) {
            this.position = position;
            this.rotation = rotation;
            this.rotationSpeed = 0.5f + (float)(Math.random() * 1.5);
        }

        void pushTransform(MatrixStack stack) {
            stack.push();
            stack.translate((float)this.position.x, (float)this.position.y, (float)this.position.z);
            float timeSeconds = (float)(System.currentTimeMillis() % 31416L) / 1000.0f;
            float pulsation = 1.0f + (float)(Math.sin(timeSeconds * 2.0f) * 0.1);
            stack.scale(pulsation, pulsation, pulsation);
            float selfRotation = (float)(System.currentTimeMillis() % 36000L) / 100.0f * this.rotationSpeed;
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)this.rotation.x));
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)this.rotation.y + selfRotation));
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)this.rotation.z));
        }
    }

    private static class GhostTrail {
        final Vec3d position;
        final int colorIndex;

        GhostTrail(Vec3d position, int colorIndex) {
            this.position = position;
            this.colorIndex = colorIndex;
        }
    }
}
