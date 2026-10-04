package code.wiosvisual.features.impl.render;

import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.events.render.FogEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import java.awt.Color;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

public class Ambience
extends Module {
    public static final String WEATHER_NONE = "Без изменений";
    public static final String WEATHER_CLEAR = "Солнечно";
    public static final String WEATHER_RAIN = "Дождь";
    public static final String WEATHER_THUNDER = "Гроза";

    public final SliderSettings time = new SliderSettings("\u0412\u0440\u0435\u043c\u044f", "\u0412\u0440\u0435\u043c\u044f \u0441\u0443\u0442\u043e\u043a (0-24000)").range(0, 24000).setValue(1000.0f);
    public final BooleanSetting customFog = new BooleanSetting("\u041a\u0430\u0441\u0442\u043e\u043c\u043d\u044b\u0439 \u0442\u0443\u043c\u0430\u043d", "\u0412\u043a\u043b\u044e\u0447\u0438\u0442\u044c \u043a\u0430\u0441\u0442\u043e\u043c\u043d\u044b\u0439 \u0442\u0443\u043c\u0430\u043d");
    public final SliderSettings fogStart = new SliderSettings("\u041d\u0430\u0447\u0430\u043b\u043e \u0442\u0443\u043c\u0430\u043d\u0430", "\u0414\u0438\u0441\u0442\u0430\u043d\u0446\u0438\u044f \u043d\u0430\u0447\u0430\u043b\u0430 \u0442\u0443\u043c\u0430\u043d\u0430").range(0, 500).setValue(5.0f).visible(() -> this.customFog.isValue());
    public final SliderSettings fogEnd = new SliderSettings("\u041a\u043e\u043d\u0435\u0446 \u0442\u0443\u043c\u0430\u043d\u0430", "\u0414\u0438\u0441\u0442\u0430\u043d\u0446\u0438\u044f \u043a\u043e\u043d\u0446\u0430 \u0442\u0443\u043c\u0430\u043d\u0430").range(0, 1000).setValue(150.0f).visible(() -> this.customFog.isValue());
    public final ColorSetting fogColor = new ColorSetting("\u0426\u0432\u0435\u0442 \u0442\u0443\u043c\u0430\u043d\u0430", "\u0426\u0432\u0435\u0442 \u043a\u0430\u0441\u0442\u043e\u043c\u043d\u043e\u0433\u043e \u0442\u0443\u043c\u0430\u043d\u0430").value(new Color(150, 130, 255).getRGB()).presets(new Color(150, 130, 255).getRGB(), new Color(120, 180, 255).getRGB(), new Color(235, 120, 220).getRGB(), new Color(255, 255, 255).getRGB()).visible(() -> this.customFog.isValue());

    /** Погода в мире: переопределяет дождь и грозу на клиенте. Порт из DELTA. */
    public final SelectSetting weather = new SelectSetting("Погода", "Погода в мире").value(WEATHER_NONE, WEATHER_CLEAR, WEATHER_RAIN, WEATHER_THUNDER).selected(WEATHER_NONE);

    /** Насыщенность картинки мира: пост-обработка в конце renderWorld. */
    public final BooleanSetting saturation = new BooleanSetting("Насыщенность", "Пост-обработка цвета мира");
    public final SliderSettings saturationValue = new SliderSettings("Уровень насыщенности", "0 — ч/б, 1 — как в ванилле, выше — сочнее").range(0.0f, 3.0f).setValue(1.35f).step(0.05f).visible(() -> this.saturation.isValue());
    public final SliderSettings vibrance = new SliderSettings("Vibrance", "Дотягивает только блёклые цвета, не пережигая яркие").range(0.0f, 1.0f).setValue(0.2f).step(0.05f).visible(() -> this.saturation.isValue());

    private double animatedTime = 1000.0;

    public Ambience() {
        super("Ambience", "Ambience", ModuleCategory.RENDER);
        this.setup(this.time, this.weather, this.saturation, this.saturationValue, this.vibrance, this.customFog, this.fogStart, this.fogEnd, this.fogColor);
    }

    public static Ambience getInstance() {
        return Instance.get(Ambience.class);
    }

    @Override
    public void activate() {
        this.animatedTime = this.time.getValue();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        double targetTime = this.time.getValue();
        double speed = 0.15;
        this.animatedTime += (targetTime - this.animatedTime) * speed;
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            event.cancel();
        }
    }

    @EventHandler
    public void onFog(FogEvent event) {
        if (!this.customFog.isValue()) {
            return;
        }
        float end = Math.max(1.0f, this.fogEnd.getValue());
        float start = Math.min(this.fogStart.getValue(), end - 0.1f);
        event.setDistance(end);
        event.setDensity(Math.max(0.0f, Math.min(100.0f, (1.0f - start / end) * 100.0f)));
        event.setColor(this.fogColor.getColor());
        event.cancel();
    }

    public static boolean customFogActive() {
        Ambience instance = Ambience.getInstance();
        return instance != null && instance.isState() && instance.customFog.isValue();
    }

    public static float customFogEnd() {
        return Math.max(1.0f, Ambience.getInstance().fogEnd.getValue());
    }

    public static float customFogStart() {
        Ambience instance = Ambience.getInstance();
        return Math.max(0.0f, Math.min(instance.fogStart.getValue(), Ambience.customFogEnd() - 0.1f));
    }

    public static org.joml.Vector4f customFogColor() {
        int c = Ambience.getInstance().fogColor.getColor();
        return new org.joml.Vector4f(((c >> 16) & 0xFF) / 255.0f, ((c >> 8) & 0xFF) / 255.0f, (c & 0xFF) / 255.0f, 1.0f);
    }

    public long getCustomTime() {
        return (long)this.animatedTime;
    }

    /** Нужна ли пост-обработка насыщенности прямо сейчас. */
    public static boolean saturationActive() {
        Ambience instance = Ambience.getInstance();
        return instance != null && instance.isState() && instance.saturation.isValue() && (Math.abs(instance.saturationValue.getValue() - 1.0f) > 0.001f || instance.vibrance.getValue() > 0.001f);
    }

    public static float saturationLevel() {
        Ambience instance = Ambience.getInstance();
        return instance == null ? 1.0f : instance.saturationValue.getValue();
    }

    public static float vibranceLevel() {
        Ambience instance = Ambience.getInstance();
        return instance == null ? 0.0f : instance.vibrance.getValue();
    }

    /** Активна ли подмена погоды (модуль включён и выбран режим). */
    public static boolean overridesWeather() {
        Ambience instance = Ambience.getInstance();
        return instance != null && instance.isState() && !instance.weather.isSelected(WEATHER_NONE);
    }

    /** Сила дождя для выбранного режима. */
    public static float rainGradient(float original) {
        Ambience instance = Ambience.getInstance();
        if (instance == null) {
            return original;
        }
        if (instance.weather.isSelected(WEATHER_CLEAR)) {
            return 0.0f;
        }
        if (instance.weather.isSelected(WEATHER_RAIN) || instance.weather.isSelected(WEATHER_THUNDER)) {
            return 1.0f;
        }
        return original;
    }

    /** Сила грозы для выбранного режима. */
    public static float thunderGradient(float original) {
        Ambience instance = Ambience.getInstance();
        if (instance == null) {
            return original;
        }
        if (instance.weather.isSelected(WEATHER_THUNDER)) {
            return 1.0f;
        }
        if (instance.weather.isSelected(WEATHER_CLEAR) || instance.weather.isSelected(WEATHER_RAIN)) {
            return 0.0f;
        }
        return original;
    }
}
