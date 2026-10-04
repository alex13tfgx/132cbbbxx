package code.wiosvisual.features.impl.render.cosmetics;

import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.utils.display.render.pipeline.WiosRender;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Питомец-спутник: отдельное существо в мире (не часть модели игрока), поэтому он виден и от первого лица.
 * Бегает за хозяином справа-сзади, поворачивается по ходу движения, при беге проигрывает анимацию
 * walk/run (а если её нет, быстрее обычную) и стоит спокойно, когда догнал.
 */
public final class CosmeticPets {
    private static Vec3d position;
    private static float yaw;
    private static float phase;
    private static long lastNanos;
    private static CosmeticCatalog.Entry current;
    private static double velX;
    private static double velZ;
    private static final Vector3f LIGHT = new Vector3f(0.35f, 0.85f, 0.4f).normalize();
    private static final Vector3f NORMAL = new Vector3f();
    private static final Quaternionf CAMERA = new Quaternionf();

    private CosmeticPets() {
    }

    public static void render(CosmeticCatalog.Entry entry, WorldRenderEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (entry == null || mc.player == null || mc.world == null) {
            position = null;
            current = null;
            return;
        }
        GeoModel model = entry.model();
        if (model == null) {
            return;
        }
        long now = System.nanoTime();
        float dt = lastNanos == 0L ? 0.016f : Math.min(0.1f, (now - lastNanos) / 1.0E9f);
        lastNanos = now;

        Vec3d player = mc.player.getLerpedPos(event.getPartialTicks());
        float playerYaw = mc.player.getYaw();
        double rad = Math.toRadians(playerYaw);
        double forwardX = -Math.sin(rad);
        double forwardZ = Math.cos(rad);
        double rightX = -Math.cos(rad);
        double rightZ = -Math.sin(rad);
        double targetX = player.x + rightX * 1.3 + forwardX * 0.9;
        double targetZ = player.z + rightZ * 1.3 + forwardZ * 0.9;
        if (position == null || current != entry || position.distanceTo(player) > 16.0) {
            position = new Vec3d(targetX, player.y, targetZ);
            yaw = playerYaw;
            current = entry;
            velX = 0.0;
            velZ = 0.0;
        }

        double dx = targetX - position.x;
        double dz = targetZ - position.z;
        double dist = Math.hypot(dx, dz);
        // Скорость плавно разгоняется и тормозит, а не включается рывком.
        double wantX = 0.0;
        double wantZ = 0.0;
        if (dist > 0.25) {
            double want = Math.min(dist * 3.2, 8.0);
            wantX = dx / dist * want;
            wantZ = dz / dist * want;
        }
        double blend = Math.min(1.0, dt * 7.0);
        velX += (wantX - velX) * blend;
        velZ += (wantZ - velZ) * blend;
        float speed = (float)Math.hypot(velX, velZ);
        double x = position.x + velX * dt;
        double z = position.z + velZ * dt;
        if (speed > 0.35f) {
            float targetYaw = (float)Math.toDegrees(Math.atan2(-velX, velZ));
            yaw = approachAngle(yaw, targetYaw, Math.min(1.0f, dt * 9.0f));
        } else {
            yaw = approachAngle(yaw, playerYaw, Math.min(1.0f, dt * 2.5f));
        }
        double y = position.y + (player.y - position.y) * Math.min(1.0, dt * 8.0);
        position = new Vec3d(x, y, z);

        boolean moving = speed > 0.6f;
        CosmeticAnimation move = entry.moveAnimation();
        CosmeticAnimation animation = moving && move != null ? move : entry.animation();
        phase += dt * (moving ? (move == null ? 1.0f + Math.min(speed, 6.0f) * 0.35f : 1.0f) : 1.0f);
        if (animation != null) {
            animation.applyAt(phase);
        }

        if (position.distanceTo(player) > 48.0) {
            return;
        }
        MatrixStack stack = event.getStack();
        stack.push();
        stack.translate((float)x, (float)y, (float)z);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - yaw));
        float scale = entry.scale();
        stack.scale(scale, scale, scale);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        CAMERA.set(mc.getEntityRenderDispatcher().camera.getRotation());
        for (GeoModel.Bone bone : model.topLevelBones) {
            drawBone(bone, stack, buffer);
        }
        WiosRender.draw(WiosRender.posTexColorSolid(entry.texture, VertexFormat.DrawMode.QUADS), buffer);
        stack.pop();
    }

    private static float approachAngle(float from, float to, float t) {
        float diff = MathHelper.wrapDegrees(to - from);
        return from + diff * t;
    }

    private static void drawBone(GeoModel.Bone bone, MatrixStack matrices, BufferBuilder buffer) {
        matrices.push();
        matrices.translate(-bone.positionX / 16.0f, bone.positionY / 16.0f, bone.positionZ / 16.0f);
        matrices.translate(bone.pivotX / 16.0f, bone.pivotY / 16.0f, bone.pivotZ / 16.0f);
        rotate(matrices, bone.rotationX, bone.rotationY, bone.rotationZ);
        matrices.scale(bone.scaleX, bone.scaleY, bone.scaleZ);
        matrices.translate(-bone.pivotX / 16.0f, -bone.pivotY / 16.0f, -bone.pivotZ / 16.0f);
        for (GeoModel.Cube cube : bone.childCubes) {
            drawCube(cube, matrices, buffer);
        }
        for (GeoModel.Bone child : bone.childBones) {
            drawBone(child, matrices, buffer);
        }
        matrices.pop();
    }

    private static void drawCube(GeoModel.Cube cube, MatrixStack matrices, BufferBuilder buffer) {
        matrices.push();
        matrices.translate(cube.pivotX / 16.0f, cube.pivotY / 16.0f, cube.pivotZ / 16.0f);
        rotate(matrices, cube.rotationX, cube.rotationY, cube.rotationZ);
        matrices.translate(-cube.pivotX / 16.0f, -cube.pivotY / 16.0f, -cube.pivotZ / 16.0f);
        Matrix4f position = matrices.peek().getPositionMatrix();
        Matrix3f normalMatrix = matrices.peek().getNormalMatrix();
        for (GeoModel.Quad quad : cube.quads) {
            if (quad == null) {
                continue;
            }
            // Мировая нормаль грани → направленное освещение: объём читается, модель не плоская.
            NORMAL.set(quad.normalX, quad.normalY, quad.normalZ);
            normalMatrix.transform(NORMAL);
            CAMERA.transform(NORMAL);
            int shade = (int)(255.0f * (0.58f + 0.42f * Math.max(0.0f, Math.abs(NORMAL.dot(LIGHT)) * 0.35f + NORMAL.dot(LIGHT) * 0.65f)));
            shade = Math.max(110, Math.min(255, shade));
            for (GeoModel.Vertex vertex : quad.vertices) {
                buffer.vertex(position, vertex.x, vertex.y, vertex.z).texture(vertex.u, vertex.v).color(shade, shade, shade, 255);
            }
        }
        matrices.pop();
    }

    private static final Quaternionf SCRATCH = new Quaternionf();

    private static void rotate(MatrixStack matrices, float x, float y, float z) {
        if (z != 0.0f) {
            matrices.multiply(SCRATCH.rotationZ(z));
        }
        if (y != 0.0f) {
            matrices.multiply(SCRATCH.rotationY(y));
        }
        if (x != 0.0f) {
            matrices.multiply(SCRATCH.rotationX(x));
        }
    }
}
