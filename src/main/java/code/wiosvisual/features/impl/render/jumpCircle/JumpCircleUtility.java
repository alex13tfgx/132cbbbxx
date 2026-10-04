/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.features.impl.render.jumpCircle;

public class JumpCircleUtility {
    public static boolean isExpired(long startTime, float lifeTime) {
        return (float)(System.currentTimeMillis() - startTime) > lifeTime;
    }
}
