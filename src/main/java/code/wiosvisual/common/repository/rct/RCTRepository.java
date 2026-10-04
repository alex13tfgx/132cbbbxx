/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_1268
 *  net.minecraft.class_1707
 *  net.minecraft.class_1713
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_2246
 *  net.minecraft.class_2596
 *  net.minecraft.class_2886
 *  net.minecraft.class_437
 *  net.minecraft.class_476
 *  net.minecraft.class_7439
 *  net.minecraft.class_8646
 */
package code.wiosvisual.common.repository.rct;

import code.wiosvisual.display.hud.Notifications;
import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.managers.event.EventManager;
import code.wiosvisual.utils.client.packet.network.Network;
import code.wiosvisual.utils.client.sound.SoundManager;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.interfaces.QuickLogger;
import code.wiosvisual.utils.interactions.inv.InventoryTask;
import code.wiosvisual.utils.math.time.StopWatch;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

public class RCTRepository
implements QuickImports,
QuickLogger {
    private final StopWatch stopWatch = new StopWatch();
    private final StopWatch hubDelayWatch = new StopWatch();
    private boolean lobby;
    private int anarchy;
    private boolean rwReconnecting;
    private boolean hubSent;
    private boolean anSent;
    private int anarchyBeforeHub;
    private boolean rwLeftWorld;
    private long rwStartTime;

    public RCTRepository(EventManager eventManager) {
        EventManager.register(this);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        GameMessageS2CPacket message;
        String text;
        Packet<?> packet2;
        if (this.anarchy != 0 && (packet2 = e.getPacket()) instanceof GameMessageS2CPacket && !(text = (message = (GameMessageS2CPacket)packet2).content().getString().toLowerCase()).contains("\u0445\u0430\u0431") && text.contains("\u043d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c")) {
            Notifications.getInstance().addList("[RCT] \u041d\u0430 \u0434\u0430\u043d\u043d\u0443\u044e \u0430\u043d\u0430\u0440\u0445\u0438\u044e " + String.valueOf(Formatting.RED) + "\u043d\u0435\u043b\u044c\u0437\u044f" + String.valueOf(Formatting.RESET) + " \u0437\u0430\u0439\u0442\u0438", 3000);
            this.anarchy = 0;
            this.rwReconnecting = false;
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (this.anarchy == 0) {
            return;
        }
        if (Network.isReallyWorld()) {
            this.tickReallyWorld();
        } else if (Network.isSpookyTime() || Network.isFunTime()) {
            this.tickCopyTime();
        } else if (this.anarchy > 0 && (this.hubSent || this.rwReconnecting)) {
            this.tickCopyTime();
        } else {
            this.anarchy = 0;
            this.rwReconnecting = false;
            this.hubSent = false;
            this.anSent = false;
            this.anarchyBeforeHub = -1;
            this.hubDelayWatch.reset();
        }
    }

    private void tickReallyWorld() {
        int currentAnarchy = Network.getAnarchy();
        if (!this.rwReconnecting) {
            this.rwReconnecting = true;
            this.hubSent = false;
            this.rwLeftWorld = false;
            this.anarchyBeforeHub = currentAnarchy;
            this.rwStartTime = System.currentTimeMillis();
            this.hubDelayWatch.reset();
        }
        if (System.currentTimeMillis() - this.rwStartTime > 25000L) {
            Notifications.getInstance().addList("[RCT] \u0422\u0430\u0439\u043c\u0430\u0443\u0442 \u2014 \u043d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0435\u0440\u0435\u0437\u0430\u0439\u0442\u0438 \u043d\u0430 \u0413\u0420\u0418\u0424 #" + String.valueOf(Formatting.BLUE) + this.anarchy + String.valueOf(Formatting.RESET), 3500);
            this.anarchy = 0;
            this.rwReconnecting = false;
            this.hubSent = false;
            this.rwLeftWorld = false;
            this.anarchyBeforeHub = -1;
            return;
        }
        if (!this.hubSent) {
            RCTRepository.mc.player.networkHandler.sendChatCommand("hub");
            this.hubSent = true;
            this.hubDelayWatch.reset();
            return;
        }
        int nowAnarchy = Network.getAnarchy();
        if (!this.rwLeftWorld && nowAnarchy != this.anarchyBeforeHub) {
            this.rwLeftWorld = true;
        }
        if (!this.rwLeftWorld) {
            return;
        }
        if (nowAnarchy == this.anarchy) {
            int completed = this.anarchy;
            SoundManager.playSound(SoundManager.ENABLE_MODULE);
            Notifications.getInstance().addList("\u041f\u0435\u0440\u0435\u0437\u0430\u0445\u043e\u0434 \u043d\u0430 \u0413\u0420\u0418\u0424 #" + String.valueOf(Formatting.BLUE) + completed + String.valueOf(Formatting.RESET) + " \u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d", 3000);
            this.anarchy = 0;
            this.rwReconnecting = false;
            this.hubSent = false;
            this.rwLeftWorld = false;
            this.anarchyBeforeHub = -1;
            return;
        }
        Screen screen2 = RCTRepository.mc.currentScreen;
        if (screen2 instanceof GenericContainerScreen) {
            GenericContainerScreen screen = (GenericContainerScreen)screen2;
            String title = screen.getTitle().getString();
            if (title.contains("\u0412\u044b\u0431\u043e\u0440 \u0441\u0435\u0440\u0432\u0435\u0440\u0430")) {
                for (int i = 0; i < ((GenericContainerScreenHandler)screen.getScreenHandler()).getInventory().size(); ++i) {
                    ItemStack stack = ((GenericContainerScreenHandler)screen.getScreenHandler()).getInventory().getStack(i);
                    String name = stack.getName().getString();
                    if (!name.contains("\u0413\u0420\u0418\u0424\u0415\u0420\u0421\u041a\u041e\u0415 \u0412\u042b\u0416\u0418\u0412\u0410\u041d\u0418\u0415") || stack.getItem() != Blocks.CRAFTING_TABLE.asItem()) continue;
                    InventoryTask.clickSlot(i, 0, SlotActionType.PICKUP, false);
                    return;
                }
            } else if (title.contains("\u0412\u044b\u0431\u043e\u0440 \u043c\u0438\u0440\u0430 \u0433\u0440\u0438\u0444\u0430")) {
                for (int i = 0; i < ((GenericContainerScreenHandler)screen.getScreenHandler()).getInventory().size(); ++i) {
                    ItemStack stack = ((GenericContainerScreenHandler)screen.getScreenHandler()).getInventory().getStack(i);
                    String name = stack.getName().getString();
                    if (!name.contains("\u0413\u0420\u0418\u0424 #" + this.anarchy) || stack.getItem() != Items.PLAYER_HEAD) continue;
                    InventoryTask.clickSlot(i, 0, SlotActionType.PICKUP, false);
                    return;
                }
            }
        } else if (this.stopWatch.every(500.0)) {
            InventoryTask.selectCompass();
            RCTRepository.mc.player.networkHandler.sendPacket((Packet)new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, RCTRepository.mc.player.getInventory().selectedSlot, RCTRepository.mc.player.getYaw(), RCTRepository.mc.player.getPitch()));
        }
    }

    private void tickCopyTime() {
        String serverTitle;
        long elapsed;
        int currentAnarchy = Network.getAnarchy();
        long l = elapsed = this.hubSent ? this.hubDelayWatch.elapsedTime() : 0L;
        serverTitle = RCTRepository.mc.world != null && RCTRepository.mc.world.getScoreboard() != null ? (RCTRepository.mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR) != null ? RCTRepository.mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR).getDisplayName().getString() : "null") : "null";
        if (currentAnarchy == this.anarchy && this.anarchy > 0 && this.rwReconnecting && this.anSent && serverTitle.contains("\u0410\u043d\u0430\u0440\u0445\u0438\u044f-")) {
            int completedAnarchy = this.anarchy;
            SoundManager.playSound(SoundManager.ENABLE_MODULE);
            Notifications.getInstance().addList("\u041f\u0435\u0440\u0435\u0437\u0430\u0445\u043e\u0434 \u043d\u0430 \u0430\u043d\u0430\u0440\u0445\u0438\u044e #" + String.valueOf(Formatting.BLUE) + completedAnarchy + String.valueOf(Formatting.RESET) + " \u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d", 3000);
            this.anarchy = 0;
            this.rwReconnecting = false;
            this.hubSent = false;
            this.anSent = false;
            this.anarchyBeforeHub = -1;
            this.hubDelayWatch.reset();
            return;
        }
        if (currentAnarchy == this.anarchy && this.anarchy > 0 && !this.rwReconnecting) {
            this.rwReconnecting = true;
            this.hubSent = false;
            this.anSent = false;
            this.hubDelayWatch.reset();
        }
        if (this.rwReconnecting) {
            if (!this.hubSent) {
                this.anarchyBeforeHub = currentAnarchy;
                RCTRepository.mc.player.networkHandler.sendChatMessage("/hub");
                this.hubSent = true;
                this.anSent = false;
                this.hubDelayWatch.reset();
            } else if (!this.anSent) {
                boolean worldChanged;
                elapsed = this.hubDelayWatch.elapsedTime();
                boolean bl = worldChanged = currentAnarchy != this.anarchyBeforeHub;
                if (elapsed >= 890L && worldChanged) {
                    RCTRepository.mc.player.networkHandler.sendChatMessage("/an" + this.anarchy);
                    this.anSent = true;
                }
            }
        }
    }

    public void reconnect(int anarchy) {
        if (Network.isReallyWorld()) {
            if (anarchy > 0) {
                this.anarchy = anarchy;
                this.rwReconnecting = false;
                this.hubSent = false;
                this.rwLeftWorld = false;
                this.anarchyBeforeHub = -1;
                this.rwStartTime = 0L;
            } else {
                Notifications.getInstance().addList("[RCT] \u041d\u0435\u0432\u0435\u0440\u043d\u044b\u0439 " + String.valueOf(Formatting.RED) + "\u0433\u0440\u0438\u0444", 3000);
            }
        } else if (Network.isSpookyTime() || Network.isFunTime()) {
            if (anarchy > 0) {
                this.anarchy = anarchy;
                this.rwReconnecting = false;
                this.hubSent = false;
                this.hubDelayWatch.reset();
            } else {
                int currentAnarchy = Network.getAnarchy();
                if (currentAnarchy > 0) {
                    this.anarchy = currentAnarchy;
                    this.rwReconnecting = false;
                    this.hubSent = false;
                    this.hubDelayWatch.reset();
                } else {
                    Notifications.getInstance().addList("[RCT] \u041d\u0435\u0432\u0435\u0440\u043d\u044b\u0439 \u043d\u043e\u043c\u0435\u0440 " + String.valueOf(Formatting.RED) + "\u0430\u043d\u0430\u0440\u0445\u0438\u0438", 3000);
                }
            }
        }
    }
}
