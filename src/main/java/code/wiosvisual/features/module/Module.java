/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_310
 *  org.apache.logging.log4j.LogManager
 */
package code.wiosvisual.features.module;

import code.wiosvisual.utils.client.server.ServerRestrictions;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.common.animation.Animation;
import code.wiosvisual.common.animation.Direction;
import code.wiosvisual.common.animation.implement.Decelerate;
import code.wiosvisual.display.hud.DynamicIsland;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.SettingRepository;
import code.wiosvisual.features.module.setting.implement.BindSetting;
import code.wiosvisual.utils.client.managers.event.EventManager;
import code.wiosvisual.features.impl.misc.Sounds;
import code.wiosvisual.utils.client.sound.SoundManager;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;

public class Module
extends SettingRepository
implements QuickImports {
    private final String name;
    private final String visibleName;
    private final ModuleCategory category;
    private final Animation animation = new Decelerate().setMs(175).setValue(1.0);
    private int key = -1;
    private int type = 1;
    private int secondKey = -1;
    public boolean state;
    private boolean hidden;
    private boolean quiet;

    public Module(String name, ModuleCategory category) {
        this.name = name;
        this.category = category;
        this.visibleName = name;
    }

    public Module(String name, String visibleName, ModuleCategory category) {
        this.name = name;
        this.visibleName = visibleName;
        this.category = category;
    }

    public BindSetting getBind() {
        BindSetting bind = new BindSetting("\u0411\u0438\u043d\u0434", "\u041a\u043b\u0430\u0432\u0438\u0448\u0430 \u0430\u043a\u0442\u0438\u0432\u0430\u0446\u0438\u0438");
        bind.setKey(this.key);
        bind.setSecondKey(this.secondKey);
        return bind;
    }

    public void setBindFromSetting(BindSetting bind) {
        if (bind != null) {
            this.key = bind.getKey();
            this.secondKey = bind.getSecondKey();
        }
    }

    public void switchState() {
        this.setState(!this.state);
    }

    public void setState(boolean state) {
        // Запрещённый на текущем сервере модуль не включается ни кнопкой, ни биндом, ни из конфига.
        if (state && !this.state && ServerRestrictions.isForbiddenHere(this)) {
            return;
        }
        this.animation.setDirection(state ? Direction.FORWARDS : Direction.BACKWARDS);
        if (state != this.state) {
            this.state = state;
            this.handleStateChange();
        }
    }

    /** Переключение без уведомления и звука — для автоматических переключений (ограничения сервера). */
    public void setStateQuietly(boolean state) {
        this.quiet = true;
        try {
            this.setState(state);
        } finally {
            this.quiet = false;
        }
    }

    private void handleStateChange() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            Hud hud = Hud.getInstance();
            if (hud == null || this.quiet || ServerRestrictions.isForbiddenHere(this)) {
                if (this.state) {
                    this.activate();
                } else {
                    this.deactivate();
                }
                this.toggleSilent(this.state);
                return;
            }
            if (mc.player != null && mc.world != null) {
                String cleanName;
                String string = cleanName = Formatting.strip((String)this.visibleName) == null ? this.visibleName : Formatting.strip((String)this.visibleName);
                if (this.state) {
                    DynamicIsland.notifyModuleToggle(cleanName, true);
                    SoundManager.playSound(SoundManager.ENABLE_MODULE);
                    Module.playToggleSound(this, true);
                    this.activate();
                } else {
                    DynamicIsland.notifyModuleToggle(cleanName, false);
                    SoundManager.playSound(SoundManager.DISABLE_MODULE);
                    Module.playToggleSound(this, false);
                    this.deactivate();
                }
            }
            this.toggleSilent(this.state);
        }
        catch (Throwable error) {
            this.state = false;
            LogManager.getLogger((String)"Wiosvisual").error("Failed to change state for module {}", (Object)this.name, (Object)error);
        }
    }

    private void toggleSilent(boolean activate) {
        EventManager eventManager = Wiosvisual.getInstance().getEventManager();
        if (eventManager == null) {
            return;
        }
        try {
            if (activate) {
                if (!EventManager.canInspectEventHandlers(this)) {
                    this.state = false;
                    return;
                }
                EventManager.register(this);
            } else {
                eventManager.unregister(this);
            }
        }
        catch (Throwable error) {
            this.state = false;
            LogManager.getLogger((String)"Wiosvisual").error("Failed to {} event handlers for module {}", (Object)(activate ? "register" : "unregister"), (Object)this.name, (Object)error);
        }
    }

    /**
     * Свой набор звуков модуля Sounds. Сам Sounds озвучивает своё включение в
     * activate(), поэтому здесь он пропускается — иначе звук был бы двойным.
     */
    private static void playToggleSound(Module module, boolean enabled) {
        try {
            Sounds sounds = Sounds.getInstance();
            if (sounds != null && module != sounds) {
                sounds.onModuleToggle(enabled);
            }
        }
        catch (Throwable ignored) {
            // Модуль звуков не обязан существовать — молчим.
        }
    }

    public void activate() {
    }

    public void deactivate() {
    }

    public String getName() {
        return this.name;
    }

    public String getVisibleName() {
        return this.visibleName;
    }

    public ModuleCategory getCategory() {
        return this.category;
    }

    public Animation getAnimation() {
        return this.animation;
    }

    public int getKey() {
        return this.key;
    }

    public int getType() {
        return this.type;
    }

    public int getSecondKey() {
        return this.secondKey;
    }

    public boolean isState() {
        return this.state;
    }

    public boolean isHidden() {
        return this.hidden || ServerRestrictions.isForbiddenHere(this);
    }

    public void setKey(int key) {
        this.key = key;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setSecondKey(int secondKey) {
        this.secondKey = secondKey;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
