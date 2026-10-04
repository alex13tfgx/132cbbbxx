/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.features.impl.misc;

import code.wiosvisual.emotions.EmotionWheelManager;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BindSetting;
import code.wiosvisual.features.module.setting.implement.ButtonSetting;
import code.wiosvisual.utils.client.Instance;

public class Globals
extends Module {
    public static final boolean ITEM_REPLACER_SYNC_ENABLED = true;
    private final BindSetting emotionWheelBind = new BindSetting("\u041a\u043e\u043b\u0435\u0441\u043e \u044d\u043c\u043e\u0446\u0438\u0439", "\u041a\u043b\u0430\u0432\u0438\u0448\u0430 \u043e\u0442\u043a\u0440\u044b\u0442\u0438\u044f \u043a\u043e\u0435\u0441\u0430 \u044d\u043c\u043e\u0446\u0438\u0439").setKey(86);
    private final ButtonSetting openEmotionWheelButton = new ButtonSetting("\u041e\u0442\u043a\u0440\u044b\u0442\u044c \u043a\u043e\u043b\u0435\u0441\u043e \u044d\u043c\u043e\u0446\u0438\u0439", "\u041e\u0442\u043a\u0440\u044b\u0432\u0430\u0435\u0442 \u044d\u043a\u0440\u0430\u043d \u0432\u044b\u0431\u043e\u0440\u0430 \u044d\u043c\u043e\u0446\u0438\u0439").setButtonName("Open").setRunnable(() -> {
        EmotionWheelManager manager = EmotionWheelManager.getInstance();
        if (manager != null) {
            manager.openFromGlobals();
        }
    });

    public static Globals getInstance() {
        return Instance.get(Globals.class);
    }

    public Globals() {
        super("Globals", "Globals", ModuleCategory.MISC);
        this.setup(this.emotionWheelBind, this.openEmotionWheelButton);
        this.setState(true);
    }

    public static void enforceEnabled() {
        Globals globals = Globals.getInstance();
        if (globals != null && !globals.isState()) {
            globals.setState(true);
        }
    }

    @Override
    public void activate() {
        if (!this.isState()) {
            this.setState(true);
        }
    }

    @Override
    public void deactivate() {
        this.setState(true);
    }

    public BindSetting getEmotionWheelBind() {
        return this.emotionWheelBind;
    }

    public static boolean isItemReplacerSyncEnabled() {
        return true;
    }
}
