package code.wiosvisual.features.impl.render;

import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.events.render.FogEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.color.ColorAssist;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

/**
 * Порт модуля BetterWorld: время суток, фуллбрайт, туман, небо и погода одним модулем.
 *
 * <p>Заметь: те же возможности по отдельности есть в {@link Ambience}, {@link Fullbright}
 * и {@link ShaderSky}. Включённые одновременно, они каждый тик пишут в одни и те же поля
 * мира и будут перебивать друг друга — держи включённым что-то одно.
 */
public class BetterWorld
extends Module {
    public static final String WEATHER_CLEAR = "Clear";
    public static final String WEATHER_RAIN = "Rain";
    public static final String WEATHER_STORM = "Storm";

    public final BooleanSetting changeTime = new BooleanSetting("Time Changer", "Зафиксировать время суток");
    public final SliderSettings time = new SliderSettings("Time", "Время суток шагами по 500 тиков").range(1.0f, 40.0f).step(1.0f).setValue(1.0f).visible(() -> this.changeTime.isValue());
    public final BooleanSetting fullBright = new BooleanSetting("FullBright", "Ночное зрение");
    public final BooleanSetting fog = new BooleanSetting("Fog", "Свой туман");
    public final SliderSettings start = new SliderSettings("StartFog", "Дистанция начала тумана").range(0.0f, 20.0f).step(1.0f).setValue(3.0f).visible(() -> this.fog.isValue());
    public final SliderSettings end = new SliderSettings("EndFog", "Дистанция конца тумана").range(0.0f, 80.0f).step(1.0f).setValue(50.0f).visible(() -> this.fog.isValue());
    public final SliderSettings alphaFog = new SliderSettings("AlphaFog", "Плотность тумана").range(30.0f, 100.0f).step(5.0f).setValue(90.0f).visible(() -> this.fog.isValue());
    public final BooleanSetting skyShader = new BooleanSetting("SkyShader", "Отключает ванильный эффект неба");
    public final BooleanSetting weather = new BooleanSetting("Weather", "Подмена погоды");
    public final SelectSetting weatherMode = new SelectSetting("WeatherMode", "Режим погоды").value(WEATHER_CLEAR, WEATHER_RAIN, WEATHER_STORM).selected(WEATHER_CLEAR);

    /** Погода сервера на момент включения — возвращаем её при выключении модуля. */
    private boolean previousRaining;

    public BetterWorld() {
        super("BetterWorld", "Better World", ModuleCategory.RENDER);
        this.setup(this.changeTime, this.time, this.fullBright, this.fog, this.start, this.end, this.alphaFog, this.skyShader, this.weather, this.weatherMode);
    }

    public static BetterWorld getInstance() {
        return Instance.get(BetterWorld.class);
    }

    @Override
    public void activate() {
        super.activate();
        if (BetterWorld.mc.world != null) {
            this.previousRaining = BetterWorld.mc.world.getLevelProperties().isRaining();
        }
    }

    @Override
    public void deactivate() {
        if (BetterWorld.mc.player != null && this.fullBright.isValue()) {
            BetterWorld.mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        if (BetterWorld.mc.world != null && this.weather.isValue()) {
            BetterWorld.mc.world.getLevelProperties().setRaining(this.previousRaining);
        }
        super.deactivate();
    }

    /** Пока время зафиксировано, обновления времени с сервера игнорируются. */
    @EventHandler
    public void onPacket(PacketEvent event) {
        if (this.changeTime.isValue() && event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            event.cancel();
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (BetterWorld.mc.player == null || BetterWorld.mc.world == null) {
            return;
        }
        if (this.changeTime.isValue()) {
            BetterWorld.mc.world.setTime(0L, (long)this.time.getValue() * 500L, false);
        }
        if (this.fullBright.isValue()) {
            BetterWorld.mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 2), BetterWorld.mc.player);
        }
        if (this.skyShader.isValue() && BetterWorld.mc.options != null) {
            BetterWorld.mc.options.getBobView().setValue(false);
        }
        if (this.weather.isValue()) {
            if (this.weatherMode.isSelected(WEATHER_RAIN)) {
                BetterWorld.mc.world.getLevelProperties().setRaining(true);
                BetterWorld.mc.world.setRainGradient(1.0f);
                BetterWorld.mc.world.setThunderGradient(0.0f);
            } else if (this.weatherMode.isSelected(WEATHER_STORM)) {
                BetterWorld.mc.world.getLevelProperties().setRaining(true);
                BetterWorld.mc.world.setRainGradient(1.0f);
                BetterWorld.mc.world.setThunderGradient(1.0f);
            } else {
                BetterWorld.mc.world.getLevelProperties().setRaining(false);
                BetterWorld.mc.world.setRainGradient(0.0f);
                BetterWorld.mc.world.setThunderGradient(0.0f);
            }
        }
    }

    /**
     * Плотность считается обратным ходом: приёмник события восстанавливает начало тумана
     * как {@code end * (1 - density / 100)}, поэтому сюда идёт {@code (1 - start / end) * 100}.
     */
    @EventHandler
    public void onFog(FogEvent event) {
        if (!this.fog.isValue()) {
            return;
        }
        float fogEnd = Math.max(1.0f, this.end.getValue());
        float fogStart = Math.min(this.start.getValue(), fogEnd - 0.1f);
        int alpha = (int)(255.0f * Math.max(0.0f, Math.min(1.0f, this.alphaFog.getValue() / 100.0f)));
        event.setDistance(fogEnd);
        event.setDensity(Math.max(0.0f, Math.min(100.0f, (1.0f - fogStart / fogEnd) * 100.0f)));
        event.setColor(ColorAssist.setAlpha(ColorAssist.getHudColor(), alpha));
        event.cancel();
    }
}
