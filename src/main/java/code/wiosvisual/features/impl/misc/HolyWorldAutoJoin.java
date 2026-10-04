/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_1268
 *  net.minecraft.class_1707
 *  net.minecraft.class_1713
 *  net.minecraft.class_1735
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_2371
 *  net.minecraft.class_2561
 *  net.minecraft.class_2596
 *  net.minecraft.class_2886
 *  net.minecraft.class_437
 *  net.minecraft.class_476
 *  net.minecraft.class_9290
 *  net.minecraft.class_9334
 *  org.apache.commons.lang3.StringUtils
 */
package code.wiosvisual.features.impl.misc;

import code.wiosvisual.display.hud.Notifications;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.packet.network.Network;
import code.wiosvisual.utils.client.sound.SoundManager;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.interactions.inv.InventoryTask;
import code.wiosvisual.utils.math.time.StopWatch;
import java.util.Iterator;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import org.apache.commons.lang3.StringUtils;

public class HolyWorldAutoJoin
implements QuickImports {
    private int targetAnarchy = -1;
    private boolean isJoining = false;
    private StopWatch hubDelayWatch = new StopWatch();
    private StopWatch actionDelayWatch = new StopWatch();
    private int currentStep = 0;
    private boolean completed = false;
    private boolean active = false;

    @EventHandler
    public void onTick(TickEvent event) {
        if (!this.active) {
            if (this.isJoining || this.completed) {
                this.reset();
            }
            return;
        }
        if (!Network.isHolyWorld()) {
            if (this.isJoining || this.completed) {
                this.reset();
            }
            return;
        }
        if (this.completed) {
            return;
        }
        if (this.currentStep == 0 && !this.isJoining) {
            if (this.targetAnarchy <= 0) {
                int anarchy = Network.getAnarchy();
                if (anarchy > 0) {
                    this.targetAnarchy = anarchy;
                } else {
                    return;
                }
            }
            this.isJoining = true;
            this.currentStep = 1;
            this.hubDelayWatch.reset();
            HolyWorldAutoJoin.mc.player.networkHandler.sendChatCommand("hub");
            return;
        }
        if (this.currentStep == 1) {
            if (this.hubDelayWatch.elapsedTime() >= 50L) {
                InventoryTask.selectCompass();
                this.actionDelayWatch.reset();
                this.currentStep = 2;
            }
            return;
        }
        if (this.currentStep == 2) {
            ItemStack mainHandStack = HolyWorldAutoJoin.mc.player.getMainHandStack();
            if (mainHandStack.getItem() == Items.COMPASS) {
                if (this.actionDelayWatch.elapsedTime() >= 50L) {
                    HolyWorldAutoJoin.mc.player.networkHandler.sendPacket((Packet)new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, HolyWorldAutoJoin.mc.player.getInventory().selectedSlot, HolyWorldAutoJoin.mc.player.getYaw(), HolyWorldAutoJoin.mc.player.getPitch()));
                    this.actionDelayWatch.reset();
                    this.currentStep = 3;
                }
            } else if (this.actionDelayWatch.elapsedTime() >= 50L) {
                InventoryTask.selectCompass();
                this.actionDelayWatch.reset();
            }
            return;
        }
        if (this.currentStep == 3) {
            if (HolyWorldAutoJoin.mc.currentScreen instanceof GenericContainerScreen) {
                if (this.actionDelayWatch.elapsedTime() >= 250L) {
                    this.currentStep = 4;
                    this.actionDelayWatch.reset();
                }
            } else if (this.actionDelayWatch.elapsedTime() >= 250L) {
                ItemStack mainHandStack = HolyWorldAutoJoin.mc.player.getMainHandStack();
                if (mainHandStack.getItem() == Items.COMPASS) {
                    HolyWorldAutoJoin.mc.player.networkHandler.sendPacket((Packet)new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, HolyWorldAutoJoin.mc.player.getInventory().selectedSlot, HolyWorldAutoJoin.mc.player.getYaw(), HolyWorldAutoJoin.mc.player.getPitch()));
                    this.actionDelayWatch.reset();
                } else {
                    InventoryTask.selectCompass();
                    this.currentStep = 2;
                    this.actionDelayWatch.reset();
                }
            }
            return;
        }
        if (this.currentStep == 4) {
            if (HolyWorldAutoJoin.mc.currentScreen instanceof GenericContainerScreen) {
                if (this.actionDelayWatch.elapsedTime() >= 500L) {
                    InventoryTask.clickSlot(12, 0, SlotActionType.PICKUP, true);
                    this.currentStep = 5;
                    this.actionDelayWatch.reset();
                }
            } else {
                this.reset();
            }
            return;
        }
        if (this.currentStep == 5) {
            if (HolyWorldAutoJoin.mc.currentScreen instanceof GenericContainerScreen) {
                if (this.actionDelayWatch.elapsedTime() >= 500L) {
                    int categorySlot = -1;
                    if (this.targetAnarchy <= 16) {
                        categorySlot = 0;
                    } else if (this.targetAnarchy <= 37) {
                        categorySlot = 1;
                    } else if (this.targetAnarchy <= 53) {
                        categorySlot = 2;
                    } else if (this.targetAnarchy >= 54) {
                        categorySlot = 3;
                    }
                    if (categorySlot != -1) {
                        InventoryTask.clickSlot(categorySlot, 0, SlotActionType.PICKUP, true);
                        this.currentStep = 6;
                        this.actionDelayWatch.reset();
                    }
                }
            } else {
                this.reset();
            }
            return;
        }
        if (this.currentStep == 6) {
            Screen screen2 = HolyWorldAutoJoin.mc.currentScreen;
            if (screen2 instanceof GenericContainerScreen) {
                GenericContainerScreen screen = (GenericContainerScreen)screen2;
                if (this.actionDelayWatch.elapsedTime() >= 500L) {
                    boolean found = false;
                    DefaultedList slots = ((GenericContainerScreenHandler)screen.getScreenHandler()).slots;
                    String title = screen.getTitle().getString();
                    boolean isLightAnarchy = title != null && (title.toLowerCase().contains("\u043b\u0430\u0439\u0442") || title.contains("1") && title.contains("64"));
                    for (int i = 0; i < slots.size(); ++i) {
                        String name;
                        LoreComponent lore;
                        Slot slot = (Slot)slots.get(i);
                        ItemStack stack = slot.getStack();
                        if (stack.isEmpty()) continue;
                        Integer anarchyNumber = null;
                        if (stack.getItem() == Items.PLAYER_HEAD && (lore = (LoreComponent)stack.get(DataComponentTypes.LORE)) != null && !lore.lines().isEmpty()) {
                            for (Text line : lore.lines()) {
                                String numberAfterHash;
                                String loreText = line.getString();
                                if (!loreText.contains("#") || (numberAfterHash = StringUtils.substringAfter((String)loreText, (String)"#")) == null || numberAfterHash.isEmpty()) continue;
                                anarchyNumber = HolyWorldAutoJoin.parseLeadingInt(numberAfterHash.trim());
                                break;
                            }
                        }
                        if (anarchyNumber == null && (isLightAnarchy || this.targetAnarchy >= 1 && this.targetAnarchy <= 64) && (anarchyNumber = HolyWorldAutoJoin.parseLeadingInt(name = stack.getName().getString().replaceAll("\u00a7[0-9a-fk-or]", "").replaceAll("\u00a7.", "").trim())) == null && stack.get(DataComponentTypes.LORE) != null) {
                            Text line;
                            Iterator iterator = ((LoreComponent)stack.get(DataComponentTypes.LORE)).lines().iterator();
                            while (iterator.hasNext() && (anarchyNumber = HolyWorldAutoJoin.parseLeadingInt((line = (Text)iterator.next()).getString().replaceAll("\u00a7[0-9a-fk-or]", "").trim())) == null) {
                            }
                        }
                        if (anarchyNumber == null || anarchyNumber != this.targetAnarchy) continue;
                        InventoryTask.clickSlot(slot.id, 0, SlotActionType.PICKUP, false);
                        found = true;
                        HolyWorldAutoJoin.mc.player.closeHandledScreen();
                        SoundManager.playSound(SoundManager.ENABLE_MODULE);
                        Notifications.getInstance().addList("\u041f\u0435\u0440\u0435\u0437\u0430\u0445\u043e\u0434 \u043d\u0430 \u0430\u043d\u0430\u0440\u0445\u0438\u044e #" + String.valueOf(Formatting.BLUE) + anarchyNumber + String.valueOf(Formatting.RESET) + " \u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d", 3000);
                        this.reset();
                        this.active = false;
                        break;
                    }
                    if (!found && this.actionDelayWatch.elapsedTime() >= 1000L) {
                        this.reset();
                    }
                }
            } else if (!this.completed) {
                this.reset();
            }
            return;
        }
    }

    private static Integer parseLeadingInt(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        StringBuilder num = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (!Character.isDigit(c)) break;
            num.append(c);
        }
        if (num.length() == 0) {
            return null;
        }
        try {
            return Integer.parseInt(num.toString());
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    public void reset() {
        this.targetAnarchy = -1;
        this.isJoining = false;
        this.currentStep = 0;
        this.completed = false;
        this.hubDelayWatch.reset();
        this.actionDelayWatch.reset();
    }

    public void setTargetAnarchy(int anarchy) {
        this.targetAnarchy = anarchy;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            this.reset();
        }
    }

    public boolean isActive() {
        return this.active;
    }
}
