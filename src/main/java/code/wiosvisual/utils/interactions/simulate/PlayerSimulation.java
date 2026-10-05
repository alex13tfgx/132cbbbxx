/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap
 *  it.unimi.dsi.fastutil.objects.Object2DoubleMap
 *  net.minecraft.class_10185
 *  net.minecraft.class_1291
 *  net.minecraft.class_1293
 *  net.minecraft.class_1294
 *  net.minecraft.class_1297
 *  net.minecraft.class_1320
 *  net.minecraft.class_1657
 *  net.minecraft.class_1690
 *  net.minecraft.class_1922
 *  net.minecraft.class_1937
 *  net.minecraft.class_2246
 *  net.minecraft.class_2248
 *  net.minecraft.class_2338
 *  net.minecraft.class_2338$class_2339
 *  net.minecraft.class_2350
 *  net.minecraft.class_238
 *  net.minecraft.class_2399
 *  net.minecraft.class_243
 *  net.minecraft.class_2533
 *  net.minecraft.class_2680
 *  net.minecraft.class_2769
 *  net.minecraft.class_3481
 *  net.minecraft.class_3486
 *  net.minecraft.class_3532
 *  net.minecraft.class_3610
 *  net.minecraft.class_3611
 *  net.minecraft.class_5134
 *  net.minecraft.class_5635
 *  net.minecraft.class_6862
 *  net.minecraft.class_6880
 *  net.minecraft.class_744
 *  net.minecraft.class_746
 */
