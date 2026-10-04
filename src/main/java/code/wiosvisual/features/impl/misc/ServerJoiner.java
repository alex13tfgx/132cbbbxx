/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1268
 *  net.minecraft.class_1707
 *  net.minecraft.class_1713
 *  net.minecraft.class_1735
 *  net.minecraft.class_1802
 *  net.minecraft.class_2596
 *  net.minecraft.class_2661
 *  net.minecraft.class_2886
 *  net.minecraft.class_437
 *  net.minecraft.class_476
 *  org.lwjgl.glfw.GLFW
 */
package code.wiosvisual.features.impl.misc;

import code.wiosvisual.display.hud.Notifications;
import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.features.module.setting.implement.TextSetting;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.packet.network.Network;
import code.wiosvisual.utils.interactions.inv.InventoryTask;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class ServerJoiner
extends Module {
    private final SelectSetting serverSelection = new SelectSetting("\u0421\u0435\u0440\u0432\u0435\u0440", "\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0446\u0435\u043b\u0435\u0432\u043e\u0439 \u0441\u0435\u0440\u0432\u0435\u0440").value("\u0420\u0438\u043b\u043b\u0438\u0412\u043e\u0440\u043b\u0434", "\u0421\u043f\u0443\u043a\u0438\u0422\u0430\u0439\u043c \u0434\u0443\u044d\u043b\u0438").selected("\u0420\u0438\u043b\u043b\u0438\u0412\u043e\u0440\u043b\u0434");
    private final SliderSettings griefSelection = new SliderSettings("\u041d\u043e\u043c\u0435\u0440 \u0433\u0440\u0438\u0444\u0430", "\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u043d\u043e\u043c\u0435\u0440 \u0441\u0435\u0440\u0432\u0435\u0440\u0430 \u0434\u043b\u044f \u0433\u0440\u0438\u0444\u0430").setValue(1.0f).range(1, 54).visible(() -> this.serverSelection.isSelected("\u0420\u0438\u043b\u043b\u0438\u0412\u043e\u0440\u043b\u0434"));
    private final TextSetting spookyCompassName = new TextSetting("\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 \u043a\u043e\u043c\u043f\u0430\u0441\u0430", "\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 \u043a\u043e\u043c\u043f\u0430\u0441\u0430 \u0434\u043b\u044f SpookyTime").setText("\u0432\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0440\u0435\u0436\u0438\u043c").setMin(1).setMax(64).visible(() -> this.serverSelection.isSelected("\u0421\u043f\u0443\u043a\u0438\u0422\u0430\u0439\u043c \u0434\u0443\u044d\u043b\u0438"));
    private long lastActionTime;
    private boolean isToggling;
    private boolean retryDuels;

    public ServerJoiner() {
        super("ServerJoiner", "Server Joiner", ModuleCategory.MISC);
        this.setup(this.serverSelection, this.griefSelection, this.spookyCompassName);
    }

    public static ServerJoiner getInstance() {
        return Instance.get(ServerJoiner.class);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (!this.state) {
            return;
        }
        if (GLFW.glfwGetKey((long)mc.getWindow().getHandle(), (int)260) == 1) {
            this.setState(false);
            return;
        }
        if (ServerJoiner.mc.currentScreen == null && ServerJoiner.mc.player != null) {
            if (this.serverSelection.isSelected("\u0421\u043f\u0443\u043a\u0438\u0422\u0430\u0439\u043c \u0434\u0443\u044d\u043b\u0438")) {
                if (System.currentTimeMillis() - this.lastActionTime > 250L) {
                    this.useSpookyCompassOrDisable();
                }
            } else if (ServerJoiner.mc.player.age < 5) {
                InventoryTask.selectCompass();
                ServerJoiner.mc.player.networkHandler.sendPacket((Packet)new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, ServerJoiner.mc.player.getInventory().selectedSlot, ServerJoiner.mc.player.getYaw(), ServerJoiner.mc.player.getPitch()));
            }
            return;
        }
        Screen screen2 = ServerJoiner.mc.currentScreen;
        if (!(screen2 instanceof GenericContainerScreen)) {
            return;
        }
        GenericContainerScreen container = (GenericContainerScreen)screen2;
        for (int i = 0; i < ((GenericContainerScreenHandler)container.getScreenHandler()).slots.size(); ++i) {
            String title;
            String slotName = ((Slot)((GenericContainerScreenHandler)container.getScreenHandler()).slots.get(i)).getStack().getName().getString().toLowerCase();
            if (this.serverSelection.isSelected("\u0420\u0438\u043b\u043b\u0438\u0412\u043e\u0440\u043b\u0434") && Network.isReallyWorld()) {
                int numberGrief;
                if (slotName.contains("\u0433\u0440\u0438\u0444\u0435\u0440\u0441\u043a\u043e\u0435 \u0432\u044b\u0436\u0438\u0432\u0430\u043d\u0438\u0435") && this.canClick()) {
                    InventoryTask.clickSlot(i, 0, SlotActionType.PICKUP, false);
                    this.lastActionTime = System.currentTimeMillis();
                }
                if (!slotName.contains("\u0433\u0440\u0438\u0444 #" + (numberGrief = (int)this.griefSelection.getValue())) || !this.canClick()) continue;
                InventoryTask.clickSlot(i, 0, SlotActionType.PICKUP, false);
                this.lastActionTime = System.currentTimeMillis();
                continue;
            }
            if (!this.serverSelection.isSelected("\u0421\u043f\u0443\u043a\u0438\u0422\u0430\u0439\u043c \u0434\u0443\u044d\u043b\u0438") || !(title = container.getTitle().getString().toLowerCase()).contains("\u0432\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0440\u0435\u0436\u0438\u043c") || !this.canClick()) continue;
            InventoryTask.clickSlot(14, 0, SlotActionType.QUICK_MOVE, false);
            this.lastActionTime = System.currentTimeMillis();
        }
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        DisconnectS2CPacket packet;
        String message;
        if (!this.state || event.getType() != PacketEvent.Type.RECEIVE) {
            return;
        }
        Packet<?> packet2 = event.getPacket();
        if (packet2 instanceof DisconnectS2CPacket && ((message = (packet = (DisconnectS2CPacket)packet2).reason().getString().toLowerCase()).contains("\u043a \u0441\u043e\u0436\u0430\u043b\u0435\u043d\u0438\u044e \u0441\u0435\u0440\u0432\u0435\u0440 \u043f\u0435\u0440\u0435\u043f\u043e\u043b\u043d\u0435\u043d") || message.contains("\u043f\u043e\u0434\u043e\u0436\u0434\u0438\u0442\u0435 20 \u0441\u0435\u043a\u0443\u043d\u0434!") || message.contains("\u0432\u044b \u0443\u0436\u0435 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u044b \u043d\u0430 \u044d\u0442\u043e\u0442 \u0441\u0435\u0440\u0432\u0435\u0440!") || message.contains("\u043f\u043e\u0434\u043e\u0436\u0434\u0438\u0442\u0435 \u043d\u0435\u0441\u043a\u043e\u043b\u044c\u043a\u043e \u0441\u0435\u043a\u0443\u043d\u0434 \u043f\u0435\u0440\u0435\u0434 \u043f\u043e\u0432\u0442\u043e\u0440\u043d\u044b\u043c \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435\u043c!") || message.contains("\u0432\u044b \u0431\u044b\u043b\u0438 \u043a\u0438\u043a\u043d\u0443\u0442\u044b \u0441 \u0441\u0435\u0440\u0432\u0435\u0440\u0430 1duels:") || message.contains("\u0432\u044b \u0431\u044b\u043b\u0438 \u043a\u0438\u043a\u043d\u0443\u0442\u044b") || message.contains("\u0431\u043e\u043b\u044c\u0448\u043e\u0439 \u043f\u043e\u0442\u043e\u043a \u0438\u0433\u0440\u043e\u043a\u043e\u0432") || message.contains("\u0441\u0435\u0440\u0432\u0435\u0440 \u0437\u0430\u043f\u043e\u043b\u043d\u0435\u043d!"))) {
            if (this.serverSelection.isSelected("\u0421\u043f\u0443\u043a\u0438\u0422\u0430\u0439\u043c \u0434\u0443\u044d\u043b\u0438")) {
                this.useSpookyCompassOrDisable();
            } else {
                InventoryTask.selectCompass();
                ServerJoiner.mc.player.networkHandler.sendPacket((Packet)new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, ServerJoiner.mc.player.getInventory().selectedSlot, ServerJoiner.mc.player.getYaw(), ServerJoiner.mc.player.getPitch()));
            }
        }
    }

    @Override
    public void activate() {
        super.activate();
        this.isToggling = false;
        this.retryDuels = false;
        this.lastActionTime = 0L;
        if (this.serverSelection.isSelected("\u0421\u043f\u0443\u043a\u0438\u0422\u0430\u0439\u043c \u0434\u0443\u044d\u043b\u0438")) {
            this.useSpookyCompassOrDisable();
        } else if (ServerJoiner.mc.player != null && ServerJoiner.mc.player.networkHandler != null) {
            InventoryTask.selectCompass();
            ServerJoiner.mc.player.networkHandler.sendPacket((Packet)new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, ServerJoiner.mc.player.getInventory().selectedSlot, ServerJoiner.mc.player.getYaw(), ServerJoiner.mc.player.getPitch()));
        }
    }

    @Override
    public void deactivate() {
        super.deactivate();
        this.lastActionTime = 0L;
        this.isToggling = false;
        this.retryDuels = false;
    }

    private boolean canClick() {
        return System.currentTimeMillis() - this.lastActionTime > 50L;
    }

    private void useSpookyCompassOrDisable() {
        if (ServerJoiner.mc.player == null || ServerJoiner.mc.player.networkHandler == null) {
            return;
        }
        Slot compassSlot = this.findSpookyCompassSlot();
        if (compassSlot == null) {
            Notifications.getInstance().addList("\u041a\u043e\u043c\u043f\u0430\u0441 \u0441 \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u0435\u043c " + this.spookyCompassName.getText() + " \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d", 2500);
            this.setState(false);
            return;
        }
        InventoryTask.swapHand(compassSlot, Hand.MAIN_HAND, false, true);
        ServerJoiner.mc.player.networkHandler.sendPacket((Packet)new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, ServerJoiner.mc.player.getInventory().selectedSlot, ServerJoiner.mc.player.getYaw(), ServerJoiner.mc.player.getPitch()));
        this.lastActionTime = System.currentTimeMillis();
    }

    private Slot findSpookyCompassSlot() {
        String searchName = ServerJoiner.normalize(this.spookyCompassName.getText());
        return InventoryTask.getSlot(slot -> {
            if (!slot.getStack().isOf(Items.COMPASS)) {
                return false;
            }
            if (searchName.isEmpty()) {
                return true;
            }
            return ServerJoiner.normalize(InventoryTask.getCleanName(slot.getStack().getName())).contains(searchName);
        });
    }

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase().trim();
    }
}
