/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1268
 *  net.minecraft.class_1291
 *  net.minecraft.class_1297
 *  net.minecraft.class_1309
 *  net.minecraft.class_2246
 *  net.minecraft.class_2248
 *  net.minecraft.class_2338
 *  net.minecraft.class_2350
 *  net.minecraft.class_238
 *  net.minecraft.class_2382
 *  net.minecraft.class_243
 *  net.minecraft.class_2596
 *  net.minecraft.class_2680
 *  net.minecraft.class_2824
 *  net.minecraft.class_2828$class_2830
 *  net.minecraft.class_2848
 *  net.minecraft.class_2848$class_2849
 *  net.minecraft.class_2886
 *  net.minecraft.class_304
 *  net.minecraft.class_3532
 *  net.minecraft.class_3675$class_307
 *  net.minecraft.class_3965
 *  net.minecraft.class_4050
 *  net.minecraft.class_408
 *  net.minecraft.class_437
 *  net.minecraft.class_6880
 *  net.minecraft.class_7204
 *  org.lwjgl.glfw.GLFW
 */
package code.wiosvisual.utils.interactions.interact;

import code.wiosvisual.features.module.setting.implement.BindSetting;
import code.wiosvisual.utils.client.packet.network.ServerUtil;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.features.aura.utils.MathAngle;
import code.wiosvisual.utils.features.aura.warp.Turns;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.lwjgl.glfw.GLFW;

