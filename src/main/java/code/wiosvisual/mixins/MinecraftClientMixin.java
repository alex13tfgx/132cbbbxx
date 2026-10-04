/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1268
 *  net.minecraft.class_1269
 *  net.minecraft.class_1269$class_9860
 *  net.minecraft.class_1269$class_9861
 *  net.minecraft.class_156
 *  net.minecraft.class_1657
 *  net.minecraft.class_239
 *  net.minecraft.class_239$class_240
 *  net.minecraft.class_310
 *  net.minecraft.class_3966
 *  net.minecraft.class_437
 *  net.minecraft.class_542
 *  net.minecraft.class_634
 *  net.minecraft.class_636
 *  net.minecraft.class_746
 *  net.minecraft.class_757
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package code.wiosvisual.mixins;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.display.screens.ias.IASScreen;
import code.wiosvisual.events.container.SetScreenEvent;
import code.wiosvisual.events.player.GameUpdateEvent;
import code.wiosvisual.events.player.HotBarUpdateEvent;
import code.wiosvisual.utils.client.FakePlayerManager;
import code.wiosvisual.utils.client.logs.Logger;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import code.wiosvisual.utils.client.managers.file.exception.FileProcessingException;
import code.wiosvisual.utils.client.window.WindowStyle;
import code.wiosvisual.utils.client.window.WindowTitleAnimation;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.font.Fonts;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(value=EnvType.CLIENT)
@Mixin(value={MinecraftClient.class})
public abstract class MinecraftClientMixin
implements QuickImports {
    @Shadow
    @Nullable
    public ClientPlayerInteractionManager interactionManager;
    @Shadow
    @Nullable
    public ClientPlayerEntity player;
    @Shadow
    @Final
    public GameRenderer gameRenderer;
    @Shadow
    @Nullable
    public Screen currentScreen;
    @Shadow
    @Nullable
    public HitResult crosshairTarget;
    private WindowTitleAnimation titleUtil;
    @Unique
    private long gameUpdateLastHookTime;
    @Unique
    private int gameUpdateAccumulatedCalls;
    private int titleUpdateCounter = 0;
    @Unique
    private boolean wiosvisual$glintFixApplied;
    @Unique
    private boolean wiosvisual$accountRestored;

    @Shadow
    @Nullable
    public abstract ClientPlayNetworkHandler getNetworkHandler();

    /*
     * 1.21.11: RealmsClient при создании асинхронно шлёт запрос, который трогает RealmsMainScreen
     * (наследник Screen). Если Screen ещё не инициализирован, его <clinit> (панорама → GPU-буфер)
     * выполняется в потоке "Download-N" и падает "Rendersystem called from wrong thread".
     * Инициализируем Screen заранее — в потоке рендера, когда GPU-устройство уже создано.
     */
    @Inject(method={"<init>"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/realms/RealmsClient;createRealmsClient(Lnet/minecraft/client/MinecraftClient;)Lnet/minecraft/client/realms/RealmsClient;")})
    private void wiosvisual$initScreenOnRenderThread(RunArgs args, CallbackInfo ci) {
        try {
            Class.forName("net.minecraft.class_437", true, Screen.class.getClassLoader());
        } catch (ClassNotFoundException ignored) {
        }
    }

    @Inject(at={@At(value="TAIL")}, method={"<init>"})
    private void onInit(RunArgs args, CallbackInfo ci) {
        if (System.getProperty("java.version").equals("sk3dguard-production-vm")) {
            System.setProperty("sun.font.layout.ffm", "false");
        }
        Fonts.init();
        try {
            this.titleUtil = WindowTitleAnimation.getInstance();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getWindow() != null) {
                client.getWindow().setTitle(this.titleUtil.getCurrentTitle());
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Inject(at={@At(value="HEAD")}, method={"stop"})
    private void stop(CallbackInfo ci) {
        block8: {
            Logger.info("Stopping for MinecraftClient");
            try {
                Wiosvisual wiosvisual = Wiosvisual.getInstance();
                if (wiosvisual == null || !wiosvisual.isInitialized()) break block8;
                try {
                    wiosvisual.getFileController().saveFiles();
                }
                catch (FileProcessingException e) {
                    Logger.error("Error occurred while saving files: " + e.getMessage() + " " + String.valueOf(e.getCause()));
                }
                finally {
                    if (wiosvisual.getFileController() != null) {
                        wiosvisual.getFileController().stopAutoSave();
                    }
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    @Inject(method={"doAttack"}, at={@At(value="HEAD")}, cancellable=true)
    private void doAttackHook(CallbackInfoReturnable<Boolean> cir) {
        if (this.player == null || this.crosshairTarget == null || this.crosshairTarget.getType() != HitResult.Type.ENTITY) {
            return;
        }
        EntityHitResult entityHitResult = (EntityHitResult)(Object)this.crosshairTarget;
        if (FakePlayerManager.handleAttack(entityHitResult.getEntity())) {
            this.player.swingHand(Hand.MAIN_HAND);
            cir.setReturnValue(true);
        }
    }

    @Inject(method={"setScreen"}, at={@At(value="HEAD")}, cancellable=true)
    public void setScreenHook(Screen screen, CallbackInfo ci) {
        try {
            Screen eventScreen;
            SetScreenEvent event = SafeEvents.dispatch(() -> new SetScreenEvent(screen));
            Wiosvisual wiosvisual = Wiosvisual.getInstance();
            if (wiosvisual != null && wiosvisual.getDraggableRepository() != null && event != null) {
                wiosvisual.getDraggableRepository().draggable().forEach(drag -> drag.setScreen(event));
            }
            Screen screen2 = eventScreen = event != null ? event.getScreen() : screen;
            if (screen != eventScreen) {
                mc.setScreen(eventScreen);
                ci.cancel();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    @Inject(method={"onResolutionChanged"}, at={@At(value="TAIL")})
    private void applyDarkMode(CallbackInfo ci) {
        Fonts.clearCache();
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("linux")) {
                return;
            }
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getWindow() != null) {
                WindowStyle.setDarkMode(client.getWindow().getHandle());
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    @Inject(method={"tick"}, at={@At(value="HEAD")})
    private void onTick(CallbackInfo ci) {
        this.wiosvisual$restoreGlintStrength();
        this.wiosvisual$restoreActiveAccount();
        try {
            if (this.titleUtil == null) {
                this.titleUtil = WindowTitleAnimation.getInstance();
            }
            this.titleUtil.updateTitle();
            if (++this.titleUpdateCounter >= 10) {
                this.titleUpdateCounter = 0;
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.getWindow() != null) {
                    client.getWindow().setTitle(this.titleUtil.getCurrentTitle());
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    @Unique
    private void wiosvisual$restoreGlintStrength() {
        if (this.wiosvisual$glintFixApplied) {
            return;
        }
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return;
            }
            this.wiosvisual$glintFixApplied = true;
            boolean changed = false;
            if ((Double)client.options.getGlintStrength().getValue() <= 0.0) {
                client.options.getGlintStrength().setValue(0.75);
                changed = true;
            }
            if ((Double)client.options.getGlintSpeed().getValue() <= 0.0) {
                client.options.getGlintSpeed().setValue(0.5);
                changed = true;
            }
            if (changed) {
                client.options.write();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    @Unique
    private void wiosvisual$restoreActiveAccount() {
        if (this.wiosvisual$accountRestored) {
            return;
        }
        try {
            Wiosvisual wiosvisual = Wiosvisual.getInstance();
            if (wiosvisual == null || !wiosvisual.isInitialized() || wiosvisual.getAccountRepository() == null) {
                return;
            }
            this.wiosvisual$accountRestored = true;
            IASScreen.restoreActiveAccount();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    @Inject(method={"updateWindowTitle"}, at={@At(value="HEAD")}, cancellable=true)
    private void onUpdateWindowTitle(CallbackInfo ci) {
        try {
            MinecraftClient client;
            if (this.titleUtil == null) {
                this.titleUtil = WindowTitleAnimation.getInstance();
            }
            if ((client = MinecraftClient.getInstance()) != null && client.getWindow() != null) {
                client.getWindow().setTitle(this.titleUtil.getCurrentTitle());
            }
            ci.cancel();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    @Inject(method={"handleInputEvents"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;")}, cancellable=true)
    public void handleInputEventsHook(CallbackInfo ci) {
        HotBarUpdateEvent event = SafeEvents.dispatch(() -> new HotBarUpdateEvent());
        if (event != null && event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method={"render"}, at={@At(value="HEAD")})
    private void onRenderGameUpdate(boolean tick, CallbackInfo ci) {
        long now = Util.getMeasuringTimeNano();
        long delta = now - this.gameUpdateLastHookTime;
        this.gameUpdateAccumulatedCalls += (int)(delta / 4166666L);
        this.gameUpdateLastHookTime += (long)this.gameUpdateAccumulatedCalls * 4166666L;
        this.gameUpdateAccumulatedCalls = Math.min(this.gameUpdateAccumulatedCalls, 240);
        while (this.gameUpdateAccumulatedCalls > 0) {
            SafeEvents.dispatch(GameUpdateEvent::new);
            --this.gameUpdateAccumulatedCalls;
        }
    }
}
