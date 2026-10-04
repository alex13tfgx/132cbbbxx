package code.wiosvisual.features.impl.render;

import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.display.render.shader.MotionBlurRenderer;

/**
 * Плавное размытие движения камеры. Сам проход — MotionBlurRenderer,
 * вызывается из GameRendererMixin между отрисовкой мира и руки.
 */
public class MotionBlur
extends Module {
    private final SliderSettings strength = new SliderSettings("Сила", "Насколько сильно размывается движение").setValue(50.0f).range(1.0f, 100.0f).step(1.0f);
    private final SliderSettings quality = new SliderSettings("Качество", "Число выборок вдоль движения: больше — плавнее, но тяжелее для видеокарты").setValue(32.0f).range(8.0f, 100.0f).step(1.0f);

    public static MotionBlur getInstance() {
        return Instance.get(MotionBlur.class);
    }

    public MotionBlur() {
        super("MotionBlur", "Motion Blur", ModuleCategory.RENDER);
        this.setup(this.strength, this.quality);
    }

    /** Как в Pulse: 50 на слайдере — это полное смещение за кадр. */
    public float blendFactor() {
        return this.strength.getValue() / 50.0f;
    }

    public int samples() {
        return Math.round(this.quality.getValue());
    }

    @Override
    public void activate() {
        super.activate();
        MotionBlurRenderer.reset();
    }

    @Override
    public void deactivate() {
        super.deactivate();
        MotionBlurRenderer.reset();
    }
}
