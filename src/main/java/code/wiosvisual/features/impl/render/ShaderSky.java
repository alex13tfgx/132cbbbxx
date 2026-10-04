/*
 * Shader Sky — procedural sky (Aurora / Night / Snow / Sky / Star / Glow),
 * a 1.21.4-native reimplementation of Nightix's ShaderSky. The original targets
 * the 1.21.5+ GPU pipeline which does not exist here, so this reuses BigBrr's
 * legacy sky-shader machinery (SkyShaderUtil + fullscreen quad) instead.
 */
package code.wiosvisual.features.impl.render;

import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.shader.ShaderSkyPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class ShaderSky
extends Module {
    private final SelectSetting mode = new SelectSetting("Режим", "Тип неба").value("Aurora", "Night", "Snow", "Sky", "Star", "Glow").selected("Aurora");
    private final SliderSettings intensity = new SliderSettings("Сила", "Интенсивность эффекта").setValue(1.0f).range(0.1f, 1.0f).step(0.05f);
    private final SliderSettings speed = new SliderSettings("Скорость", "Скорость анимации").setValue(1.0f).range(0.1f, 3.0f).step(0.1f);
    private final SliderSettings scale = new SliderSettings("Масштаб", "Масштаб узора").setValue(1.1f).range(0.45f, 3.0f).step(0.05f);
    private final SliderSettings stars = new SliderSettings("Звёзды", "Плотность звёзд").setValue(0.75f).range(0.0f, 1.0f).step(0.05f);
    private final SelectSetting typeColor = new SelectSetting("Режим цвета", "Color mode").value("Тема", "Свой").selected("Тема");
    private final ColorSetting color1 = new ColorSetting("Цвет 1", "Первый цвет").value(0xFF79E6FF).visible(() -> this.typeColor.isSelected("Свой"));
    private final ColorSetting color2 = new ColorSetting("Цвет 2", "Второй цвет").value(0xFFFF7AE6).visible(() -> this.typeColor.isSelected("Свой"));
    private final BooleanSetting vanillaSky = new BooleanSetting("Ванильное небо", "Не скрывать ванильное небо").setValue(false);
    private long startMillis = -1L;

    public ShaderSky() {
        super("Shader Sky", "Shader Sky", ModuleCategory.RENDER);
        this.setup(this.mode, this.intensity, this.speed, this.scale, this.stars, this.typeColor, this.color1, this.color2, this.vanillaSky);
    }

    public static ShaderSky getInstance() {
        return Instance.get(ShaderSky.class);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        this.failed = false;
        this.startMillis = -1L;
    }

    public static boolean shouldReplaceVanillaSky() {
        ShaderSky module = ShaderSky.getInstance();
        if (module == null || !module.isState() || module.vanillaSky.isValue()) {
            return false;
        }
        return !(MinecraftClient.getInstance().currentScreen instanceof GameMenuScreen);
    }

    private int getColor1() {
        if (this.typeColor.isSelected("Тема")) {
            return ColorAssist.getClientColor();
        }
        return this.color1.getColor();
    }

    private int getColor2() {
        if (this.typeColor.isSelected("Тема")) {
            return ColorAssist.multDark(ColorAssist.getClientColor(), 0.25f);
        }
        return this.color2.getColor();
    }

    private float modeIndex() {
        switch (this.mode.getSelected()) {
            case "Night": return 1.0f;
            case "Snow": return 2.0f;
            case "Sky": return 3.0f;
            case "Star": return 4.0f;
            case "Glow": return 5.0f;
            default: return 0.0f;
        }
    }

    private ShaderSkyPipeline pipeline;
    private boolean failed;
    private final Matrix4f invViewProj = new Matrix4f();
    private final Matrix4f view = new Matrix4f();
    private final org.joml.Quaternionf rot = new org.joml.Quaternionf();

    public void renderSkyShader() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (this.failed || mc.world == null || mc.getEntityRenderDispatcher() == null || mc.getEntityRenderDispatcher().camera == null) {
            return;
        }
        if (this.startMillis < 0L) {
            this.startMillis = System.currentTimeMillis();
        }
        try {
            if (this.pipeline == null) {
                this.pipeline = new ShaderSkyPipeline();
            }
            Matrix4f inv = this.invViewProj.set(code.wiosvisual.utils.display.render.geometry.Render3D.lastProjMat)
                    .mul(this.view.rotation(mc.getEntityRenderDispatcher().camera.getRotation().conjugate(this.rot))).invert();
            float time = (System.currentTimeMillis() - this.startMillis) / 1000.0f;
            this.pipeline.draw(inv, time, this.intensity.getValue(), this.speed.getValue(), this.scale.getValue(),
                    this.stars.getValue(), this.modeIndex(), this.getColor1() & 0xFFFFFF, this.getColor2() & 0xFFFFFF);
        } catch (Throwable t) {
            this.failed = true;
            t.printStackTrace();
        }
    }



}
