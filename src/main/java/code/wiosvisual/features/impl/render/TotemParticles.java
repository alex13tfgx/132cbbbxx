package code.wiosvisual.features.impl.render;

import code.wiosvisual.display.hud.HudTheme;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.particle.Particle;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;

/**
 * Настройка частиц сработавшего тотема: цвет, размер, количество и разлёт.
 * Сами частицы перехватываются в {@code ParticleManagerMixin}.
 */
public class TotemParticles
extends Module {
    public final BooleanSetting hide = new BooleanSetting("Скрыть", "Полностью убрать частицы тотема").setValue(false);
    public final SelectSetting colorMode = new SelectSetting("Цвет", "Откуда брать цвет частиц").value("Тема", "Свой", "Радуга", "Ванильный").selected("Тема").visible(() -> !this.hide.isValue());
    public final ColorSetting color1 = new ColorSetting("Цвет 1", "Первый цвет частиц").value(new Color(150, 130, 255).getRGB()).visible(() -> !this.hide.isValue() && this.colorMode.isSelected("Свой"));
    public final ColorSetting color2 = new ColorSetting("Цвет 2", "Второй цвет частиц").value(new Color(120, 200, 255).getRGB()).visible(() -> !this.hide.isValue() && this.colorMode.isSelected("Свой"));
    public final SliderSettings size = new SliderSettings("Размер", "Размер частиц").range(0.3f, 3.0f).setValue(1.0f).step(0.05f).visible(() -> !this.hide.isValue());
    public final SliderSettings amount = new SliderSettings("Количество", "Сколько частиц вылетает (1 — как в ванилле)").range(0.1f, 3.0f).setValue(1.0f).step(0.05f).visible(() -> !this.hide.isValue());
    public final SliderSettings speed = new SliderSettings("Разлёт", "Скорость разлёта частиц (1 — как в ванилле)").range(0.2f, 3.0f).setValue(1.0f).step(0.05f).visible(() -> !this.hide.isValue());

    public TotemParticles() {
        super("TotemParticles", "Totem Particles", ModuleCategory.RENDER);
        this.setup(this.hide, this.colorMode, this.color1, this.color2, this.size, this.amount, this.speed);
    }

    public static TotemParticles getInstance() {
        return Instance.get(TotemParticles.class);
    }

    public static TotemParticles activeFor(ParticleEffect effect) {
        TotemParticles module = TotemParticles.getInstance();
        if (module == null || !module.isState() || effect == null || effect.getType() != ParticleTypes.TOTEM_OF_UNDYING) {
            return null;
        }
        return module;
    }

    /**
     * Сколько частиц создать вместо одной ванильной. Дробная часть решается случайно:
     * при 1.5 в среднем полторы частицы, при 0.3 — примерно каждая третья.
     */
    public int rollCopies() {
        float value = this.amount.getValue();
        int whole = (int)value;
        return whole + (ThreadLocalRandom.current().nextFloat() < value - (float)whole ? 1 : 0);
    }

    public void apply(Particle particle) {
        if (particle == null) {
            return;
        }
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int color = switch (this.colorMode.getSelected()) {
            case "Тема" -> TotemParticles.lerp(HudTheme.accent(255), HudTheme.accentSecondary(255), rng.nextFloat());
            case "Свой" -> TotemParticles.lerp(this.color1.getColor(), this.color2.getColor(), rng.nextFloat());
            case "Радуга" -> Color.HSBtoRGB(rng.nextFloat(), 0.75f, 1.0f);
            default -> 0;
        };
        if (color != 0) {
            // Лёгкий разброс яркости, чтобы облако не выглядело плоской заливкой.
            float shade = 0.8f + rng.nextFloat() * 0.2f;
        }
        float scale = this.size.getValue();
        if (Math.abs(scale - 1.0f) > 0.01f) {
            particle.scale(scale);
        }
    }

    private static int lerp(int c1, int c2, float f) {
        int r = (int)MathHelper.lerp(f, c1 >> 16 & 0xFF, c2 >> 16 & 0xFF);
        int g = (int)MathHelper.lerp(f, c1 >> 8 & 0xFF, c2 >> 8 & 0xFF);
        int b = (int)MathHelper.lerp(f, c1 & 0xFF, c2 & 0xFF);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
