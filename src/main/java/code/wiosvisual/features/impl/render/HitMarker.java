/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_10055
 *  net.minecraft.class_1007
 *  net.minecraft.class_1297
 *  net.minecraft.class_1309
 *  net.minecraft.class_1657
 *  net.minecraft.class_239
 *  net.minecraft.class_243
 *  net.minecraft.class_3532
 *  net.minecraft.class_3966
 *  net.minecraft.class_4050
 *  net.minecraft.class_4184
 *  net.minecraft.class_4587
 *  net.minecraft.class_591
 *  net.minecraft.class_638
 *  net.minecraft.class_745
 *  net.minecraft.class_7833
 *  net.minecraft.class_897
 */
package code.wiosvisual.features.impl.render;

import code.wiosvisual.events.player.AttackEvent;
import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

public class HitMarker
extends Module {
    private static final long DURATION = 1000L;
    private static final int MAX_MARKERS = 24;
    private static final float WORLD_SCALE = 0.012f;
    private final SliderSettings sizeSetting = new SliderSettings("\u0420\u0430\u0437\u043c\u0435\u0440", "\u0420\u0430\u0437\u043c\u0435\u0440 \u0445\u0438\u0442-\u043c\u0430\u0440\u043a\u0435\u0440\u0430").setValue(12.0f).range(6, 30);
    private final List<Marker> markers = new ArrayList<Marker>();

    public static HitMarker getInstance() {
        return Instance.get(HitMarker.class);
    }

    public HitMarker() {
        super("Hit Marker", "Hit Marker", ModuleCategory.RENDER);
        this.setup(this.sizeSetting);
    }

    @Override
    public void deactivate() {
        this.markers.clear();
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        // Удар отменён (например, NoFriendDamage) — реагировать не на что.
        if (e.isCancelled()) {
            return;
        }
        EntityHitResult result;
        LivingEntity living;
        Entity entity = e.getEntity();
        if (!(entity instanceof LivingEntity) || (living = (LivingEntity)entity) == HitMarker.mc.player || !living.isAlive()) {
            return;
        }
        HitResult hitResult = HitMarker.mc.crosshairTarget;
        Vec3d hitPos = hitResult instanceof EntityHitResult && (result = (EntityHitResult)hitResult).getEntity() == entity ? result.getPos() : living.getBoundingBox().getCenter();
        this.markers.add(new Marker(hitPos, System.currentTimeMillis()));
        if (this.markers.size() > 24) {
            this.markers.remove(0);
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (HitMarker.mc.player == null || HitMarker.mc.world == null || HitMarker.mc.gameRenderer == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Camera camera = HitMarker.mc.gameRenderer.getCamera();
        MatrixStack matrices = e.getStack();
        if (!this.markers.isEmpty()) {
            this.renderMarkers(now, camera, matrices);
        }
    }

    private void renderMarkers(long now, Camera camera, MatrixStack matrices) {
        Iterator<Marker> iterator = this.markers.iterator();
        while (iterator.hasNext()) {
            Marker marker = iterator.next();
            long elapsed = now - marker.time;
            if (elapsed > 1000L) {
                iterator.remove();
                continue;
            }
            float progress = (float)elapsed / 1000.0f;
            float fade = 1.0f - progress;
            float scale = 1.0f + progress * 0.35f;
            float size = this.sizeSetting.getValue() * scale;
            float half = size / 2.0f;
            float gap = size * 0.18f;
            float thickness = Math.max(1.5f, size / 7.0f);
            int color = ColorAssist.multAlpha(ColorAssist.getHudColor(), fade);
            matrices.push();
            matrices.translate(marker.pos.x, marker.pos.y, marker.pos.z);
            matrices.multiply(camera.getRotation());
            matrices.scale(-0.012f, -0.012f, 0.012f);
            HitMarker.drawCross(matrices, 0.0f, 0.0f, half, gap, thickness, color);
            matrices.pop();
        }
    }

    private static void drawCross(MatrixStack matrix, float cx, float cy, float half, float gap, float thickness, int color) {
        HitMarker.drawArm(matrix, cx, cy, -1, -1, half, gap, thickness, color);
        HitMarker.drawArm(matrix, cx, cy, 1, -1, half, gap, thickness, color);
        HitMarker.drawArm(matrix, cx, cy, 1, 1, half, gap, thickness, color);
        HitMarker.drawArm(matrix, cx, cy, -1, 1, half, gap, thickness, color);
    }

    private static void drawArm(MatrixStack matrix, float cx, float cy, int sx, int sy, float half, float gap, float thickness, int color) {
        float nx = (float)sx / (float)Math.sqrt(2.0);
        float ny = (float)sy / (float)Math.sqrt(2.0);
        float x1 = cx + nx * gap;
        float y1 = cy + ny * gap;
        float x2 = cx + nx * half;
        float y2 = cy + ny * half;
        Render2D.drawLine(matrix, x1, y1, x2, y2, thickness, color);
    }

    private record Marker(Vec3d pos, long time) {
    }

}
