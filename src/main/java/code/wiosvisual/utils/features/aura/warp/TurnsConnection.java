/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_1309
 *  net.minecraft.class_243
 *  net.minecraft.class_2596
 *  net.minecraft.class_2708
 *  net.minecraft.class_2828
 *  net.minecraft.class_3532
 */
package code.wiosvisual.utils.features.aura.warp;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.events.player.PlayerVelocityStrafeEvent;
import code.wiosvisual.events.player.RotationUpdateEvent;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.managers.event.EventManager;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.features.aura.rotations.constructor.LinearConstructor;
import code.wiosvisual.utils.features.aura.utils.MathAngle;
import code.wiosvisual.utils.features.aura.warp.Turns;
import code.wiosvisual.utils.features.aura.warp.TurnsConfig;
import code.wiosvisual.utils.features.aura.warp.TurnsConstructor;
import code.wiosvisual.utils.math.task.TaskPriority;
import code.wiosvisual.utils.math.task.TaskProcessor;
import java.lang.runtime.SwitchBootstraps;
import java.util.Objects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class TurnsConnection
implements QuickImports {
    public static TurnsConnection INSTANCE = new TurnsConnection();
    private TurnsConstructor lastRotationPlan;
    private final TaskProcessor<TurnsConstructor> rotationPlanTaskProcessor = new TaskProcessor();
    public Turns currentAngle;
    private Turns previousAngle;
    private Turns serverAngle = Turns.DEFAULT;
    private boolean rotateBack = false;
    private long rotateBackTime = -1L;
    private Turns manualAngle;
    private int manualAngleTicks = 0;
    private LinearConstructor linearConstructor = new LinearConstructor();

    public TurnsConnection() {
        Wiosvisual.getInstance().getEventManager();
        EventManager.register(this);
    }

    public void setRotation(Turns value) {
        this.previousAngle = value == null ? (this.currentAngle != null ? this.currentAngle : MathAngle.cameraAngle()) : this.currentAngle;
        this.currentAngle = value;
    }

    public void holdRotation(Turns value) {
        this.rotationPlanTaskProcessor.activeTasks.clear();
        this.lastRotationPlan = null;
        this.rotationPlanTaskProcessor.tickCounter = 0;
        this.manualAngle = value;
        this.manualAngleTicks = 2;
        this.rotateBack = false;
        this.rotateBackTime = -1L;
        this.setRotation(value);
    }

    public Turns getRotation() {
        return this.currentAngle != null ? this.currentAngle : MathAngle.cameraAngle();
    }

    public Turns getPreviousRotation() {
        if (this.currentAngle != null && this.previousAngle != null) {
            return this.previousAngle;
        }
        if (TurnsConnection.mc.player == null) {
            return new Turns(0.0f, 0.0f);
        }
        return new Turns(TurnsConnection.mc.player.lastYaw, TurnsConnection.mc.player.lastPitch);
    }

    public Turns getMoveRotation() {
        TurnsConstructor rotationPlan = this.getCurrentRotationPlan();
        return this.currentAngle != null && rotationPlan != null && rotationPlan.isMoveCorrection() ? this.currentAngle : MathAngle.cameraAngle();
    }

    public TurnsConstructor getCurrentRotationPlan() {
        return this.rotationPlanTaskProcessor.fetchActiveTaskValue() != null ? this.rotationPlanTaskProcessor.fetchActiveTaskValue() : this.lastRotationPlan;
    }

    public void rotateTo(Turns.VecRotation vecRotation, LivingEntity entity, int reset, TurnsConfig configurable, TaskPriority taskPriority, Module provider) {
        this.rotateTo(configurable.createRotationPlan(vecRotation.getAngle(), vecRotation.getVec(), (Entity)entity, reset), taskPriority, provider);
    }

    public void rotateTo(Turns angle, int reset, TurnsConfig configurable, TaskPriority taskPriority, Module provider) {
        this.rotateTo(configurable.createRotationPlan(angle, angle.toVector(), null, reset), taskPriority, provider);
    }

    public void rotateTo(Turns angle, TurnsConfig configurable, TaskPriority taskPriority, Module provider) {
        this.rotateTo(configurable.createRotationPlan(angle, angle.toVector(), null, 1), taskPriority, provider);
    }

    public void rotateTo(TurnsConstructor plan, TaskPriority taskPriority, Module provider) {
        this.rotationPlanTaskProcessor.addTask(new TaskProcessor.Task<TurnsConstructor>(1, taskPriority.getPriority(), provider, plan));
    }

    public void update() {
        boolean hasActiveTask;
        if (this.manualAngleTicks > 0 && this.manualAngle != null) {
            this.rotateBack = false;
            this.rotateBackTime = -1L;
            this.setRotation(this.manualAngle.adjustSensitivity());
            --this.manualAngleTicks;
            if (this.manualAngleTicks <= 0) {
                this.manualAngle = null;
            }
            return;
        }
        TurnsConstructor activePlan = this.getCurrentRotationPlan();
        if (this.rotationPlanTaskProcessor.fetchActiveTaskValue() == null) {
            if (!this.rotateBack) {
                this.rotateBackTime = System.currentTimeMillis();
                this.rotateBack = true;
            }
        } else {
            this.rotateBack = false;
            this.rotateBackTime = -1L;
        }
        Turns clientAngle = MathAngle.cameraAngle();
        boolean bl = hasActiveTask = this.rotationPlanTaskProcessor.fetchActiveTaskValue() != null;
        if (hasActiveTask) {
            Turns newAngle = activePlan.nextRotation(this.currentAngle != null ? this.currentAngle : clientAngle, false).adjustSensitivity();
            this.setRotation(newAngle);
            this.lastRotationPlan = activePlan;
            this.rotationPlanTaskProcessor.tick(1);
        }
        if (this.rotateBack) {
            if (this.currentAngle == null) {
                this.rotateBack = false;
                return;
            }
            if (System.currentTimeMillis() - this.rotateBackTime < 1000L) {
                Turns delta = MathAngle.calculateDelta(this.currentAngle, clientAngle).adjustSensitivity();
                this.setRotation(new Turns(MathHelper.lerp((float)0.5f, (float)this.currentAngle.getYaw(), (float)(this.currentAngle.getYaw() + delta.getYaw())), MathHelper.lerp((float)0.5f, (float)this.currentAngle.getPitch(), (float)(this.currentAngle.getPitch() + delta.getPitch()))));
            } else {
                this.setRotation(null);
                this.lastRotationPlan = null;
                this.rotateBack = false;
            }
        }
    }

    public static double computeRotationDifference(Turns a, Turns b) {
        return Math.hypot(Math.abs(TurnsConnection.computeAngleDifference(a.getYaw(), b.getYaw())), Math.abs(a.getPitch() - b.getPitch()));
    }

    public static float computeAngleDifference(float a, float b) {
        return MathHelper.wrapDegrees((float)(a - b));
    }

    private Vec3d fixVelocity(Vec3d currVelocity, Vec3d movementInput, float speed) {
        Float yaw = this.resolveMoveCorrectionYaw();
        if (yaw == null) {
            return currVelocity;
        }
        double d = movementInput.lengthSquared();
        if (d < 1.0E-7) {
            return Vec3d.ZERO;
        }
        Vec3d vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).multiply((double)speed);
        float f = MathHelper.sin((float)(yaw.floatValue() * ((float)Math.PI / 180)));
        float g = MathHelper.cos((float)(yaw.floatValue() * ((float)Math.PI / 180)));
        return new Vec3d(vec3d.getX() * (double)g - vec3d.getZ() * (double)f, vec3d.getY(), vec3d.getZ() * (double)g + vec3d.getX() * (double)f);
    }

    private Float resolveMoveCorrectionYaw() {
        if (this.currentAngle != null) {
            return Float.valueOf(this.currentAngle.getYaw());
        }
        return null;
    }

    public void clear() {
        this.rotationPlanTaskProcessor.activeTasks.clear();
        this.lastRotationPlan = null;
        this.rotationPlanTaskProcessor.tickCounter = 0;
        this.manualAngle = null;
        this.manualAngleTicks = 0;
    }

    @EventHandler
    public void onPlayerVelocityStrafe(PlayerVelocityStrafeEvent e) {
        TurnsConstructor currentRotationPlan = this.getCurrentRotationPlan();
        if (currentRotationPlan != null && currentRotationPlan.isMoveCorrection()) {
            e.setVelocity(this.fixVelocity(e.getVelocity(), e.getMovementInput(), e.getSpeed()));
            return;
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        SafeEvents.dispatch(() -> new RotationUpdateEvent((byte)0));
        this.update();
        SafeEvents.dispatch(() -> new RotationUpdateEvent((byte)2));
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (!event.isCancelled()) {
            Packet<?> packet2 = event.getPacket();
            Objects.requireNonNull(packet2);
            Packet<?> packet3 = packet2;
            int n = 0;
            block4: while (true) {
                switch (code.wiosvisual.utils.TypeSwitch.typeSwitch(new Object[]{PlayerMoveC2SPacket.class, PlayerPositionLookS2CPacket.class}, packet3, n)) {
                    case 0: {
                        PlayerMoveC2SPacket player = (PlayerMoveC2SPacket)packet3;
                        if (!player.changesLook()) {
                            n = 1;
                            continue block4;
                        }
                        this.serverAngle = new Turns(player.getYaw(1.0f), player.getPitch(1.0f));
                        break block4;
                    }
                    case 1: {
                        PlayerPositionLookS2CPacket player = (PlayerPositionLookS2CPacket)packet3;
                        this.serverAngle = new Turns(player.change().yaw(), player.change().pitch());
                        break block4;
                    }
                }
                break;
            }
        }
    }

    public TurnsConstructor getLastRotationPlan() {
        return this.lastRotationPlan;
    }

    public TaskProcessor<TurnsConstructor> getRotationPlanTaskProcessor() {
        return this.rotationPlanTaskProcessor;
    }

    public Turns getCurrentAngle() {
        return this.currentAngle;
    }

    public Turns getPreviousAngle() {
        return this.previousAngle;
    }

    public Turns getServerAngle() {
        return this.serverAngle;
    }

    public boolean isRotateBack() {
        return this.rotateBack;
    }

    public long getRotateBackTime() {
        return this.rotateBackTime;
    }

    public Turns getManualAngle() {
        return this.manualAngle;
    }

    public int getManualAngleTicks() {
        return this.manualAngleTicks;
    }

    public LinearConstructor getLinearConstructor() {
        return this.linearConstructor;
    }
}
