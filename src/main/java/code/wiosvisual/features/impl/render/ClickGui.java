/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.features.impl.render;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.ModuleRepository;
import code.wiosvisual.features.module.setting.implement.BindSetting;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.RadioSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;

public class ClickGui
extends Module {
    private static ClickGui instance;
    public static final int OPEN_KEY = 344;
    private final RadioSetting guiType = new RadioSetting("\u0422\u0438\u043f GUI", "\u0412\u044b\u0431\u043e\u0440 \u0442\u0438\u043f\u0430 \u0438\u043d\u0442\u0435\u0440\u0444\u0435\u0439\u0441\u0430 ClickGUI", new String[]{"\u041a\u043b\u0430\u0441\u0441\u0438\u0447\u0435\u0441\u043a\u0438\u0439"}, "\u041a\u043b\u0430\u0441\u0441\u0438\u0447\u0435\u0441\u043a\u0438\u0439").visible(() -> false);
    private final BooleanSetting showCustomization = new BooleanSetting("\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c Customization", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0435\u0442 \u043f\u0430\u043d\u0435\u043b\u044c Customization \u0432 GUI").setValue(true).visible(() -> this.guiType.get().equals("\u041a\u043b\u0430\u0441\u0441\u0438\u0447\u0435\u0441\u043a\u0438\u0439"));
    private final BooleanSetting showModuleDescriptions = new BooleanSetting("\u041e\u043f\u0438\u0441\u0430\u043d\u0438\u0435 \u043c\u043e\u0434\u0443\u043b\u0435\u0439", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0435\u0442 \u043e\u043f\u0438\u0441\u0430\u043d\u0438\u0435 \u043c\u043e\u0434\u0443\u043b\u044f \u043f\u0440\u0438 \u043d\u0430\u0432\u0435\u0434\u0435\u043d\u0438\u0438 \u043a\u0443\u0440\u0441\u043e\u0440\u0430").setValue(true).visible(() -> this.guiType.get().equals("\u041a\u043b\u0430\u0441\u0441\u0438\u0447\u0435\u0441\u043a\u0438\u0439"));
    private final BooleanSetting backgroundBlur = new BooleanSetting("Blur Background", "\u041b\u0435\u0433\u043a\u043e\u0435 \u0440\u0430\u0437\u043c\u044b\u0442\u0438\u0435 \u0437\u0430\u0434\u043d\u0435\u0433\u043e \u0444\u043e\u043d\u0430 \u043f\u0440\u0438 \u043e\u0442\u043a\u0440\u044b\u0442\u043e\u043c ClickGUI").setValue(false).visible(() -> this.guiType.get().equals("\u041a\u043b\u0430\u0441\u0441\u0438\u0447\u0435\u0441\u043a\u0438\u0439"));
    private final BooleanSetting gradient = new BooleanSetting("\u0413\u0440\u0430\u0434\u0438\u0435\u043d\u0442", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0435\u0442 \u0433\u0440\u0430\u0434\u0438\u0435\u043d\u0442 \u0441\u043d\u0438\u0437\u0443 \u044d\u043a\u0440\u0430\u043d\u0430").setValue(true).visible(() -> this.guiType.get().equals("\u041a\u043b\u0430\u0441\u0441\u0438\u0447\u0435\u0441\u043a\u0438\u0439"));
    private final BooleanSetting shine = new BooleanSetting("\u0411\u043b\u0438\u043a", "\u0410\u043d\u0438\u043c\u0438\u0440\u043e\u0432\u0430\u043d\u043d\u0430\u044f \u043f\u043e\u043b\u043e\u0441\u043a\u0430 \u0431\u043b\u0438\u043a\u0430 \u043d\u0430 \u043f\u0430\u043d\u0435\u043b\u044f\u0445 ClickGUI").setValue(false).visible(() -> this.guiType.get().equals("\u041a\u043b\u0430\u0441\u0441\u0438\u0447\u0435\u0441\u043a\u0438\u0439"));
    private final SliderSettings shineSize = new SliderSettings("\u0420\u0430\u0437\u043c\u0435\u0440 \u0431\u043b\u0438\u043a\u0430", "\u0428\u0438\u0440\u0438\u043d\u0430 \u043f\u043e\u043b\u043e\u0441\u043a\u0438 \u0431\u043b\u0438\u043a\u0430").setValue(1.0f).range(0.5f, 2.0f).step(0.05f).visible(() -> this.shine.isValue() && this.guiType.get().equals("\u041a\u043b\u0430\u0441\u0441\u0438\u0447\u0435\u0441\u043a\u0438\u0439"));
    private final SliderSettings shineSpeed = new SliderSettings("\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u0431\u043b\u0438\u043a\u0430", "\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u0434\u0432\u0438\u0436\u0435\u043d\u0438\u044f \u0431\u043b\u0438\u043a\u0430").setValue(1.0f).range(0.5f, 2.0f).step(0.05f).visible(() -> this.shine.isValue() && this.guiType.get().equals("\u041a\u043b\u0430\u0441\u0441\u0438\u0447\u0435\u0441\u043a\u0438\u0439"));
    private final SliderSettings shineBrightness = new SliderSettings("\u042f\u0440\u043a\u043e\u0441\u0442\u044c \u0431\u043b\u0438\u043a\u0430", "\u042f\u0440\u043a\u043e\u0441\u0442\u044c \u0441\u0432\u0435\u0447\u0435\u043d\u0438\u044f \u0431\u043b\u0438\u043a\u0430").setValue(0.45f).range(0.1f, 1.0f).step(0.05f).visible(() -> this.shine.isValue() && this.guiType.get().equals("\u041a\u043b\u0430\u0441\u0441\u0438\u0447\u0435\u0441\u043a\u0438\u0439"));
    private final SliderSettings gradientIntensity = new SliderSettings("\u0418\u043d\u0442\u0435\u043d\u0441\u0438\u0432\u043d\u043e\u0441\u0442\u044c \u0433\u0440\u0430\u0434\u0438\u0435\u043d\u0442\u0430", "\u0412\u044b\u0441\u043e\u0442\u0430 \u0433\u0440\u0430\u0434\u0438\u0435\u043d\u0442\u0430").setValue(0.3f).range(0.1f, 0.5f).step(0.05f).visible(() -> this.guiType.get().equals("\u041a\u043b\u0430\u0441\u0441\u0438\u0447\u0435\u0441\u043a\u0438\u0439"));
    public static SliderSettings scrollSpeed;

    public static ClickGui getInstance() {
        if (instance != null) {
            return instance;
        }
        try {
            Wiosvisual wiosvisual = Wiosvisual.getInstance();
            if (wiosvisual == null) {
                return null;
            }
            ModuleRepository moduleRepository = wiosvisual.getModuleRepository();
            if (moduleRepository == null) {
                return null;
            }
            instance = moduleRepository.modules().stream().filter(m -> m instanceof ClickGui).map(m -> (ClickGui)m).findFirst().orElse(null);
            return instance;
        }
        catch (Exception ignored) {
            return null;
        }
    }

    public ClickGui() {
        super("ClickGui", "Click GUI", ModuleCategory.RENDER);
        this.setup(this.guiType, this.showCustomization, this.showModuleDescriptions, this.backgroundBlur, this.gradient, this.shine, this.shineSize, this.shineSpeed, this.shineBrightness, this.gradientIntensity, scrollSpeed);
        this.setState(true);
        this.setKey(344);
        instance = this;
    }

    @Override
    public void setKey(int key) {
        super.setKey(344);
    }

    @Override
    public int getKey() {
        return 344;
    }

    @Override
    public void setSecondKey(int secondKey) {
        super.setSecondKey(-1);
    }

    @Override
    public int getSecondKey() {
        return -1;
    }

    @Override
    public void setBindFromSetting(BindSetting bind) {
        this.setKey(344);
        this.setSecondKey(-1);
    }

    public static void enforceOpenKey() {
        ClickGui clickGui = ClickGui.getInstance();
        if (clickGui != null) {
            clickGui.setKey(344);
            clickGui.setSecondKey(-1);
            if (!clickGui.isState()) {
                clickGui.setState(true);
            }
        }
    }

    public boolean shouldShowModuleDescriptions() {
        return this.showModuleDescriptions.isValue();
    }

    public boolean isDropDownMode() {
        return false;
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

    public boolean shouldRenderCustomization() {
        return this.showCustomization.isValue();
    }

    public boolean shouldRenderBackgroundBlur() {
        return this.backgroundBlur.isValue();
    }

    public boolean shouldRenderGradient() {
        return this.gradient.isValue();
    }

    public boolean shouldRenderShine() {
        return this.shine.isValue();
    }

    public float getShineSize() {
        return this.shineSize.getValue();
    }

    public float getShineSpeed() {
        return this.shineSpeed.getValue();
    }

    public float getShineBrightness() {
        return this.shineBrightness.getValue();
    }

    public float getGradientIntensity() {
        return this.gradientIntensity.getValue();
    }

    public RadioSetting getGuiType() {
        return this.guiType;
    }

    public BooleanSetting getShowCustomization() {
        return this.showCustomization;
    }

    public BooleanSetting getShowModuleDescriptions() {
        return this.showModuleDescriptions;
    }

    public BooleanSetting getBackgroundBlur() {
        return this.backgroundBlur;
    }

    public BooleanSetting getGradient() {
        return this.gradient;
    }

    public BooleanSetting getShine() {
        return this.shine;
    }

    static {
        scrollSpeed = new SliderSettings("\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u0441\u043a\u0440\u043e\u043b\u043b\u0430", "\u041a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e \u043c\u043e\u0434\u0443\u043b\u0435\u0439 \u0437\u0430 \u043e\u0434\u0438\u043d \u0441\u043a\u0440\u043e\u043b\u043b").setValue(1.0f).range(1.0f, 4.0f).step(1.0f).visible(() -> ClickGui.getInstance() != null && ClickGui.getInstance().guiType.get().equals("\u041a\u043b\u0430\u0441\u0441\u0438\u0447\u0435\u0441\u043a\u0438\u0439"));
    }
}
