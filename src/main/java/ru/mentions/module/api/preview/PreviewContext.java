package ru.mentions.module.api.preview;

import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class PreviewContext {

    private OtherClientPlayerEntity dummy;
    private Vec3d anchor = Vec3d.ZERO;
    private float distance;
    private float height;
    private int phase;
    private long startedAt = System.currentTimeMillis();

    public OtherClientPlayerEntity dummy() {
        return dummy;
    }

    public Vec3d anchor() {
        return anchor;
    }

    public float distance() {
        return distance;
    }

    public float height() {
        return height;
    }

    public int phase() {
        return phase;
    }

    public long elapsedMs() {
        return System.currentTimeMillis() - startedAt;
    }

    public void setDummy(OtherClientPlayerEntity dummy) {
        this.dummy = dummy;
    }

    public void setAnchor(Vec3d anchor) {
        this.anchor = anchor;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public void nextPhase() {
        phase++;
    }

    public void reset() {
        dummy = null;
        anchor = Vec3d.ZERO;
        phase = 0;
        startedAt = System.currentTimeMillis();
    }
}
