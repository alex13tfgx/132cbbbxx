package code.wiosvisual.features.impl.render.custommodels;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Мост к Figura через рефлексию: Wios не зависит от Figura при компиляции
 * и спокойно работает, если мод не установлен.
 */
public final class FiguraBridge {
    private static final Logger LOGGER = LogManager.getLogger("Wiosvisual");

    private static boolean initialized;
    private static boolean available;
    private static Method loadLocalAvatar;
    private static Method getLoadedAvatar;
    private static Method setAvatar;
    private static Method clearAvatars;
    private static Field avatarNbt;
    private static Field avatarLoaded;

    private FiguraBridge() {
    }

    public static boolean isAvailable() {
        init();
        return available;
    }

    private static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        if (!FabricLoader.getInstance().isModLoaded("figura")) {
            return;
        }
        try {
            Class<?> manager = Class.forName("org.figuramc.figura.avatar.AvatarManager");
            Class<?> avatar = Class.forName("org.figuramc.figura.avatar.Avatar");
            loadLocalAvatar = manager.getMethod("loadLocalAvatar", Path.class);
            getLoadedAvatar = manager.getMethod("getLoadedAvatar", UUID.class);
            setAvatar = manager.getMethod("setAvatar", UUID.class, NbtCompound.class);
            clearAvatars = manager.getMethod("clearAvatars", UUID.class);
            avatarNbt = avatar.getField("nbt");
            avatarLoaded = avatar.getField("loaded");
            available = true;
        } catch (Throwable error) {
            LOGGER.error("Figura found, but its API is incompatible", error);
        }
    }

    public static void loadLocal(Path path) {
        invoke(loadLocalAvatar, path);
    }

    public static void setAvatar(UUID uuid, NbtCompound nbt) {
        invoke(setAvatar, uuid, nbt);
    }

    /** Сбрасывает аватар игрока — Figura заново подтянет его родной аватар с бэкенда. */
    public static void clear(UUID uuid) {
        invoke(clearAvatars, uuid);
    }

    public static Object getAvatar(UUID uuid) {
        return invoke(getLoadedAvatar, uuid);
    }

    public static NbtCompound getNbt(Object avatar) {
        if (avatar == null || !isAvailable()) {
            return null;
        }
        try {
            return avatarLoaded.getBoolean(avatar) ? (NbtCompound) avatarNbt.get(avatar) : null;
        } catch (Throwable error) {
            return null;
        }
    }

    private static Object invoke(Method method, Object... args) {
        if (!isAvailable()) {
            return null;
        }
        try {
            return method.invoke(null, args);
        } catch (Throwable error) {
            LOGGER.error("Figura call {} failed", method.getName(), error);
            return null;
        }
    }
}
