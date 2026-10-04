/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.features.impl.render;

import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BindSetting;

public class Emotions
extends Module {
    private final BindSetting wheelBind = new BindSetting("\u041e\u0442\u043a\u0440\u044b\u0442\u044c \u043a\u043e\u043b\u0435\u0441\u043e", "\u041a\u043b\u0430\u0432\u0438\u0448\u0430 \u043e\u0442\u043a\u0440\u044b\u0442\u0438\u044f \u043a\u043e\u043b\u0435\u0441\u0430 \u044d\u043c\u043e\u0446\u0438\u0439").setKey(86);

    public Emotions() {
        super("Emotions", "Emotions", ModuleCategory.RENDER);
        this.setup(this.wheelBind);
        this.setState(true);
    }

    public BindSetting getWheelBind() {
        return this.wheelBind;
    }
}
