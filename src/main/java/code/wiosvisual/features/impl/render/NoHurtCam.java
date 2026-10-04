package code.wiosvisual.features.impl.render;

import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.utils.client.Instance;

/**
 * Убирает наклон камеры при получении урона (vanilla GameRenderer#tiltViewWhenHurt)
 * и качание камеры при ходьбе (vanilla GameRenderer#bobView).
 * Сама отмена живёт в GameRendererMixin — здесь только состояние модуля.
 */
public class NoHurtCam
extends Module {
    public final BooleanSetting tiltSetting = new BooleanSetting("Наклон при уроне", "Убирает наклон камеры при получении урона").setValue(true);
    public final BooleanSetting shakeSetting = new BooleanSetting("Тряска камеры", "Убирает качание экрана при ходьбе").setValue(true);

    public static NoHurtCam getInstance() {
        return Instance.get(NoHurtCam.class);
    }

    public NoHurtCam() {
        super("NoHurtCam", "No Hurt Cam", ModuleCategory.RENDER);
        this.setup(this.tiltSetting, this.shakeSetting);
    }

    public static boolean shouldCancelTilt() {
        NoHurtCam module = NoHurtCam.getInstance();
        return module != null && module.isState() && module.tiltSetting.isValue();
    }

    public static boolean shouldCancelShake() {
        NoHurtCam module = NoHurtCam.getInstance();
        return module != null && module.isState() && module.shakeSetting.isValue();
    }
}
