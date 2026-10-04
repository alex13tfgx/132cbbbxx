package code.wiosvisual.features.impl.misc;

import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import java.io.BufferedInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import net.minecraft.util.Identifier;

/**
 * Свои звуки на включение и выключение модулей: четыре набора wav-файлов
 * с отдельной громкостью. Порт Sounds из DELTA.
 */
public class Sounds
extends Module {
    private static final String TYPE_1 = "Тип 1";
    private static final String TYPE_2 = "Тип 2";
    private static final String TYPE_3 = "Тип 3";
    private static final String TYPE_4 = "Тип 4";

    public final SelectSetting typeSetting = new SelectSetting("Набор звуков", "Какой набор звуков играть").value(TYPE_1, TYPE_2, TYPE_3, TYPE_4).selected(TYPE_1);
    public final SliderSettings volumeSetting = new SliderSettings("Громкость", "Громкость воспроизведения").setValue(0.25f).range(0.0f, 1.0f).step(0.05f);

    /** Отдельный демон-поток: воспроизведение не должно блокировать рендер. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Wiosvisual-ClientSound");
        thread.setDaemon(true);
        return thread;
    });

    public static Sounds getInstance() {
        return Instance.get(Sounds.class);
    }

    public Sounds() {
        super("Sounds", "Sounds", ModuleCategory.MISC);
        this.setup(this.typeSetting, this.volumeSetting);
    }

    /**
     * Вызывается при переключении любого модуля. Сам модуль Sounds должен быть
     * включён, иначе звук не играет.
     */
    public void onModuleToggle(boolean enabled) {
        if (!this.isState()) {
            return;
        }
        this.play(this.fileFor(enabled));
    }

    @Override
    public void activate() {
        super.activate();
        this.play(this.fileFor(true));
    }

    private void play(String fileName) {
        if (fileName == null || fileName.isEmpty() || mc == null || mc.getResourceManager() == null) {
            return;
        }
        this.executor.execute(() -> {
            try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(new BufferedInputStream(mc.getResourceManager().getResource(Identifier.of((String)"wiosvisual", (String)("sounds/" + fileName))).get().getInputStream()));){
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gain = (FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float decibels = (float)(20.0 * Math.log10(Math.max(this.volumeSetting.getValue(), 1.0E-4f)));
                    gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
                }
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
                clip.start();
            }
            catch (Exception error) {
                // Битый или отсутствующий wav не должен ронять клиент.
            }
        });
    }

    private String fileFor(boolean enabled) {
        if (this.typeSetting.isSelected(TYPE_1)) {
            return enabled ? "enable.wav" : "disable.wav";
        }
        if (this.typeSetting.isSelected(TYPE_2)) {
            return enabled ? "enable1.wav" : "disable1.wav";
        }
        if (this.typeSetting.isSelected(TYPE_3)) {
            return enabled ? "enable2.wav" : "disable2.wav";
        }
        if (this.typeSetting.isSelected(TYPE_4)) {
            return enabled ? "enable3.wav" : "disable3.wav";
        }
        return null;
    }
}
