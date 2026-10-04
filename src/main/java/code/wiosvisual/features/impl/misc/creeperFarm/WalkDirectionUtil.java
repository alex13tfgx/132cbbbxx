/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.NonNull
 *  net.minecraft.class_10185
 *  net.minecraft.class_1297
 *  net.minecraft.class_243
 *  net.minecraft.class_3532
 */
package code.wiosvisual.features.impl.misc.creeperFarm;

import code.wiosvisual.utils.input.MoveUtil;
import lombok.NonNull;
import net.minecraft.entity.Entity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class WalkDirectionUtil {
    public static PlayerInput fromVec3d(Vec3d vec3d, float yaw) {
        Vec3d direction = vec3d.normalize();
        float movementAngle = (float)Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0f;
        float angleDiff = MathHelper.wrapDegrees((float)(movementAngle - yaw));
        return MoveUtil.getPlayerInput(angleDiff);
    }

    public static PlayerInput fromEntity(@NonNull Entity entity, float yaw) {
        if (entity == null) {
            throw new NullPointerException("entity is marked non-null but is null");
        }
        return WalkDirectionUtil.fromVec3d(new net.minecraft.util.math.Vec3d(entity.getX(), entity.getY(), entity.getRandomBodyY()), yaw);
    }
}
