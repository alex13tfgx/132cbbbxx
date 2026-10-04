/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_276
 *  net.minecraft.class_310
 *  net.minecraft.class_4184
 *  net.minecraft.class_5498
 *  net.minecraft.class_5636
 *  net.minecraft.class_6367
 */
package code.wiosvisual.features.impl.render;

import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.display.render.shader.HandFx;
import code.wiosvisual.utils.theme.ThemeManager;
import java.awt.Color;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;

public class HandShader
extends Module {
    public final SelectSetting mode = new SelectSetting("\u0420\u0435\u0436\u0438\u043c", "\u0420\u0435\u0436\u0438\u043c \u044d\u0444\u0444\u0435\u043a\u0442\u0430 \u0448\u0435\u0439\u0434\u0435\u0440\u0430").value("\u0412\u0435\u0431-\u0448\u0435\u0439\u0434\u0435\u0440", "\u041f\u043b\u0430\u0437\u043c\u0430");
    public final ColorSetting color = new ColorSetting("\u0426\u0432\u0435\u0442", "\u0426\u0432\u0435\u0442 \u0448\u0435\u0439\u0434\u0435\u0440\u0430").value(new Color(150, 130, 255).getRGB()).presets(new Color(150, 130, 255).getRGB(), new Color(120, 180, 255).getRGB(), new Color(235, 120, 220).getRGB(), new Color(255, 255, 255).getRGB());
    public final SliderSettings blurRadius = new SliderSettings("\u0420\u0430\u0434\u0438\u0443\u0441 \u0440\u0430\u0437\u043c\u044b\u0442\u0438\u044f", "\u0420\u0430\u0434\u0438\u0443\u0441 \u0441\u0432\u0435\u0447\u0435\u043d\u0438\u044f \u0432\u043e\u043a\u0440\u0443\u0433 \u0440\u0443\u043a\u0438").setValue(18.0f).range(1.0f, 50.0f).visible(() -> this.mode.isSelected("\u041d\u0435\u043f\u043e\u043b\u043d\u044b\u0439"));
    public final SliderSettings opacity = new SliderSettings("\u041d\u0435\u043f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c", "\u041f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c \u044d\u0444\u0444\u0435\u043a\u0442\u0430").setValue(0.6f).range(0.0f, 1.0f);
    public final SliderSettings threshold = new SliderSettings("\u041f\u043e\u0440\u043e\u0433", "\u041c\u0438\u043d\u0438\u043c\u0430\u043b\u044c\u043d\u0430\u044f \u044f\u0440\u043a\u043e\u0441\u0442\u044c \u0441\u0432\u0435\u0447\u0435\u043d\u0438\u044f").setValue(0.03f).range(0.005f, 0.15f).visible(() -> this.mode.isSelected("\u041d\u0435\u043f\u043e\u043b\u043d\u044b\u0439"));
    public final SliderSettings shaderSpeed = new SliderSettings("\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c", "\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u0430\u043d\u0438\u043c\u0430\u0446\u0438\u0438 \u0448\u0435\u0439\u0434\u0435\u0440\u0430").setValue(1.0f).range(0.1f, 3.0f).visible(() -> this.mode.isSelected("\u0412\u0435\u0431-\u0448\u0435\u0439\u0434\u0435\u0440") || this.mode.isSelected("\u041f\u043b\u0430\u0437\u043c\u0430"));
    public final SliderSettings shaderScale = new SliderSettings("\u041c\u0430\u0441\u0448\u0442\u0430\u0431", "\u041f\u0440\u0438\u0431\u043b\u0438\u0436\u0435\u043d\u0438\u0435 \u0438\u043b\u0438 \u043e\u0442\u0434\u0430\u043b\u0435\u043d\u0438\u0435 \u0443\u0437\u043e\u0440\u0430").setValue(1.0f).range(0.25f, 4.0f).visible(() -> this.mode.isSelected("\u0412\u0435\u0431-\u0448\u0435\u0439\u0434\u0435\u0440") || this.mode.isSelected("\u041f\u043b\u0430\u0437\u043c\u0430"));
    private Framebuffer savedBefore = null;
    private Framebuffer savedAfter = null;
    private SimpleFramebuffer outputFbo = null;
    private boolean captureReady = false;

    public static HandShader getInstance() {
        return Instance.get(HandShader.class);
    }

    public HandShader() {
        super("Hand Shader", "Hand Shader", ModuleCategory.RENDER);
        this.setup(this.mode, this.color, this.blurRadius, this.opacity, this.threshold, this.shaderSpeed, this.shaderScale);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        this.invalidateCapture();
    }

    public void invalidateCapture() {
        this.savedBefore = null;
        this.savedAfter = null;
        this.outputFbo = null;
        this.captureReady = false;
    }

    public void beginHandCapture() {
        this.captureReady = false;
    }

    public boolean shouldRender() {
        return this.isState() && HandShader.firstPersonHandVisible();
    }

    /** Рука видна от первого лица: не в третьем лице, не под водой и не в лаве. Общее для эффектов руки. */
    public static boolean firstPersonHandVisible() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return false;
        }
        if (mc.options.getPerspective() != Perspective.FIRST_PERSON) {
            return false;
        }
        Camera camera = mc.gameRenderer.getCamera();
        if (camera.getSubmersionType() != CameraSubmersionType.NONE) {
            return false;
        }
        if (mc.player.isSubmergedInWater() || mc.player.isInLava()) {
            return false;
        }
        return !mc.player.isSwimming() || !mc.player.isTouchingWater();
    }

    public void onShaderCapture(Framebuffer before, Framebuffer after) {
        this.savedBefore = before;
        this.savedAfter = after;
        this.captureReady = true;
    }

    public void renderCapturedEffect() {
        if (!this.shouldRender()) {
            return;
        }
        float[] tint = HandShader.rgba(this.color.getColor(), this.opacity.getValue());
        float[] tint2 = HandShader.rgba(ThemeManager.secondaryColor.getColor(), this.opacity.getValue());
        float time = (float)(System.currentTimeMillis() % 100000L) / 1000.0f * this.shaderSpeed.getValue();
        float plasma = this.mode.isSelected("\u041f\u043b\u0430\u0437\u043c\u0430") ? 1.0f : 0.0f;
        float[] params = new float[]{
                time, this.opacity.getValue(), plasma, this.shaderScale.getValue(),
                tint[0], tint[1], tint[2], 0.0f,
                tint2[0], tint2[1], tint2[2], 0.0f,
                HandFx.width(), HandFx.height(), 0.0f, 0.0f};
        HandFx.draw(HandFx.HAND_SHADER, params);
    }

    private static float[] rgba(int argb, float alphaOverride) {
        Color c = new Color(argb, true);
        return new float[]{(float)c.getRed() / 255.0f, (float)c.getGreen() / 255.0f, (float)c.getBlue() / 255.0f, alphaOverride};
    }
}