package code.wiosvisual.utils.interactions.simulate;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.interactions.simulate.Simulations;
import code.wiosvisual.utils.interactions.simulate.interfaces.Simulation;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.state.property.Property;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class PlayerSimulation
implements Simulation,
QuickImports {
    public final PlayerEntity player;
    public final SimulatedPlayerInput input;
    public Vec3d pos;
    public Vec3d velocity;
    public Box boundingBox;
    public float yaw;
    public float pitch;
    public boolean sprinting;
    public float fallDistance;
    public int jumpingCooldown;
    public boolean isJumping;
    public boolean isFallFlying;
    public boolean onGround;
    public boolean horizontalCollision;
    public boolean verticalCollision;
    public boolean touchingWater;
    public boolean isSwimming;
    public boolean submergedInWater;
    private final Object2DoubleMap<TagKey<Fluid>> fluidHeight;
    private final HashSet<TagKey<Fluid>> submergedFluidTag;
    private int simulatedTicks = 0;
    private boolean clipLedged = false;
    private static final double STEP_HEIGHT = 0.5;

    public PlayerSimulation(PlayerEntity player, SimulatedPlayerInput input, Vec3d pos, Vec3d velocity, Box boundingBox, float yaw, float pitch, boolean sprinting, float fallDistance, int jumpingCooldown, boolean isJumping, boolean isFallFlying, boolean onGround, boolean horizontalCollision, boolean verticalCollision, boolean touchingWater, boolean isSwimming, boolean submergedInWater, Object2DoubleMap<TagKey<Fluid>> fluidHeight, HashSet<TagKey<Fluid>> submergedFluidTag) {
        this.player = player;
        this.input = input;
        this.pos = pos;
        this.velocity = velocity;
        this.boundingBox = boundingBox;
        this.yaw = yaw;
        this.pitch = pitch;
        this.sprinting = sprinting;
        this.fallDistance = fallDistance;
        this.jumpingCooldown = jumpingCooldown;
        this.isJumping = isJumping;
        this.isFallFlying = isFallFlying;
        this.onGround = onGround;
        this.horizontalCollision = horizontalCollision;
        this.verticalCollision = verticalCollision;
        this.touchingWater = touchingWater;
        this.isSwimming = isSwimming;
        this.submergedInWater = submergedInWater;
        this.fluidHeight = fluidHeight;
        this.submergedFluidTag = submergedFluidTag;
    }

    public static PlayerSimulation simulateLocalPlayer(int ticks) {
        PlayerSimulation simulatedPlayer = PlayerSimulation.fromClientPlayer(SimulatedPlayerInput.fromClientPlayer(PlayerSimulation.mc.player.input.playerInput));
        for (int i = 0; i < ticks; ++i) {
            simulatedPlayer.tick();
        }
        return simulatedPlayer;
    }

    public static PlayerSimulation simulateOtherPlayer(PlayerEntity player, int ticks) {
        PlayerSimulation simulatedPlayer = PlayerSimulation.fromOtherPlayer(player, SimulatedPlayerInput.guessInput(player));
        for (int i = 0; i < ticks; ++i) {
            simulatedPlayer.tick();
        }
        return simulatedPlayer;
    }

    public static PlayerSimulation fromClientPlayer(SimulatedPlayerInput input) {
        ClientPlayerEntity player = PlayerSimulation.mc.player;
        return new PlayerSimulation((PlayerEntity)player, input, new net.minecraft.util.math.Vec3d(player.getX(), player.getY(), player.getZ()), player.getVelocity(), player.getBoundingBox(), player.getYaw(), player.getPitch(), player.isSprinting(), (float)player.fallDistance, player.jumpingCooldown, player.jumping, player.isGliding(), player.isOnGround(), player.horizontalCollision, player.verticalCollision, player.isTouchingWater(), player.isSwimming(), player.isSubmergedInWater(), (Object2DoubleMap<TagKey<Fluid>>)new Object2DoubleArrayMap(player.fluidHeight), new HashSet<TagKey<Fluid>>(player.submergedFluidTag));
    }

    public static PlayerSimulation fromOtherPlayer(PlayerEntity player, SimulatedPlayerInput input) {
        return new PlayerSimulation(player, input, new net.minecraft.util.math.Vec3d(player.getX(), player.getY(), player.getZ()), new net.minecraft.util.math.Vec3d(player.getX(), player.getY(), player.getZ()).subtract(new Vec3d(player.lastX, player.lastY, player.lastZ)), player.getBoundingBox(), player.getYaw(), player.getPitch(), player.isSprinting(), (float)player.fallDistance, player.jumpingCooldown, player.jumping, player.isGliding(), player.isOnGround(), player.horizontalCollision, player.verticalCollision, player.isTouchingWater(), player.isSwimming(), player.isSubmergedInWater(), (Object2DoubleMap<TagKey<Fluid>>)new Object2DoubleArrayMap(player.fluidHeight), new HashSet<TagKey<Fluid>>(player.submergedFluidTag));
    }

    @Override
    public Vec3d pos() {
        return new net.minecraft.util.math.Vec3d(this.player.getX(), this.player.getY(), this.player.getZ());
    }

    @Override
    public void tick() {
        ++this.simulatedTicks;
        this.clipLedged = false;
        if (this.pos.y <= -70.0) {
            return;
        }
        this.input.update();
        this.checkWaterState();
        this.updateSubmergedInWaterState();
        this.updateSwimming();
        if (this.jumpingCooldown > 0) {
            --this.jumpingCooldown;
        }
        this.isJumping = this.input.playerInput.jump();
        double newX = this.velocity.x;
        double newY = this.velocity.y;
        double newZ = this.velocity.z;
        if (Math.abs(this.velocity.x) < 0.003) {
            newX = 0.0;
        }
        if (Math.abs(this.velocity.y) < 0.003) {
            newY = 0.0;
        }
        if (Math.abs(this.velocity.z) < 0.003) {
            newZ = 0.0;
        }
        if (this.onGround) {
            this.isFallFlying = false;
        }
        this.velocity = new Vec3d(newX, newY, newZ);
        if (this.isJumping) {
            double fluidLevel = this.isInLava() ? this.getFluidHeight((TagKey<Fluid>)FluidTags.LAVA) : this.getFluidHeight((TagKey<Fluid>)FluidTags.WATER);
            boolean inWater = this.isTouchingWater() && fluidLevel > 0.0;
            double swimHeight = this.getSwimHeight();
            if (inWater && (!this.onGround || fluidLevel > swimHeight)) {
                this.swimUpward((TagKey<Fluid>)FluidTags.WATER);
            } else if (this.isInLava() && (!this.onGround || fluidLevel > swimHeight)) {
                this.swimUpward((TagKey<Fluid>)FluidTags.LAVA);
            } else if ((this.onGround || inWater && fluidLevel <= swimHeight) && this.jumpingCooldown == 0) {
                this.jump();
                if (this.player.equals((Object)PlayerSimulation.mc.player)) {
                    this.jumpingCooldown = 10;
                }
            }
        }
        float sidewaysSpeed = this.input.movementSideways * 0.98f;
        float forwardSpeed = this.input.movementForward * 0.98f;
        float upwardsSpeed = 0.0f;
        if (this.hasStatusEffect((RegistryEntry<StatusEffect>)StatusEffects.SLOW_FALLING) || this.hasStatusEffect((RegistryEntry<StatusEffect>)StatusEffects.LEVITATION)) {
            this.onLanding();
        }
        this.travel(new Vec3d((double)sidewaysSpeed, (double)upwardsSpeed, (double)forwardSpeed));
    }

    private void travel(Vec3d movementInput) {
        boolean falling;
        if (this.isSwimming && !this.player.hasVehicle()) {
            double g = this.getRotationVector().y;
            double h = g < -0.2 ? 0.085 : 0.06;
            BlockPos posAbove = new BlockPos(MathHelper.floor((double)this.pos.x), MathHelper.floor((double)(this.pos.y + 1.0 - 0.1)), MathHelper.floor((double)this.pos.z));
            if (g <= 0.0 || this.input.playerInput.jump() || !this.player.world.getBlockState(posAbove).getFluidState().isEmpty()) {
                this.velocity = this.velocity.add(0.0, (g - this.velocity.y) * h, 0.0);
            }
        }
        double beforeTravelVelocityY = this.velocity.y;
        double d = 0.08;
        boolean bl = falling = this.velocity.y <= 0.0;
        if (this.velocity.y <= 0.0 && this.hasStatusEffect((RegistryEntry<StatusEffect>)StatusEffects.SLOW_FALLING)) {
            d = 0.01;
            this.onLanding();
        }
        if (this.isTouchingWater() && this.player.shouldSwimInFluids()) {
            Vec3d vec3d2;
            double e = this.pos.y;
            float f = this.isSprinting() ? 0.9f : 0.8f;
            float g = 0.02f;
            float h = (float)this.getAttributeValue((RegistryEntry<EntityAttribute>)EntityAttributes.WATER_MOVEMENT_EFFICIENCY);
            if (!this.onGround) {
                h *= 0.5f;
            }
            if (h > 0.0f) {
                f += (0.54600006f - f) * h / 3.0f;
                g += (this.getMovementSpeed() - g) * h / 3.0f;
            }
            if (this.hasStatusEffect((RegistryEntry<StatusEffect>)StatusEffects.DOLPHINS_GRACE)) {
                f = 0.96f;
            }
            this.updateVelocity(g, movementInput);
            this.move(this.velocity);
            Vec3d tempVel = this.velocity;
            if (this.horizontalCollision && this.isClimbing()) {
                tempVel = new Vec3d(tempVel.x, 0.2, tempVel.z);
            }
            this.velocity = tempVel.multiply((double)f, 0.8, (double)f);
            this.velocity = vec3d2 = this.player.applyFluidMovingSpeed(d, falling, this.velocity);
            if (this.horizontalCollision && this.doesNotCollide(vec3d2.x, vec3d2.y + 0.6 - this.pos.y + e, vec3d2.z)) {
                this.velocity = new Vec3d(vec3d2.x, 0.3, vec3d2.z);
            }
        } else if (this.isInLava() && this.player.shouldSwimInFluids()) {
            double e = this.pos.y;
            this.updateVelocity(0.02f, movementInput);
            this.move(this.velocity);
            if (this.getFluidHeight((TagKey<Fluid>)FluidTags.LAVA) <= this.getSwimHeight()) {
                this.velocity = this.velocity.multiply(0.5, 0.8, 0.5);
                this.velocity = this.player.applyFluidMovingSpeed(d, falling, this.velocity);
            } else {
                this.velocity = this.velocity.multiply(0.5);
            }
            if (!this.player.hasNoGravity()) {
                this.velocity = this.velocity.add(0.0, -d / 4.0, 0.0);
            }
            if (this.horizontalCollision && this.doesNotCollide(this.velocity.x, this.velocity.y + 0.6 - this.pos.y + e, this.velocity.z)) {
                this.velocity = new Vec3d(this.velocity.x, 0.3, this.velocity.z);
            }
        } else if (this.isFallFlying) {
            double k;
            Vec3d e = this.velocity;
            if (e.y > -0.5) {
                this.fallDistance = 1.0f;
            }
            Vec3d vec3d3 = this.getRotationVector();
            float f = this.pitch * ((float)Math.PI / 180);
            double g = Math.sqrt(vec3d3.x * vec3d3.x + vec3d3.z * vec3d3.z);
            double horizontalSpeed = this.velocity.horizontalLength();
            double i = vec3d3.length();
            float j = MathHelper.cos((float)f);
            j = (float)((double)j * ((double)j * Math.min(1.0, i / 0.4)));
            e = this.velocity.add(0.0, d * (-1.0 + (double)j * 0.75), 0.0);
            if (e.y < 0.0 && g > 0.0) {
                k = e.y * -0.1 * (double)j;
                e = e.add(vec3d3.x * k / g, k, vec3d3.z * k / g);
            }
            if (f < 0.0f && g > 0.0) {
                k = horizontalSpeed * (double)(-MathHelper.sin((float)f)) * 0.04;
                e = e.add(-vec3d3.x * k / g, k * 3.2, -vec3d3.z * k / g);
            }
            if (g > 0.0) {
                e = e.add((vec3d3.x / g * horizontalSpeed - e.x) * 0.1, 0.0, (vec3d3.z / g * horizontalSpeed - e.z) * 0.1);
            }
            this.velocity = e.multiply(0.99, 0.98, 0.99);
            this.move(this.velocity);
        } else {
            BlockPos blockPos = this.getVelocityAffectingPos();
            float p = this.player.world.getBlockState(blockPos).getBlock().getSlipperiness();
            float f = this.onGround ? p * 0.91f : 0.91f;
            Vec3d vec3d6 = this.applyMovementInput(movementInput, p);
            double q = vec3d6.y;
            if (this.hasStatusEffect((RegistryEntry<StatusEffect>)StatusEffects.LEVITATION)) {
                StatusEffectInstance levitation = this.getStatusEffect((RegistryEntry<StatusEffect>)StatusEffects.LEVITATION);
                if (levitation != null) {
                    q += (0.05 * (double)(levitation.getAmplifier() + 1) - vec3d6.y) * 0.2;
                }
            } else if (this.player.world.isClient() && !this.player.world.isChunkLoaded(blockPos)) {
                q = this.pos.y > (double)this.player.world.getBottomY() ? -0.1 : 0.0;
            } else if (!this.player.hasNoGravity()) {
                q -= d;
            }
            this.velocity = this.player.hasNoDrag() ? new Vec3d(vec3d6.x, q, vec3d6.z) : new Vec3d(vec3d6.x * (double)f, q * (double)0.98f, vec3d6.z * (double)f);
        }
        if (this.player.getAbilities().flying && !this.player.hasVehicle()) {
            this.velocity = new Vec3d(this.velocity.x, beforeTravelVelocityY * 0.6, this.velocity.z);
            this.onLanding();
        }
    }

    private Vec3d applyMovementInput(Vec3d movementInput, float slipperiness) {
        this.updateVelocity(this.getMovementSpeed(slipperiness), movementInput);
        this.velocity = this.applyClimbingSpeed(this.velocity);
        this.move(this.velocity);
        Vec3d result = this.velocity;
        BlockPos posBlock = this.posToBlockPos(this.pos);
        BlockState state = this.getState(posBlock);
        if ((this.horizontalCollision || this.isJumping) && (this.isClimbing() || state != null && state.isOf(Blocks.POWDER_SNOW) && PowderSnowBlock.canWalkOnPowderSnow((Entity)this.player))) {
            result = new Vec3d(result.x, 0.2, result.z);
        }
        return result;
    }

    private void updateVelocity(float speed, Vec3d movementInput) {
        Vec3d vec = Entity.movementInputToVelocity((Vec3d)movementInput, (float)speed, (float)this.yaw);
        this.velocity = this.velocity.add(vec);
    }

    private float getMovementSpeed(float slipperiness) {
        return this.onGround ? this.getMovementSpeed() * (0.21600002f / (slipperiness * slipperiness * slipperiness)) : this.getAirStrafingSpeed();
    }

    private float getAirStrafingSpeed() {
        float speed = 0.02f;
        if (this.input.playerInput.sprint()) {
            return speed + 0.006f;
        }
        return speed;
    }

    private float getMovementSpeed() {
        return 0.1f;
    }

    private void move(Vec3d movement) {
        Vec3d modifiedMovement = movement;
        Vec3d adjustedMovement = this.adjustMovementForCollisions(modifiedMovement = this.adjustMovementForSneaking(modifiedMovement));
        if (adjustedMovement.lengthSquared() > 1.0E-7) {
            this.pos = this.pos.add(adjustedMovement);
            this.boundingBox = this.player.dimensions.getBoxAt(this.pos);
        }
        boolean xCollision = !MathHelper.approximatelyEquals((double)movement.x, (double)adjustedMovement.x);
        boolean zCollision = !MathHelper.approximatelyEquals((double)movement.z, (double)adjustedMovement.z);
        this.horizontalCollision = xCollision || zCollision;
        this.verticalCollision = movement.y != adjustedMovement.y;
        boolean bl = this.onGround = this.verticalCollision && movement.y < 0.0;
        if (!this.isTouchingWater()) {
            this.checkWaterState();
        }
        if (this.onGround) {
            this.onLanding();
        } else if (movement.y < 0.0) {
            this.fallDistance -= (float)movement.y;
        }
        Vec3d currentVel = this.velocity;
        if (this.horizontalCollision || this.verticalCollision) {
            this.velocity = new Vec3d(xCollision ? 0.0 : currentVel.x, this.onGround ? 0.0 : currentVel.y, zCollision ? 0.0 : currentVel.z);
        }
    }

    private Vec3d adjustMovementForCollisions(Vec3d movement) {
        boolean stepPossible;
        Box box = new Box(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3).offset(this.pos);
        List collisionShapes = Collections.emptyList();
        Vec3d adjusted = movement.lengthSquared() == 0.0 ? movement : Entity.adjustMovementForCollisions((Entity)this.player, (Vec3d)movement, (Box)box, (World)this.player.world, collisionShapes);
        boolean xCollide = movement.x != adjusted.x;
        boolean yCollide = movement.y != adjusted.y;
        boolean zCollide = movement.z != adjusted.z;
        boolean bl = stepPossible = this.onGround || yCollide && movement.y < 0.0;
        if (this.player.getStepHeight() > 0.0f && stepPossible && (xCollide || zCollide)) {
            Vec3d stepAdjust = Entity.adjustMovementForCollisions((Entity)this.player, (Vec3d)new Vec3d(movement.x, (double)this.player.getStepHeight(), movement.z), (Box)box, (World)this.player.world, collisionShapes);
            Vec3d stepOffset = Entity.adjustMovementForCollisions((Entity)this.player, (Vec3d)new Vec3d(0.0, (double)this.player.getStepHeight(), 0.0), (Box)box.stretch(movement.x, 0.0, movement.z), (World)this.player.world, collisionShapes);
            Vec3d combined = Entity.adjustMovementForCollisions((Entity)this.player, (Vec3d)new Vec3d(movement.x, 0.0, movement.z), (Box)box.offset(stepOffset), (World)this.player.world, collisionShapes).add(stepOffset);
            if (stepOffset.y < (double)this.player.getStepHeight() && combined.horizontalLengthSquared() > stepAdjust.horizontalLengthSquared()) {
                stepAdjust = combined;
            }
            if (stepAdjust.horizontalLengthSquared() > adjusted.horizontalLengthSquared()) {
                return stepAdjust.add(Entity.adjustMovementForCollisions((Entity)this.player, (Vec3d)new Vec3d(0.0, -stepAdjust.y + movement.y, 0.0), (Box)box.offset(stepAdjust), (World)this.player.world, collisionShapes));
            }
        }
        return adjusted;
    }

    private void onLanding() {
        this.fallDistance = 0.0f;
    }

    public void jump() {
        this.velocity = this.velocity.add(0.0, (double)this.getJumpVelocity() - this.velocity.y, 0.0);
        if (this.isSprinting()) {
            float rad = (float)Math.toRadians(this.yaw);
            this.velocity = this.velocity.add((double)(-MathHelper.sin((float)rad)) * 0.2, 0.0, (double)MathHelper.cos((float)rad) * 0.2);
        }
    }

    private Vec3d applyClimbingSpeed(Vec3d motion) {
        if (!this.isClimbing()) {
            return motion;
        }
        this.onLanding();
        double clampedX = MathHelper.clamp((double)motion.x, (double)-0.15f, (double)0.15f);
        double clampedZ = MathHelper.clamp((double)motion.z, (double)-0.15f, (double)0.15f);
        double clampedY = Math.max(motion.y, (double)-0.15f);
        if (clampedY < 0.0 && !this.getState(this.posToBlockPos(this.pos)).isOf(Blocks.SCAFFOLDING) && this.player.isHoldingOntoLadder()) {
            clampedY = 0.0;
        }
        return new Vec3d(clampedX, clampedY, clampedZ);
    }

    public boolean isClimbing() {
        BlockPos posBlock = this.posToBlockPos(this.pos);
        BlockState state = this.getState(posBlock);
        if (state.isIn(BlockTags.CLIMBABLE)) {
            return true;
        }
        return state.getBlock() instanceof TrapdoorBlock && this.canEnterTrapdoor(posBlock, state);
    }

    private boolean canEnterTrapdoor(BlockPos pos, BlockState state) {
        if (!((Boolean)state.get((Property)TrapdoorBlock.OPEN)).booleanValue()) {
            return false;
        }
        BlockState below = this.player.world.getBlockState(pos.down());
        return below.isOf(Blocks.LADDER) && ((Direction)below.get((Property)LadderBlock.FACING)).equals((Object)state.get((Property)TrapdoorBlock.FACING));
    }

    private Vec3d adjustMovementForSneaking(Vec3d movement) {
        if (movement.y <= 0.0 && this.method_30263()) {
            double dx;
            double dz = movement.z;
            double step = 0.05;
            for (dx = movement.x; dx != 0.0 && this.player.world.isSpaceEmpty((Entity)this.player, this.boundingBox.offset(dx, -0.5, 0.0)); dx += dx > 0.0 ? -step : step) {
                if (!(dx < step) || !(dx >= -step)) continue;
                dx = 0.0;
                break;
            }
            while (dz != 0.0 && this.player.world.isSpaceEmpty((Entity)this.player, this.boundingBox.offset(0.0, -0.5, dz))) {
                if (dz < step && dz >= -step) {
                    dz = 0.0;
                    break;
                }
                dz += dz > 0.0 ? -step : step;
            }
            while (dx != 0.0 && dz != 0.0 && this.player.world.isSpaceEmpty((Entity)this.player, this.boundingBox.offset(dx, -0.5, dz))) {
                double d = dx < step && dx >= -step ? 0.0 : (dx = dx > 0.0 ? dx - step : dx + step);
                if (dz < step && dz >= -step) {
                    dz = 0.0;
                    break;
                }
                dz += dz > 0.0 ? -step : step;
            }
            if (movement.x != dx || movement.z != dz) {
                this.clipLedged = true;
            }
            if (this.shouldClipAtLedge()) {
                movement = new Vec3d(dx, movement.y, dz);
            }
        }
        return movement;
    }

    protected boolean shouldClipAtLedge() {
        return this.input.playerInput.sneak() || this.input.forceSafeWalk;
    }

    private boolean method_30263() {
        return this.onGround || (double)this.fallDistance < 0.5 && !this.player.world.isSpaceEmpty((Entity)this.player, this.boundingBox.offset(0.0, (double)this.fallDistance - 0.5, 0.0));
    }

    private boolean isSprinting() {
        return this.sprinting;
    }

    private float getJumpVelocity() {
        return 0.42f * this.getJumpVelocityMultiplier() + this.getJumpBoostVelocityModifier();
    }

    private float getJumpBoostVelocityModifier() {
        if (this.hasStatusEffect((RegistryEntry<StatusEffect>)StatusEffects.JUMP_BOOST)) {
            StatusEffectInstance boost = this.getStatusEffect((RegistryEntry<StatusEffect>)StatusEffects.JUMP_BOOST);
            return 0.1f * (float)(boost.getAmplifier() + 1);
        }
        return 0.0f;
    }

    private float getJumpVelocityMultiplier() {
        float multiplier1 = 0.0f;
        Block block = this.getState(this.posToBlockPos(this.pos)).getBlock();
        if (block != null) {
            multiplier1 = block.getJumpVelocityMultiplier();
        }
        float multiplier2 = 0.0f;
        Block block2 = this.getState(this.getVelocityAffectingPos()).getBlock();
        if (block2 != null) {
            multiplier2 = block2.getJumpVelocityMultiplier();
        }
        return multiplier1 == 1.0f ? multiplier2 : multiplier1;
    }

    private boolean doesNotCollide(double offsetX, double offsetY, double offsetZ) {
        return this.doesNotCollide(this.boundingBox.offset(offsetX, offsetY, offsetZ));
    }

    private boolean doesNotCollide(Box box) {
        return this.player.world.isSpaceEmpty((Entity)this.player, box) && !this.player.world.containsFluid(box);
    }

    private void swimUpward(TagKey<Fluid> fluidTag) {
        this.velocity = this.velocity.add(0.0, (double)0.04f, 0.0);
    }

    private BlockPos getVelocityAffectingPos() {
        return BlockPos.ofFloored((double)this.pos.x, (double)(this.boundingBox.minY - 0.5000001), (double)this.pos.z);
    }

    private double getSwimHeight() {
        return (double)this.player.getStandingEyeHeight() < 0.4 ? 0.0 : 0.4;
    }

    private boolean isTouchingWater() {
        return this.touchingWater;
    }

    public boolean isInLava() {
        return this.fluidHeight.getDouble((Object)FluidTags.LAVA) > 0.0;
    }

    private void checkWaterState() {
        BoatEntity boat;
        if (this.player.getVehicle() instanceof BoatEntity && !(boat = (BoatEntity)this.player.getVehicle()).isSubmergedInWater()) {
            this.touchingWater = false;
            return;
        }
        if (this.updateMovementInFluid((TagKey<Fluid>)FluidTags.WATER, 0.014)) {
            this.onLanding();
            this.touchingWater = true;
        } else {
            this.touchingWater = false;
        }
    }

    private void updateSwimming() {
        this.isSwimming = this.isSwimming ? this.isSprinting() && this.isTouchingWater() && !this.player.hasVehicle() : this.isSprinting() && this.isSubmergedInWater() && !this.player.hasVehicle() && this.player.world.getFluidState(this.posToBlockPos(this.pos)).isIn(FluidTags.WATER);
    }

    private void updateSubmergedInWaterState() {
        BoatEntity boat;
        this.submergedInWater = this.submergedFluidTag.contains(FluidTags.WATER);
        this.submergedFluidTag.clear();
        double eyeLevel = this.getEyeY() - 0.1111111119389534;
        Entity vehicle = this.player.getVehicle();
        if (vehicle instanceof BoatEntity && !(boat = (BoatEntity)vehicle).isSubmergedInWater() && boat.getBoundingBox().maxY >= eyeLevel && boat.getBoundingBox().minY <= eyeLevel) {
            return;
        }
        BlockPos posEye = BlockPos.ofFloored((double)this.pos.x, (double)eyeLevel, (double)this.pos.z);
        FluidState fluidState = this.player.world.getFluidState(posEye);
        double height = (float)posEye.getY() + fluidState.getHeight((BlockView)this.player.world, posEye);
        if (height > eyeLevel) {
            this.submergedFluidTag.addAll(fluidState.streamTags().toList());
        }
    }

    private double getEyeY() {
        return this.pos.y + (double)this.player.getStandingEyeHeight();
    }

    public boolean isSubmergedInWater() {
        return this.submergedInWater && this.isTouchingWater();
    }

    private double getFluidHeight(TagKey<Fluid> tag) {
        return this.fluidHeight.getDouble(tag);
    }

    private boolean updateMovementInFluid(TagKey<Fluid> tag, double speed) {
        if (this.isRegionUnloaded()) {
            return false;
        }
        Box box = this.boundingBox.contract(0.001);
        int i = MathHelper.floor((double)box.minX);
        int j = MathHelper.ceil((double)box.maxX);
        int k = MathHelper.floor((double)box.minY);
        int l = MathHelper.ceil((double)box.maxY);
        int m = MathHelper.floor((double)box.minZ);
        int n = MathHelper.ceil((double)box.maxZ);
        double d = 0.0;
        boolean pushedByFluids = true;
        boolean foundFluid = false;
        Vec3d fluidVelocity = Vec3d.ZERO;
        int count = 0;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int p = i; p < j; ++p) {
            for (int q = k; q < l; ++q) {
                for (int r = m; r < n; ++r) {
                    double e;
                    mutable.set(p, q, r);
                    FluidState fluidState = this.player.world.getFluidState((BlockPos)mutable);
                    if (!fluidState.isIn(tag) || !((e = (double)((float)q + fluidState.getHeight((BlockView)this.player.world, (BlockPos)mutable))) >= box.minY)) continue;
                    foundFluid = true;
                    d = Math.max(e - box.minY, d);
                    if (!pushedByFluids) continue;
                    Vec3d vel = fluidState.getVelocity((BlockView)this.player.world, (BlockPos)mutable);
                    if (d < 0.4) {
                        vel = vel.multiply(d);
                    }
                    fluidVelocity = fluidVelocity.add(vel);
                    ++count;
                }
            }
        }
        if (fluidVelocity.length() > 0.0) {
            if (count > 0) {
                fluidVelocity = fluidVelocity.multiply(1.0 / (double)count);
            }
            fluidVelocity = fluidVelocity.multiply(speed);
            if (Math.abs(this.velocity.x) < 0.003 && Math.abs(this.velocity.z) < 0.003 && fluidVelocity.length() < 0.0045) {
                fluidVelocity = fluidVelocity.normalize().multiply(0.0045);
            }
            this.velocity = this.velocity.add(fluidVelocity);
        }
        this.fluidHeight.put(tag, d);
        return foundFluid;
    }

    private boolean isRegionUnloaded() {
        Box box = this.boundingBox.expand(1.0);
        int i = MathHelper.floor((double)box.minX);
        int j = MathHelper.ceil((double)box.maxX);
        int k = MathHelper.floor((double)box.minZ);
        int l = MathHelper.ceil((double)box.maxZ);
        return !this.player.world.isRegionLoaded(i, k, j, l);
    }

    private Vec3d getRotationVector() {
        return this.getRotationVector(this.pitch, this.yaw);
    }

    private Vec3d getRotationVector(float pitch, float yaw) {
        float f = (float)((double)pitch * Math.PI / 180.0);
        float g = (float)((double)(-yaw) * Math.PI / 180.0);
        float h = MathHelper.cos((float)g);
        float i = MathHelper.sin((float)g);
        float j = MathHelper.cos((float)f);
        float k = MathHelper.sin((float)f);
        return new Vec3d((double)(i * j), (double)(-k), (double)(h * j));
    }

    public boolean hasStatusEffect(RegistryEntry<StatusEffect> effect) {
        StatusEffectInstance instance = this.player.getStatusEffect(effect);
        return instance != null && instance.getDuration() >= this.simulatedTicks;
    }

    private StatusEffectInstance getStatusEffect(RegistryEntry<StatusEffect> effect) {
        StatusEffectInstance instance = this.player.getStatusEffect(effect);
        if (instance == null || instance.getDuration() < this.simulatedTicks) {
            return null;
        }
        return instance;
    }

    public double getAttributeValue(RegistryEntry<EntityAttribute> attribute) {
        return this.player.getAttributes().getValue(attribute);
    }

    public PlayerSimulation clone() {
        return new PlayerSimulation(this.player, this.input, this.pos, this.velocity, this.boundingBox, this.yaw, this.pitch, this.sprinting, this.fallDistance, this.jumpingCooldown, this.isJumping, this.isFallFlying, this.onGround, this.horizontalCollision, this.verticalCollision, this.touchingWater, this.isSwimming, this.submergedInWater, (Object2DoubleMap<TagKey<Fluid>>)new Object2DoubleArrayMap(this.fluidHeight), new HashSet<TagKey<Fluid>>(this.submergedFluidTag));
    }

    public BlockPos posToBlockPos(Vec3d pos) {
        return new BlockPos(MathHelper.floor((double)pos.x), MathHelper.floor((double)pos.y), MathHelper.floor((double)pos.z));
    }

    public BlockState getState(BlockPos pos) {
        return this.player.world.getBlockState(pos);
    }

    public static class SimulatedPlayerInput
    extends Input {
        public boolean forceSafeWalk = false;
        public float movementForward;
        public float movementSideways;
        public PlayerInput playerInput;
        public static final double MAX_WALKING_SPEED = 0.121;

        public SimulatedPlayerInput(PlayerInput input) {
            this.playerInput = input;
        }

        public void update() {
            this.movementForward = this.playerInput.forward() != this.playerInput.backward() ? (this.playerInput.forward() ? 1.0f : -1.0f) : 0.0f;
            if (this.playerInput.left() == this.playerInput.right()) {
                this.movementSideways = 0.0f;
            } else {
                float f = this.movementSideways = this.playerInput.left() ? 1.0f : -1.0f;
            }
            if (this.playerInput.sneak()) {
                this.movementSideways *= 0.3f;
                this.movementForward *= 0.3f;
            }
        }

        public String toString() {
            return "SimulatedPlayerInput(forwards={" + this.playerInput.forward() + "}, backwards={" + this.playerInput.backward() + "}, left={" + this.playerInput.left() + "}, right={" + this.playerInput.right() + "}, jumping={" + this.playerInput.jump() + "}, sprinting=" + this.playerInput.sprint() + ", slowDown=" + this.playerInput.sneak() + ")";
        }

        public static SimulatedPlayerInput fromClientPlayer(PlayerInput input) {
            return new SimulatedPlayerInput(input);
        }

        public static SimulatedPlayerInput guessInput(PlayerEntity entity) {
            Vec3d velocity = new net.minecraft.util.math.Vec3d(entity.getX(), entity.getY(), entity.getZ()).subtract(new Vec3d(entity.lastX, entity.lastY, entity.lastZ));
            double horizontalVelocity = velocity.horizontalLengthSquared();
            PlayerInput input = new PlayerInput(false, false, false, false, !entity.isOnGround(), entity.isSneaking(), horizontalVelocity >= 0.014641);
            if (horizontalVelocity > 0.0025000000000000005) {
                double velocityAngle = Simulations.getDegreesRelativeToView(velocity, entity.getYaw());
                double wrappedAngle = MathHelper.wrapDegrees((double)velocityAngle);
                input = Simulations.getDirectionalInputForDegrees(input, wrappedAngle);
            }
            return new SimulatedPlayerInput(input);
        }
    }
}
