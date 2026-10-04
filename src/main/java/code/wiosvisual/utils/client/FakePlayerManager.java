/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  net.minecraft.class_1297
 *  net.minecraft.class_1297$class_5529
 *  net.minecraft.class_1304
 *  net.minecraft.class_1799
 *  net.minecraft.class_243
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_3532
 *  net.minecraft.class_4050
 *  net.minecraft.class_745
 */
package code.wiosvisual.utils.client;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class FakePlayerManager {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final UUID FAKE_UUID = UUID.nameUUIDFromBytes("WiosvisualFakePlayer".getBytes(StandardCharsets.UTF_8));
    private static final String DEFAULT_NAME = "FakePlayer";
    private static OtherClientPlayerEntity fakePlayer;

    private FakePlayerManager() {
    }

    public static boolean add(String name) {
        if (FakePlayerManager.mc.world == null || FakePlayerManager.mc.player == null) {
            return false;
        }
        FakePlayerManager.remove();
        String cleanName = FakePlayerManager.sanitizeName(name);
        GameProfile profile = new GameProfile(FAKE_UUID, cleanName);
        OtherClientPlayerEntity entity = new OtherClientPlayerEntity(FakePlayerManager.mc.world, profile);
        float yawRad = FakePlayerManager.mc.player.getYaw() * (float)Math.PI / 180.0f;
        Vec3d forward = new Vec3d((double)(-MathHelper.sin((float)yawRad)), 0.0, (double)MathHelper.cos((float)yawRad)).normalize();
        Vec3d pos = new net.minecraft.util.math.Vec3d(FakePlayerManager.mc.player.getX(), FakePlayerManager.mc.player.getY(), FakePlayerManager.mc.player.getRandomBodyY()).add(forward.multiply(2.5));
        entity.refreshPositionAndAngles(pos.x, FakePlayerManager.mc.player.getY(), pos.z, FakePlayerManager.mc.player.getYaw() + 180.0f, 0.0f);
        entity.headYaw = entity.getYaw();
        entity.bodyYaw = entity.getYaw();
        entity.setPose(EntityPose.STANDING);
        entity.setHealth(entity.getMaxHealth());
        entity.setAbsorptionAmount(0.0f);
        entity.setInvulnerable(true);
        entity.setCustomName((Text)Text.literal((String)cleanName));
        entity.setCustomNameVisible(true);
        entity.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        entity.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        FakePlayerManager.mc.world.addEntity((Entity)entity);
        fakePlayer = entity;
        return true;
    }

    public static boolean remove() {
        if (fakePlayer == null) {
            return false;
        }
        if (FakePlayerManager.mc.world != null) {
            FakePlayerManager.mc.world.removeEntity(fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
        }
        fakePlayer = null;
        return true;
    }

    public static boolean handleAttack(Entity target) {
        if (!FakePlayerManager.isFakePlayer(target)) {
            return false;
        }
        FakePlayerManager.restoreHealth();
        return true;
    }

    public static boolean isSpawned() {
        return fakePlayer != null && !fakePlayer.isRemoved();
    }

    public static boolean isFakePlayer(Entity entity) {
        return fakePlayer != null && entity != null && entity.getId() == fakePlayer.getId();
    }

    public static boolean isFakePlayer(UUID uuid) {
        return fakePlayer != null && uuid != null && uuid.equals(fakePlayer.getUuid());
    }

    private static void restoreHealth() {
        if (fakePlayer == null) {
            return;
        }
        fakePlayer.setHealth(fakePlayer.getMaxHealth());
        fakePlayer.setAbsorptionAmount(0.0f);
        FakePlayerManager.fakePlayer.hurtTime = 0;
        FakePlayerManager.fakePlayer.maxHurtTime = 0;
    }

    private static String sanitizeName(String name) {
        if (name == null || name.isBlank()) {
            return DEFAULT_NAME;
        }
        String cleanName = name.trim();
        if (cleanName.length() > 16) {
            cleanName = cleanName.substring(0, 16);
        }
        return cleanName;
    }
}
