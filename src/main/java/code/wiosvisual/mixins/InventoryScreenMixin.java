/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1657
 *  net.minecraft.class_1661
 *  net.minecraft.class_1703
 *  net.minecraft.class_1713
 *  net.minecraft.class_1723
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_465
 *  net.minecraft.class_490
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import code.wiosvisual.utils.interactions.inv.InventoryFlowManager;
import code.wiosvisual.display.widgets.ClearButtonWidget;
import code.wiosvisual.mixins.IScreen;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={InventoryScreen.class})
public abstract class InventoryScreenMixin
extends HandledScreen<PlayerScreenHandler> {
    @Unique
    private ClearButtonWidget deleteButton;
    @Unique
    private static volatile boolean deleting = false;
    @Unique
    private static final ScheduledExecutorService clearExecutor = Executors.newSingleThreadScheduledExecutor();

    @Unique
    public ClearButtonWidget getDeleteButton() {
        return this.deleteButton;
    }

    public InventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method={"init"}, at={@At(value="TAIL")})
    private void onInit(CallbackInfo ci) {
        if (!"SpookyTime".equals(InventoryFlowManager.INVENTORY_MODE)) {
            return;
        }
        InventoryScreen screen = (InventoryScreen)(Object)this;
        this.deleteButton = new ClearButtonWidget(0, 0, 12, 12, () -> this.deleteAllItems());
        IScreen iScreen = (IScreen)screen;
        iScreen.getDrawables().add(this.deleteButton);
        iScreen.getSelectables().add(this.deleteButton);
        iScreen.getChildren().add(this.deleteButton);
    }

    @Inject(method={"render"}, at={@At(value="TAIL")})
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.deleteButton != null && "SpookyTime".equals(InventoryFlowManager.INVENTORY_MODE)) {
            InventoryScreen screen;
            InventoryScreen screenBase = screen = (InventoryScreen)(Object)this;
            int guiLeft = (screenBase.width - this.backgroundWidth) / 2;
            int guiTop = (screenBase.height - 166) / 2;
            int buttonX = guiLeft + this.backgroundWidth - 12 - 5;
            int buttonY = guiTop + 5;
            this.deleteButton.setPosition(buttonX, buttonY);
        } else if (this.deleteButton != null && !"SpookyTime".equals(InventoryFlowManager.INVENTORY_MODE)) {
            InventoryScreen screen = (InventoryScreen)(Object)this;
            IScreen iScreen = (IScreen)screen;
            iScreen.getDrawables().remove(this.deleteButton);
            iScreen.getSelectables().remove(this.deleteButton);
            iScreen.getChildren().remove(this.deleteButton);
            this.deleteButton = null;
        }
    }

    private void deleteAllItems() {
        if (deleting) {
            return;
        }
        deleting = true;
        for (int i = 0; i < 36; ++i) {
            int slot = i;
            clearExecutor.schedule(() -> {
                ItemStack stack;
                if (QuickImports.mc.player != null && QuickImports.mc.interactionManager != null && QuickImports.mc.player.currentScreenHandler != null && (stack = QuickImports.mc.player.getInventory().getStack(slot)).getItem() != Items.AIR) {
                    int containerSlot = slot < 9 ? slot + 36 : slot;
                    QuickImports.mc.interactionManager.clickSlot(QuickImports.mc.player.currentScreenHandler.syncId, containerSlot, 45, SlotActionType.SWAP, (PlayerEntity)QuickImports.mc.player);
                }
            }, (long)i * 80L, TimeUnit.MILLISECONDS);
        }
        clearExecutor.schedule(() -> {
            ItemStack stack;
            if (QuickImports.mc.player != null && QuickImports.mc.interactionManager != null && QuickImports.mc.player.currentScreenHandler != null && (stack = QuickImports.mc.player.getInventory().getStack(40)).getItem() != Items.AIR) {
                QuickImports.mc.interactionManager.clickSlot(QuickImports.mc.player.currentScreenHandler.syncId, 45, 45, SlotActionType.SWAP, (PlayerEntity)QuickImports.mc.player);
            }
        }, 2880L, TimeUnit.MILLISECONDS);
        clearExecutor.schedule(() -> {
            ItemStack stack;
            if (QuickImports.mc.player != null && QuickImports.mc.interactionManager != null && QuickImports.mc.player.currentScreenHandler != null && (stack = QuickImports.mc.player.getInventory().getStack(39)).getItem() != Items.AIR) {
                QuickImports.mc.interactionManager.clickSlot(QuickImports.mc.player.currentScreenHandler.syncId, 5, 45, SlotActionType.SWAP, (PlayerEntity)QuickImports.mc.player);
            }
        }, 2960L, TimeUnit.MILLISECONDS);
        clearExecutor.schedule(() -> {
            ItemStack stack;
            if (QuickImports.mc.player != null && QuickImports.mc.interactionManager != null && QuickImports.mc.player.currentScreenHandler != null && (stack = QuickImports.mc.player.getInventory().getStack(38)).getItem() != Items.AIR) {
                QuickImports.mc.interactionManager.clickSlot(QuickImports.mc.player.currentScreenHandler.syncId, 6, 45, SlotActionType.SWAP, (PlayerEntity)QuickImports.mc.player);
            }
        }, 3040L, TimeUnit.MILLISECONDS);
        clearExecutor.schedule(() -> {
            ItemStack stack;
            if (QuickImports.mc.player != null && QuickImports.mc.interactionManager != null && QuickImports.mc.player.currentScreenHandler != null && (stack = QuickImports.mc.player.getInventory().getStack(37)).getItem() != Items.AIR) {
                QuickImports.mc.interactionManager.clickSlot(QuickImports.mc.player.currentScreenHandler.syncId, 7, 45, SlotActionType.SWAP, (PlayerEntity)QuickImports.mc.player);
            }
        }, 3120L, TimeUnit.MILLISECONDS);
        clearExecutor.schedule(() -> {
            ItemStack stack;
            if (QuickImports.mc.player != null && QuickImports.mc.interactionManager != null && QuickImports.mc.player.currentScreenHandler != null && (stack = QuickImports.mc.player.getInventory().getStack(36)).getItem() != Items.AIR) {
                QuickImports.mc.interactionManager.clickSlot(QuickImports.mc.player.currentScreenHandler.syncId, 8, 45, SlotActionType.SWAP, (PlayerEntity)QuickImports.mc.player);
            }
            deleting = false;
        }, 3200L, TimeUnit.MILLISECONDS);
    }
}
