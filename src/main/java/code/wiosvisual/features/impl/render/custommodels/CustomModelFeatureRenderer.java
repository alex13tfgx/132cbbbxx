package code.wiosvisual.features.impl.render.custommodels;

import code.wiosvisual.features.impl.render.CustomModels;
import code.wiosvisual.features.impl.render.cosmetics.ICosmeticRenderState;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Рисует модель Blockbench вместо игрока: ванильное тело скрывает PlayerCosmeticsMixin,
 * а этот слой строит модель из {@link BbModel}.
 *
 * Как в Figura: группы Head/Body/RightArm/LeftArm/RightLeg/LeftLeg повторяют позу
 * ванильной части тела — поворот вокруг её точки крепления и её сдвиги (присед).
 * Поверх этого проигрываются анимации модели ({@link ModelAnimator}).
 */
public class CustomModelFeatureRenderer
extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    /** Высота точки отсчёта модели игрока: 1.501 блока = 24.016 пикселя. */
    private static final float MODEL_OFFSET_PX = 24.016f;

    /** Части ванильного тела в порядке полей позы. */
    private enum Part {
        HEAD("Head"), BODY("Body"), RIGHT_ARM("RightArm"), LEFT_ARM("LeftArm"), RIGHT_LEG("RightLeg"), LEFT_LEG("LeftLeg");

        final String key;

        Part(String key) {
            this.key = key;
        }
    }

    /** Снимок ванильной части: поворот и смещение точки крепления относительно позы по умолчанию (в пикселях Blockbench). */
    private static final class PartPose {
        final Quaternionf rotation;
        final float[] pivot;
        final float[] defaultPivot;

        PartPose(ModelPart part) {
            this.rotation = new Quaternionf().rotationZYX(part.roll, -part.yaw, -part.pitch);
            this.pivot = toBb(part.originX, part.originY, part.originZ);
            ModelTransform def = part.getDefaultTransform();
            this.defaultPivot = toBb(def.x(), def.y(), def.z());
        }

        /** Пространство модели игрока (Y вниз, X зеркален) → пространство Blockbench. */
        private static float[] toBb(float x, float y, float z) {
            return new float[]{-x, MODEL_OFFSET_PX - y, z};
        }
    }

    public CustomModelFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue vertexConsumers, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        if (!(state instanceof ICosmeticRenderState access) || !access.wiosvisual$isCustomModel()) {
            return;
        }
        BbModel model = CustomModels.currentModel();
        if (model == null) {
            return;
        }
        assignParts(model);
        // Позы снимаются сейчас: команда рендера выполнится позже, когда модель уже будет в позе другого игрока.
        PlayerEntityModel player = this.getContextModel();
        PartPose[] poses = {
                new PartPose(player.head), new PartPose(player.body), new PartPose(player.rightArm),
                new PartPose(player.leftArm), new PartPose(player.rightLeg), new PartPose(player.leftLeg)};
        boolean moving = state.limbSwingAmplitude > 0.15f;
        Map<String, ModelAnimator.Pose> anim = ModelAnimator.update(model, state.id, moving, state.isInSneakingPose, state.handSwingProgress > 0f);
        Frame frame = new Frame(model, poses, anim, state.age, moving);

        for (int texture = 0; texture < model.textures.size(); texture++) {
            Identifier id = model.textures.get(texture);
            if (id == null) {
                continue;
            }
            int textureIndex = texture;
            RenderLayer layer = RenderLayers.entityCutoutNoCull(id);
            vertexConsumers.submitCustom(matrices, layer, (entry, buffer) -> {
                MatrixStack local = new MatrixStack();
                local.peek().copy(entry);
                local.push();
                // Из пространства модели игрока обратно в пространство сущности (Y вверх, ноги в 0).
                local.translate(0.0f, MODEL_OFFSET_PX / 16.0f, 0.0f);
                local.scale(-1.0f, -1.0f, 1.0f);
                for (BbModel.Group group : model.roots) {
                    renderGroup(group, local, buffer, light, textureIndex, frame);
                }
                local.pop();
            });
        }
    }

    /** Всё, что нужно для одного кадра модели. */
    private record Frame(BbModel model, PartPose[] poses, Map<String, ModelAnimator.Pose> anim, float age, boolean moving) {
    }

    private static void renderGroup(BbModel.Group group, MatrixStack matrices, VertexConsumer buffer, int light, int texture, Frame frame) {
        if (!group.visible) {
            return;
        }
        matrices.push();
        if (group.part >= 0) {
            // Как Figura: часть повторяет ванильную — поворот вокруг её точки крепления + её сдвиг.
            PartPose pose = frame.poses[group.part];
            matrices.translate(pose.pivot[0] / 16.0f, pose.pivot[1] / 16.0f, pose.pivot[2] / 16.0f);
            matrices.multiply(pose.rotation);
            matrices.translate(-pose.defaultPivot[0] / 16.0f, -pose.defaultPivot[1] / 16.0f, -pose.defaultPivot[2] / 16.0f);
        }

        float[] rot = {group.rotation[0], group.rotation[1], group.rotation[2]};
        float[] pos = {0f, 0f, 0f};
        float[] scale = {1f, 1f, 1f};
        ModelAnimator.Pose anim = frame.anim.get(group.uuid);
        if (anim != null) {
            for (int i = 0; i < 3; i++) {
                rot[i] += anim.rot[i];
                pos[i] += anim.pos[i];
                scale[i] *= anim.scale[i];
            }
        }
        float[] scripted = ModelAnimator.scripted(frame.model, group.name, frame.age, frame.moving);
        if (scripted != null) {
            for (int i = 0; i < 3; i++) {
                rot[i] += scripted[i];
                pos[i] += scripted[3 + i];
            }
        }

        float ox = group.origin[0] / 16.0f;
        float oy = group.origin[1] / 16.0f;
        float oz = group.origin[2] / 16.0f;
        matrices.translate(ox + pos[0] / 16.0f, oy + pos[1] / 16.0f, oz + pos[2] / 16.0f);
        rotate(matrices, rot);
        if (scale[0] != 1f || scale[1] != 1f || scale[2] != 1f) {
            matrices.scale(scale[0], scale[1], scale[2]);
        }
        matrices.translate(-ox, -oy, -oz);
        for (BbModel.Cube cube : group.cubes) {
            renderCube(cube, matrices, buffer, light, texture);
        }
        for (BbModel.Mesh mesh : group.meshes) {
            renderMesh(mesh, matrices, buffer, light, texture);
        }
        for (BbModel.Group child : group.groups) {
            renderGroup(child, matrices, buffer, light, texture, frame);
        }
        matrices.pop();
    }

    // ------------------------------------------------------------------ привязка групп к частям тела

    /**
     * Определяет, какие группы следуют за телом игрока. Сначала правило Figura: имя начинается
     * с Head/Body/RightArm/… Если таких групп нет совсем — «умный» поиск по именам
     * (ArmR, Right_Leg_top, leftleg, torso…). Каждая часть привязывается только к самой
     * внешней группе ветки, чтобы поворот не применялся дважды.
     */
    private static void assignParts(BbModel model) {
        if (model.partsAssigned) {
            return;
        }
        model.partsAssigned = true;
        boolean strict = anyStrict(model.roots);
        assign(model.roots, strict, 0);
    }

    private static boolean anyStrict(List<BbModel.Group> groups) {
        for (BbModel.Group g : groups) {
            if (g.visible && strictPart(g.name) != null || anyStrict(g.groups)) {
                return true;
            }
        }
        return false;
    }

    /** used — битовая маска частей, уже привязанных выше по ветке. */
    private static void assign(List<BbModel.Group> groups, boolean strict, int used) {
        for (BbModel.Group g : groups) {
            Part part = strict ? strictPart(g.name) : fuzzyPart(g);
            int mask = used;
            if (part != null && (used & (1 << part.ordinal())) == 0) {
                g.part = part.ordinal();
                mask |= 1 << part.ordinal();
            }
            assign(g.groups, strict, mask);
        }
    }

    /** Правило Figura: имя начинается с ключевого слова (с учётом регистра), но это не «...Pivot». */
    private static Part strictPart(String name) {
        if (name.toLowerCase(Locale.ROOT).contains("pivot")) {
            return null;
        }
        for (Part part : Part.values()) {
            if (name.startsWith(part.key)) {
                return part;
            }
        }
        return null;
    }

    private static Part fuzzyPart(BbModel.Group group) {
        String s = group.name.toLowerCase(Locale.ROOT).replace(" ", "_");
        if (s.contains("pivot")) {
            return null;
        }
        String compact = s.replace("_", "");
        if (compact.startsWith("head")) {
            return Part.HEAD;
        }
        if ((compact.equals("body") || compact.equals("torso")) && !containsLeg(group)) {
            return Part.BODY;
        }
        boolean arm = compact.contains("arm") && !compact.equals("arms");
        boolean leg = compact.contains("leg") && !compact.equals("legs");
        if (!arm && !leg) {
            return null;
        }
        boolean right = s.contains("right") || s.matches(".*(arm|leg)_?r\\d*(_.*)?$") || s.matches("^r_?(arm|leg).*") || s.matches(".*_r\\d*$");
        boolean left = s.contains("left") || s.matches(".*(arm|leg)_?l\\d*(_.*)?$") || s.matches("^l_?(arm|leg).*") || s.matches(".*_l\\d*$");
        if (right == left) {
            return null;
        }
        if (arm) {
            return right ? Part.RIGHT_ARM : Part.LEFT_ARM;
        }
        return right ? Part.RIGHT_LEG : Part.LEFT_LEG;
    }

    private static boolean containsLeg(BbModel.Group group) {
        for (BbModel.Group child : group.groups) {
            String c = child.name.toLowerCase(Locale.ROOT);
            if (c.contains("leg") || containsLeg(child)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ геометрия

    /** Порядок Blockbench: сначала X, потом Y, потом Z (углы в градусах). */
    private static void rotate(MatrixStack matrices, float[] degrees) {
        if (degrees[0] == 0.0f && degrees[1] == 0.0f && degrees[2] == 0.0f) {
            return;
        }
        matrices.multiply(new Quaternionf().rotationZYX((float) Math.toRadians(degrees[2]), (float) Math.toRadians(degrees[1]), (float) Math.toRadians(degrees[0])));
    }

    private static void renderCube(BbModel.Cube cube, MatrixStack matrices, VertexConsumer buffer, int light, int texture) {
        boolean any = false;
        for (int i = 0; i < 6; i++) {
            if (cube.texture[i] == texture && cube.uv[i] != null) {
                any = true;
                break;
            }
        }
        if (!any) {
            return;
        }
        matrices.push();
        float px = cube.origin[0] / 16.0f;
        float py = cube.origin[1] / 16.0f;
        float pz = cube.origin[2] / 16.0f;
        matrices.translate(px, py, pz);
        rotate(matrices, cube.rotation);
        matrices.translate(-px, -py, -pz);

        float x0 = (cube.from[0] - cube.inflate) / 16.0f;
        float y0 = (cube.from[1] - cube.inflate) / 16.0f;
        float z0 = (cube.from[2] - cube.inflate) / 16.0f;
        float x1 = (cube.to[0] + cube.inflate) / 16.0f;
        float y1 = (cube.to[1] + cube.inflate) / 16.0f;
        float z1 = (cube.to[2] + cube.inflate) / 16.0f;

        Matrix4f position = matrices.peek().getPositionMatrix();
        Matrix3f normalMatrix = matrices.peek().getNormalMatrix();
        // north, east, south, west, up, down: левый-верх, левый-низ, правый-низ, правый-верх глядя снаружи.
        float[][][] corners = {
                {{x1, y1, z0}, {x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}},
                {{x1, y1, z1}, {x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}},
                {{x0, y1, z1}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}},
                {{x0, y1, z0}, {x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}},
                {{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}},
                {{x0, y0, z1}, {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}},
        };
        float[][] normals = {{0, 0, -1}, {1, 0, 0}, {0, 0, 1}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}};
        Vector3f normal = new Vector3f();
        for (int face = 0; face < 6; face++) {
            if (cube.texture[face] != texture || cube.uv[face] == null) {
                continue;
            }
            float[] uv = cube.uv[face];
            // Углы UV: левый-верх, левый-низ, правый-низ, правый-верх.
            float[][] uvs = {{uv[0], uv[1]}, {uv[0], uv[3]}, {uv[2], uv[3]}, {uv[2], uv[1]}};
            normal.set(normals[face][0], normals[face][1], normals[face][2]);
            normalMatrix.transform(normal);
            for (int v = 0; v < 4; v++) {
                float[] c = corners[face][v];
                vertex(buffer, position, c[0], c[1], c[2], uvs[v][0], uvs[v][1], light, normal);
            }
        }
        matrices.pop();
    }

    /** Mesh-элемент: вершины относительно origin, поворот вокруг origin; треугольник — квад с повтором вершины. */
    private static void renderMesh(BbModel.Mesh mesh, MatrixStack matrices, VertexConsumer buffer, int light, int texture) {
        matrices.push();
        matrices.translate(mesh.origin[0] / 16.0f, mesh.origin[1] / 16.0f, mesh.origin[2] / 16.0f);
        rotate(matrices, mesh.rotation);
        Matrix4f position = matrices.peek().getPositionMatrix();
        Matrix3f normalMatrix = matrices.peek().getNormalMatrix();
        Vector3f normal = new Vector3f();
        for (BbModel.MeshFace face : mesh.faces) {
            if (face.texture != texture) {
                continue;
            }
            float[] a = face.vertices.get(0), b = face.vertices.get(1), c = face.vertices.get(2);
            normal.set(
                    (b[1] - a[1]) * (c[2] - a[2]) - (b[2] - a[2]) * (c[1] - a[1]),
                    (b[2] - a[2]) * (c[0] - a[0]) - (b[0] - a[0]) * (c[2] - a[2]),
                    (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]));
            if (normal.lengthSquared() > 0f) {
                normal.normalize();
            }
            normalMatrix.transform(normal);
            for (int i = 0; i < 4; i++) {
                float[] v = face.vertices.get(Math.min(i, face.vertices.size() - 1));
                vertex(buffer, position, v[0] / 16.0f, v[1] / 16.0f, v[2] / 16.0f, v[3], v[4], light, normal);
            }
        }
        matrices.pop();
    }

    private static void vertex(VertexConsumer buffer, Matrix4f position, float x, float y, float z, float u, float v, int light, Vector3f normal) {
        buffer.vertex(position, x, y, z)
                .color(-1)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normal.x, normal.y, normal.z);
    }
}
