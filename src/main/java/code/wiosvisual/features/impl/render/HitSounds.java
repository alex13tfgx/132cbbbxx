package code.wiosvisual.features.impl.render;

import code.wiosvisual.events.player.AttackEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import java.io.BufferedInputStream;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Звук при ударе по цели. Файлы лежат в assets/wiosvisual/sounds/hitsounds и играются
 * через Java Sound мимо звукового движка игры — громкость не зависит от настроек звука.
 */
public class HitSounds
extends Module {
    private static final String DEFAULT = "Default";
    private static final String MOAN = "Moan";
    private static final String[] HIT_FILES = {"hit3"};
    private static final String[] MOAN_FILES = {"moan1", "moan2", "moan3", "moan4"};
    private static final Random RANDOM = new Random();

    private final SelectSetting sound = new SelectSetting("Звук", "Какой звук играть при ударе").value(DEFAULT, "Bell", "Bonk", "Bubble", "Pop", "Uwu", MOAN).selected(DEFAULT);
    private final SliderSettings volume = new SliderSettings("Громкость", "Громкость звука удара").setValue(1.0f).range(0.1f, 2.0f).step(0.1f);
    private final BooleanSetting onlyPlayers = new BooleanSetting("Только игроки", "Играть звук только при ударе по игроку").setValue(false);

    /** Отдельный демон-поток: открытие wav не должно подтормаживать тик атаки. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Wiosvisual-HitSounds");
        thread.setDaemon(true);
        return thread;
    });

    public HitSounds() {
        super("HitSounds", "Hit Sounds", ModuleCategory.RENDER);
        this.setup(this.sound, this.volume, this.onlyPlayers);
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        // Удар отменён (например, NoFriendDamage) — реагировать не на что.
        if (e.isCancelled()) {
            return;
        }
        if (e.getEntity() == null) {
            return;
        }
        if (this.onlyPlayers.isValue() && !(e.getEntity() instanceof PlayerEntity)) {
            return;
        }
        this.play(this.pickFile());
    }

    private String pickFile() {
        String selected = this.sound.getSelected();
        if (DEFAULT.equals(selected)) {
            return HIT_FILES[RANDOM.nextInt(HIT_FILES.length)];
        }
        if (MOAN.equals(selected)) {
            return MOAN_FILES[RANDOM.nextInt(MOAN_FILES.length)];
        }
        return selected.toLowerCase(Locale.ROOT);
    }

    private void play(String name) {
        if (mc == null || mc.getResourceManager() == null) {
            return;
        }
        float level = this.volume.getValue();
        this.executor.execute(() -> {
            Identifier id = Identifier.of("wiosvisual", "sounds/hitsounds/" + name + ".wav");
            try (AudioInputStream stream = AudioSystem.getAudioInputStream(new BufferedInputStream(mc.getResourceManager().getResource(id).get().getInputStream()))) {
                Clip clip = AudioSystem.getClip();
                clip.open(stream);
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gain = (FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float decibels = (float)(20.0 * Math.log10(Math.max(0.01f, level)));
                    gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
                }
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
                clip.start();
            }
            catch (Exception ignored) {
                // Нет файла или звуковое устройство занято — просто без звука.
            }
        });
    }
}