public final class PlayerInteractionHelper
implements QuickImports {
    public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        PlayerInteractionHelper.mc.interactionManager.sendSequencedPacket(PlayerInteractionHelper.mc.world, packetCreator);
    }

    public static void interactItem(Hand hand) {
        PlayerInteractionHelper.interactItem(hand, MathAngle.cameraAngle());
    }

    public static void interactItem(Hand hand, Turns angle) {
        PlayerInteractionHelper.sendSequencedPacket(i -> new PlayerInteractItemC2SPacket(hand, i, angle.getYaw(), angle.getPitch()));
    }

    public static void interactEntity(Entity entity) {
        PlayerInteractionHelper.mc.player.networkHandler.sendPacket((Packet)PlayerInteractEntityC2SPacket.interactAt((Entity)entity, (boolean)false, (Hand)Hand.MAIN_HAND, (Vec3d)entity.getBoundingBox().getCenter()));
        PlayerInteractionHelper.mc.player.networkHandler.sendPacket((Packet)PlayerInteractEntityC2SPacket.interact((Entity)entity, (boolean)false, (Hand)Hand.MAIN_HAND));
    }

    public static void startFallFlying() {
        PlayerInteractionHelper.mc.player.networkHandler.sendPacket((Packet)new ClientCommandC2SPacket((Entity)PlayerInteractionHelper.mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        PlayerInteractionHelper.mc.player.startGliding();
    }

    public static void sendPacketWithOutEvent(Packet<?> packet) {
        mc.getNetworkHandler().getConnection().send(packet, null);
    }

    public static void grimSuperBypass$$$(double y, Turns angle) {
        PlayerInteractionHelper.mc.player.networkHandler.sendPacket((Packet)new PlayerMoveC2SPacket.Full(PlayerInteractionHelper.mc.player.getX(), PlayerInteractionHelper.mc.player.getY() + y, PlayerInteractionHelper.mc.player.getZ(), angle.getYaw(), angle.getPitch(), PlayerInteractionHelper.mc.player.isOnGround(), PlayerInteractionHelper.mc.player.horizontalCollision));
    }

    public static String getHealthString(LivingEntity entity) {
        return PlayerInteractionHelper.getHealthString(PlayerInteractionHelper.getHealth(entity));
    }

    public static String getHealthString(float hp) {
        return String.format("%.1f", Float.valueOf(hp)).replace(",", ".").replace(".0", "");
    }

    public static float getHealth(LivingEntity entity) {
        float hp = ServerUtil.getHealthValue(entity) + entity.getAbsorptionAmount();
        return MathHelper.clamp((float)hp, (float)0.0f, (float)(ServerUtil.getMaxHealth(entity) + entity.getAbsorptionAmount()));
    }

    public static void jump() {
        if (PlayerInteractionHelper.mc.player.isSprinting()) {
            float g = PlayerInteractionHelper.mc.player.getYaw() * ((float)Math.PI / 180);
            PlayerInteractionHelper.mc.player.addVelocityInternal(new Vec3d((double)(-MathHelper.sin((float)g) * 0.2f), 0.0, (double)(MathHelper.cos((float)g) * 0.2f)));
        }
    }

    public static List<BlockPos> getCube(BlockPos center, float radius) {
        return PlayerInteractionHelper.getCube(center, radius, radius, true);
    }

    public static List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY) {
        return PlayerInteractionHelper.getCube(center, radiusXZ, radiusY, true);
    }

    public static List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY, boolean down) {
        ArrayList<BlockPos> positions = new ArrayList<BlockPos>();
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();
        int posY = down ? centerY - (int)radiusY : centerY;
        int x = centerX - (int)radiusXZ;
        while ((float)x <= (float)centerX + radiusXZ) {
            int z = centerZ - (int)radiusXZ;
            while ((float)z <= (float)centerZ + radiusXZ) {
                int y = posY;
                while ((float)y <= (float)centerY + radiusY) {
                    positions.add(new BlockPos(x, y, z));
                    ++y;
                }
                ++z;
            }
            ++x;
        }
        return positions;
    }

    public static List<BlockPos> getCube(BlockPos start, BlockPos end) {
        ArrayList<BlockPos> positions = new ArrayList<BlockPos>();
        for (int x = start.getX(); x <= end.getX(); ++x) {
            for (int z = start.getZ(); z <= end.getZ(); ++z) {
                for (int y = start.getY(); y <= end.getY(); ++y) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }
        return positions;
    }

    public static InputUtil.Type getKeyType(int key) {
        return key < 8 ? InputUtil.Type.MOUSE : InputUtil.Type.KEYSYM;
    }

    public static Stream<Entity> streamEntities() {
        return StreamSupport.stream(PlayerInteractionHelper.mc.world.getEntities().spliterator(), false);
    }

    public static boolean canChangeIntoPose(EntityPose pose, Vec3d pos) {
        return PlayerInteractionHelper.mc.player.world.isSpaceEmpty((Entity)PlayerInteractionHelper.mc.player, PlayerInteractionHelper.mc.player.getDimensions(pose).getBoxAt(pos).contract(1.0E-7));
    }

    public static boolean isPotionActive(RegistryEntry<StatusEffect> statusEffect) {
        return PlayerInteractionHelper.mc.player.getActiveStatusEffects().containsKey(statusEffect);
    }

    public static boolean isPlayerInBlock(Block block) {
        return PlayerInteractionHelper.isBoxInBlock(PlayerInteractionHelper.mc.player.getBoundingBox().expand(-0.001), block);
    }

    public static boolean isBoxInBlock(Box box, Block block) {
        return PlayerInteractionHelper.isBox(box, pos -> PlayerInteractionHelper.mc.world.getBlockState(pos).getBlock().equals(block));
    }

    public static boolean isBoxInBlocks(Box box, List<Block> blocks) {
        return PlayerInteractionHelper.isBox(box, pos -> blocks.contains(PlayerInteractionHelper.mc.world.getBlockState(pos).getBlock()));
    }

    public static boolean isBox(Box box, Predicate<BlockPos> pos) {
        return BlockPos.stream((Box)box).anyMatch(pos);
    }

    public static boolean isKey(BindSetting setting) {
        if (PlayerInteractionHelper.mc.currentScreen != null || !setting.isVisible()) {
            return false;
        }
        int key = setting.getKey();
        if (setting.isCombo()) {
            int secondKey = setting.getSecondKey();
            return PlayerInteractionHelper.isKey(PlayerInteractionHelper.getKeyType(key), key) && PlayerInteractionHelper.isKey(PlayerInteractionHelper.getKeyType(secondKey), secondKey);
        }
        return PlayerInteractionHelper.isKey(PlayerInteractionHelper.getKeyType(key), key);
    }

    public static boolean isKey(KeyBinding key) {
        return PlayerInteractionHelper.isKey(key.getDefaultKey().getCategory(), key.getDefaultKey().getCode());
    }

    public static boolean isKey(InputUtil.Type type, int keyCode) {
        if (keyCode != -1) {
            switch (type) {
                case KEYSYM: {
                    return GLFW.glfwGetKey((long)mc.getWindow().getHandle(), (int)keyCode) == 1;
                }
                case MOUSE: {
                    return GLFW.glfwGetMouseButton((long)mc.getWindow().getHandle(), (int)keyCode) == 1;
                }
            }
        }
        return false;
    }

    public static boolean isAir(BlockPos blockPos) {
        return PlayerInteractionHelper.isAir(PlayerInteractionHelper.mc.world.getBlockState(blockPos));
    }

    public static boolean isAir(BlockState state) {
        return state.isAir() || state.getBlock().equals(Blocks.CAVE_AIR) || state.getBlock().equals(Blocks.VOID_AIR);
    }

    public static boolean isChat(Screen screen) {
        return screen instanceof ChatScreen;
    }

    public static boolean nullCheck() {
        return PlayerInteractionHelper.mc.player == null || PlayerInteractionHelper.mc.world == null;
    }

    public static Vec3d getSafePositionNearChest(BlockPos chest, Direction side) {
        BlockPos[] candidates;
        if (PlayerInteractionHelper.mc.world == null) {
            return null;
        }
        for (BlockPos p : candidates = new BlockPos[]{chest.offset(side), chest.up(), chest.offset(side).up(), chest.up(2)}) {
            if (PlayerInteractionHelper.mc.world.getBlockState(p).isSolid()) continue;
            return Vec3d.ofCenter((Vec3i)p);
        }
        return null;
    }

    public static void interactBlockChestWithInBlockBypass(BlockPos chest, Vec3d center, Direction side) {
        Vec3d safe = PlayerInteractionHelper.getSafePositionNearChest(chest, side);
        if (safe != null) {
            PlayerInteractionHelper.mc.player.networkHandler.sendPacket((Packet)new PlayerMoveC2SPacket.Full(safe.x, safe.y, safe.z, PlayerInteractionHelper.mc.player.getYaw(), PlayerInteractionHelper.mc.player.getPitch(), PlayerInteractionHelper.mc.player.isOnGround(), false));
        }
        PlayerInteractionHelper.mc.interactionManager.interactBlock(PlayerInteractionHelper.mc.player, Hand.MAIN_HAND, new BlockHitResult(center, side, chest, false));
    }

    private PlayerInteractionHelper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
