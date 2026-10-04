/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_3675
 *  net.minecraft.class_3675$class_306
 */
package code.wiosvisual.utils.client.bind;

import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.setting.implement.BindSetting;
import code.wiosvisual.utils.client.bind.BindCodes;
import net.minecraft.client.util.InputUtil;

public final class BindCaptureHelper {
    private static Object target;

    public static boolean isCapturing() {
        return target != null;
    }

    public static boolean isCapturing(BindSetting setting) {
        return target == setting;
    }

    public static boolean isCapturing(Module module) {
        return target == module;
    }

    public static void start(BindSetting setting) {
        target = setting;
    }

    public static void start(Module module) {
        target = module;
    }

    public static void cancel() {
        target = null;
    }

    public static void toggle(BindSetting setting) {
        if (target == setting) {
            BindCaptureHelper.cancel();
        } else {
            BindCaptureHelper.start(setting);
        }
    }

    public static void toggle(Module module) {
        if (target == module) {
            BindCaptureHelper.cancel();
        } else {
            BindCaptureHelper.start(module);
        }
    }

    public static boolean handleKey(int keyCode, int scanCode) {
        if (target == null) {
            return false;
        }
        if (keyCode == 256) {
            BindCaptureHelper.applyKey(-1);
            BindCaptureHelper.cancel();
            return true;
        }
        int resolved = BindCaptureHelper.resolveKey(keyCode, scanCode);
        if (BindCodes.canBind(resolved)) {
            BindCaptureHelper.applyKey(resolved);
            BindCaptureHelper.cancel();
            return true;
        }
        return true;
    }

    public static boolean handleMouse(int button) {
        if (target == null) {
            return false;
        }
        if (BindCodes.canBind(button)) {
            BindCaptureHelper.applyKey(button);
            BindCaptureHelper.cancel();
        }
        return true;
    }

    public static boolean handleScroll(double amount) {
        if (target == null || amount == 0.0) {
            return false;
        }
        BindCaptureHelper.applyKey(amount > 0.0 ? 998 : 999);
        BindCaptureHelper.cancel();
        return true;
    }

    private static int resolveKey(int keyCode, int scanCode) {
        InputUtil.Key key;
        if (BindCodes.canBind(keyCode)) {
            return keyCode;
        }
        if (keyCode == -1 && scanCode > 0 && (key = InputUtil.fromKeyCode(new net.minecraft.client.input.KeyInput(keyCode, scanCode, 0))) != null && BindCodes.canBind(key.getCode())) {
            return key.getCode();
        }
        return -1;
    }

    private static void applyKey(int key) {
        Object object = target;
        if (object instanceof BindSetting) {
            BindSetting setting = (BindSetting)object;
            setting.setKey(key);
            setting.clearSecondKey();
        } else {
            object = target;
            if (object instanceof Module) {
                Module module = (Module)object;
                module.setKey(key);
                module.setSecondKey(-1);
            }
        }
    }

    private BindCaptureHelper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Object getTarget() {
        return target;
    }
}
