/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1657
 *  net.minecraft.class_243
 */
package code.wiosvisual.features.impl.render.jumpCircle.data;

import code.wiosvisual.common.animation.implement.OutBack;
import code.wiosvisual.features.impl.render.jumpCircle.data.JumpCircleLayer;
import code.wiosvisual.features.impl.render.jumpCircle.receiver.JumpCircleModule;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class JumpCircleStorage {
    private final List<JumpCircleLayer> jumpCircleLayers = new CopyOnWriteArrayList<JumpCircleLayer>();
    private final JumpCircleModule module;

    public JumpCircleStorage(JumpCircleModule module) {
        this.module = module;
    }

    public void add(Vec3d vec3d) {
        int lifeTime = (int)this.module.getLifeTime().getValue();
        this.jumpCircleLayers.add(new JumpCircleLayer(vec3d, new OutBack().setMs(lifeTime).setValue(1.0)));
    }

    public void add(PlayerEntity player) {
        this.add(new net.minecraft.util.math.Vec3d(player.getX(), player.getY(), player.getZ()).add(0.0, (double)0.01f, 0.0));
    }

    public boolean isPresent() {
        return !this.jumpCircleLayers.isEmpty();
    }

    public List<JumpCircleLayer> getJumpCircleLayers() {
        return this.jumpCircleLayers;
    }

    public JumpCircleModule getModule() {
        return this.module;
    }
}
