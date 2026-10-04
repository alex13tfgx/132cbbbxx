/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1109
 *  net.minecraft.class_1113
 *  net.minecraft.class_2378
 *  net.minecraft.class_2960
 *  net.minecraft.class_3414
 *  net.minecraft.class_7923
 */
package code.wiosvisual.utils.client.sound;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class SoundManager
implements QuickImports {
    public static SoundEvent OPEN_GUI = SoundEvent.of((Identifier)Identifier.of((String)"minecraft:gui_open"));
    public static SoundEvent CLOSE_GUI = SoundEvent.of((Identifier)Identifier.of((String)"minecraft:gui_close"));
    public static SoundEvent ENABLE_MODULE = SoundEvent.of((Identifier)Identifier.of((String)"minecraft:module_enable"));
    public static SoundEvent DISABLE_MODULE = SoundEvent.of((Identifier)Identifier.of((String)"minecraft:module_disable"));
    public static SoundEvent CATEGORY_CLICK = SoundEvent.of((Identifier)Identifier.of((String)"minecraft:category_click"));
    public static SoundEvent NOTIF = SoundEvent.of((Identifier)Identifier.of((String)"minecraft:notif"));
    public static SoundEvent SETTING_INTERACT = CATEGORY_CLICK;
    public static SoundEvent ORTHODOX = SoundEvent.of((Identifier)Identifier.of((String)"minecraft:kolokolnia_kill"));
    public static SoundEvent KILL1 = SoundEvent.of((Identifier)Identifier.of((String)"minecraft:kill1"));
    public static SoundEvent KILL2 = SoundEvent.of((Identifier)Identifier.of((String)"minecraft:kill2"));
    public static SoundEvent KILL3 = SoundEvent.of((Identifier)Identifier.of((String)"minecraft:kill3"));
    public static SoundEvent KILL4 = SoundEvent.of((Identifier)Identifier.of((String)"minecraft:kill4"));
    public static SoundEvent KILL5 = SoundEvent.of((Identifier)Identifier.of((String)"minecraft:kill5"));
    public static SoundEvent KILL6 = SoundEvent.of((Identifier)Identifier.of((String)"minecraft:kill6"));
    public static SoundEvent CRITICAL = SoundEvent.of((Identifier)Identifier.of((String)"minecraft:critical"));
    public static SoundEvent HELLO = SoundEvent.of((Identifier)Identifier.of((String)"minecraft:hello"));

    public static void init() {
        Registry.register((Registry)Registries.SOUND_EVENT, (Identifier)OPEN_GUI.id(), (Object)OPEN_GUI);
        Registry.register((Registry)Registries.SOUND_EVENT, (Identifier)CLOSE_GUI.id(), (Object)CLOSE_GUI);
        Registry.register((Registry)Registries.SOUND_EVENT, (Identifier)ENABLE_MODULE.id(), (Object)ENABLE_MODULE);
        Registry.register((Registry)Registries.SOUND_EVENT, (Identifier)DISABLE_MODULE.id(), (Object)DISABLE_MODULE);
        Registry.register((Registry)Registries.SOUND_EVENT, (Identifier)CATEGORY_CLICK.id(), (Object)CATEGORY_CLICK);
        Registry.register((Registry)Registries.SOUND_EVENT, (Identifier)NOTIF.id(), (Object)NOTIF);
        Registry.register((Registry)Registries.SOUND_EVENT, (Identifier)ORTHODOX.id(), (Object)ORTHODOX);
        Registry.register((Registry)Registries.SOUND_EVENT, (Identifier)KILL1.id(), (Object)KILL1);
        Registry.register((Registry)Registries.SOUND_EVENT, (Identifier)KILL2.id(), (Object)KILL2);
        Registry.register((Registry)Registries.SOUND_EVENT, (Identifier)KILL3.id(), (Object)KILL3);
        Registry.register((Registry)Registries.SOUND_EVENT, (Identifier)KILL4.id(), (Object)KILL4);
        Registry.register((Registry)Registries.SOUND_EVENT, (Identifier)KILL5.id(), (Object)KILL5);
        Registry.register((Registry)Registries.SOUND_EVENT, (Identifier)KILL6.id(), (Object)KILL6);
        Registry.register((Registry)Registries.SOUND_EVENT, (Identifier)CRITICAL.id(), (Object)CRITICAL);
        Registry.register((Registry)Registries.SOUND_EVENT, (Identifier)HELLO.id(), (Object)HELLO);
    }

    public static void playSound(SoundEvent sound) {
        boolean guiSound;
        if (PlayerInteractionHelper.nullCheck()) {
            return;
        }
        boolean bl = guiSound = sound == OPEN_GUI || sound == CLOSE_GUI || sound == CATEGORY_CLICK || sound == SETTING_INTERACT || sound == NOTIF || sound == CRITICAL || sound == HELLO;
        if (guiSound) {
            mc.getSoundManager().play((SoundInstance)PositionedSoundInstance.ui((SoundEvent)sound, (float)1.4f));
            return;
        }
    }

    private SoundManager() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
