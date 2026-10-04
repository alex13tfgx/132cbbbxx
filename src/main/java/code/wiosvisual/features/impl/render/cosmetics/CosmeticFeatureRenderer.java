package code.wiosvisual.features.impl.render.cosmetics;

import code.wiosvisual.features.impl.render.Cosmetics;
import net.minecraft.client.model.ModelPart;
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
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Слой рендера игрока, рисующий объёмную косметику (всё, кроме накидок — они подменяют
 * текстуру ванильного плаща). Матрицы на входе уже в пространстве модели игрока, а позы
 * частей тела выставлены на текущий кадр, поэтому предметы следуют за головой и телом.
 */
public class CosmeticFeatureRenderer
extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private static final CosmeticCatalog.Category[] MODEL_CATEGORIES = new CosmeticCatalog.Category[]{
            CosmeticCatalog.Category.WINGS, CosmeticCatalog.Category.BACK, CosmeticCatalog.Category.HAT,
            CosmeticCatalog.Category.EARS, CosmeticCatalog.Category.TAILS, CosmeticCatalog.Category.HEADS, CosmeticCatalog.Category.MASKS,
            CosmeticCatalog.Category.PAWS, CosmeticCatalog.Category.STOCKINGS};

    public CosmeticFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue vertexConsumers, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        // Косметика чисто клиентская (без сети) — рисуем только на своей модели, иначе она
        // отображалась бы на всех видимых игроках. Флаг ставится в PlayerCosmeticsMixin.
        if (!(state instanceof ICosmeticRenderState access) || !access.wiosvisual$isLocalPlayer()) {
            return;
        }
        Cosmetics cosmetics = Cosmetics.getInstance();
        if (cosmetics == null || !cosmetics.isState()) {
            return;
        }
        for (CosmeticCatalog.Category category : MODEL_CATEGORIES) {
            CosmeticCatalog.Entry entry = cosmetics.selected(category);
            if (entry == null) {
                continue;
            }
            GeoModel model = entry.model();
            if (model == null) {
                continue;
            }
            CosmeticAnimation animation = entry.animation();
            if (animation != null) {
                animation.apply();
            }
            this.submitEntry(entry, model, matrices, vertexConsumers, light);
        }
    }

    /**
     * FeatureRenderer.render в 1.21.11 больше не отдаёт постоянный VertexConsumer на кадр
     * (RenderLayer-фабрика method_23578 удалена) — теперь геометрия отправляется в очередь
     * рендера (class_11659#method_73483) вместе с RenderLayer и коллбэком, который получает
     * уже ЗАФИКСИРОВАННую матрицу (class_4587.class_4665), а не общий изменяемый стек: команда
     * может выполниться позже кадра. Поэтому вся иерархия костей строится на локальном стеке,
     * засеянном этой матрицей, а не на входном `matrices`.
     */
    private void submitEntry(CosmeticCatalog.Entry entry, GeoModel model, MatrixStack matrices, OrderedRenderCommandQueue vertexConsumers, int light) {
        RenderLayer layer = RenderLayers.entityCutoutNoCull(entry.texture);
        vertexConsumers.submitCustom(matrices, layer, (matrixEntry, buffer) -> {
            MatrixStack local = new MatrixStack();
            local.peek().copy(matrixEntry);
            this.renderEntry(entry, model, local, buffer, light);
        });
    }

    private void renderEntry(CosmeticCatalog.Entry entry, GeoModel model, MatrixStack matrices, VertexConsumer buffer, int light) {
        PlayerEntityModel playerModel = (PlayerEntityModel)this.getContextModel();
        switch (entry.position()) {
            case ARMS -> {
                this.renderAttached(entry, model, matrices, buffer, light, playerModel.rightArm);
                this.renderAttached(entry, model, matrices, buffer, light, playerModel.leftArm);
            }
            case LEGS -> {
                this.renderAttached(entry, model, matrices, buffer, light, playerModel.rightLeg);
                this.renderAttached(entry, model, matrices, buffer, light, playerModel.leftLeg);
            }
            default -> this.renderAttached(entry, model, matrices, buffer, light, null);
        }
    }

    private void renderAttached(CosmeticCatalog.Entry entry, GeoModel model, MatrixStack matrices, VertexConsumer buffer, int light, ModelPart limb) {
        matrices.push();
        PlayerEntityModel playerModel = (PlayerEntityModel)this.getContextModel();
        float yOffset = 0.0f;
        if (limb != null) {
            CosmeticFeatureRenderer.attachTo(matrices, limb);
        } else {
            switch (entry.position()) {
                case HEAD -> {
                    CosmeticFeatureRenderer.attachTo(matrices, playerModel.head);
                    yOffset = entry.external() ? 0.0f : entry.headOffset();
                }
                case BODY -> {
                    CosmeticFeatureRenderer.attachTo(matrices, playerModel.body);
                    yOffset = entry.external() ? 0.0f : -0.3f;
                }
                default -> {
                }
            }
        }
        // Модели Bedrock смотрят «вверх ногами» относительно модели игрока.
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
        matrices.translate(entry.x(), entry.y() + yOffset, entry.z());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entry.yaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entry.pitch()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(entry.roll()));
        matrices.scale(entry.scale(), entry.scale(), entry.scale());
        float frameV = entry.frameOffsetV();
        for (GeoModel.Bone bone : model.topLevelBones) {
            CosmeticFeatureRenderer.renderBone(bone, matrices, buffer, light, frameV);
        }
        matrices.pop();
    }

    /** Переносит начало координат в точку крепления части тела и поворачивает вместе с ней. */
    private static void attachTo(MatrixStack matrices, ModelPart part) {
        matrices.translate(part.originX / 16.0f, part.originY / 16.0f, part.originZ / 16.0f);
        if (part.roll != 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)Math.toDegrees(part.roll)));
        }
        if (part.yaw != 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)Math.toDegrees(part.yaw)));
        }
        if (part.pitch != 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)Math.toDegrees(part.pitch)));
        }
    }

    private static void renderBone(GeoModel.Bone bone, MatrixStack matrices, VertexConsumer buffer, int light, float frameV) {
        matrices.push();
        matrices.translate(-bone.positionX / 16.0f, bone.positionY / 16.0f, bone.positionZ / 16.0f);
        matrices.translate(bone.pivotX / 16.0f, bone.pivotY / 16.0f, bone.pivotZ / 16.0f);
        CosmeticFeatureRenderer.rotate(matrices, bone.rotationX, bone.rotationY, bone.rotationZ);
        matrices.scale(bone.scaleX, bone.scaleY, bone.scaleZ);
        matrices.translate(-bone.pivotX / 16.0f, -bone.pivotY / 16.0f, -bone.pivotZ / 16.0f);
        for (GeoModel.Cube cube : bone.childCubes) {
            CosmeticFeatureRenderer.renderCube(cube, matrices, buffer, light, frameV);
        }
        for (GeoModel.Bone child : bone.childBones) {
            CosmeticFeatureRenderer.renderBone(child, matrices, buffer, light, frameV);
        }
        matrices.pop();
    }

    private static void renderCube(GeoModel.Cube cube, MatrixStack matrices, VertexConsumer buffer, int light, float frameV) {
        matrices.push();
        matrices.translate(cube.pivotX / 16.0f, cube.pivotY / 16.0f, cube.pivotZ / 16.0f);
        CosmeticFeatureRenderer.rotate(matrices, cube.rotationX, cube.rotationY, cube.rotationZ);
        matrices.translate(-cube.pivotX / 16.0f, -cube.pivotY / 16.0f, -cube.pivotZ / 16.0f);
        Matrix4f position = matrices.peek().getPositionMatrix();
        Matrix3f normalMatrix = matrices.peek().getNormalMatrix();
        Vector3f normal = new Vector3f();
        for (GeoModel.Quad quad : cube.quads) {
            if (quad == null) {
                continue;
            }
            normal.set(quad.normalX, quad.normalY, quad.normalZ);
            normalMatrix.transform(normal);
            // У плоских кубов (толщина 0) нормаль разворачиваем наружу, иначе грань тёмная.
            if ((cube.sizeY == 0.0f || cube.sizeZ == 0.0f) && normal.x < 0.0f) {
                normal.x = -normal.x;
            }
            if ((cube.sizeX == 0.0f || cube.sizeZ == 0.0f) && normal.y < 0.0f) {
                normal.y = -normal.y;
            }
            if ((cube.sizeX == 0.0f || cube.sizeY == 0.0f) && normal.z < 0.0f) {
                normal.z = -normal.z;
            }
            for (GeoModel.Vertex vertex : quad.vertices) {
                buffer.vertex(position, vertex.x, vertex.y, vertex.z)
                        .color(-1)
                        .texture(vertex.u, vertex.v + frameV)
                        .overlay(OverlayTexture.DEFAULT_UV)
                        .light(light)
                        .normal(normal.x, normal.y, normal.z);
            }
        }
        matrices.pop();
    }

    /** Порядок как в GeckoLib: сначала Z, потом Y, потом X. */
    private static void rotate(MatrixStack matrices, float x, float y, float z) {
        if (z != 0.0f) {
            matrices.multiply(new Quaternionf().rotationZ(z));
        }
        if (y != 0.0f) {
            matrices.multiply(new Quaternionf().rotationY(y));
        }
        if (x != 0.0f) {
            matrices.multiply(new Quaternionf().rotationX(x));
        }
    }
}
