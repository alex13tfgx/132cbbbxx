package code.wiosvisual.utils.client.discord;

import code.wiosvisual.utils.client.logs.Logger;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Аватарка из Discord для ClickGUI. Логин берём не у пользователя, а у самого
 * Discord: при хендшейке IPC он присылает в событии READY профиль владельца
 * запущенного клиента, оттуда — id и хеш аватарки, а картинку тянем с CDN.
 * Если Discord не запущен или аватарки нет, {@link #texture()} вернёт null,
 * и интерфейс просто нарисует запасную иконку.
 */
public final class DiscordAvatar {
    private static final Identifier TEXTURE_ID = Identifier.of("wiosvisual", "textures/discord_avatar");
    private static final String CDN = "https://cdn.discordapp.com/avatars/%s/%s.png?size=128";
    private static final int TIMEOUT_MS = 8000;

    private static volatile String loadedKey = "";
    private static volatile boolean loading;
    private static volatile boolean ready;
    private static volatile String userName = "";

    private DiscordAvatar() {
    }

    /** Текстура аватарки или null, пока она не загружена. */
    public static Identifier texture() {
        return ready ? TEXTURE_ID : null;
    }

    /** Ник владельца Discord — им подписывается кнопка профиля. */
    public static String userName() {
        return userName;
    }

    /**
     * Вызывается из {@link DiscordIPC} на событие READY. Повторные вызовы с тем же
     * пользователем ничего не делают: Discord шлёт READY при каждом переподключении.
     */
    public static void onUser(String id, String avatarHash, String name) {
        if (name != null && !name.isBlank()) {
            userName = name;
        }
        if (id == null || id.isBlank() || avatarHash == null || avatarHash.isBlank()) {
            return;
        }
        String key = id + ":" + avatarHash;
        if (key.equals(loadedKey) || loading) {
            return;
        }
        loadedKey = key;
        loading = true;
        Thread worker = new Thread(() -> download(id, avatarHash), "wios-discord-avatar");
        worker.setDaemon(true);
        worker.start();
    }

    private static void download(String id, String avatarHash) {
        try {
            String url = String.format(Locale.ROOT, CDN, id, avatarHash);
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "Wiosvisual");
            byte[] bytes;
            try (InputStream stream = connection.getInputStream()) {
                bytes = stream.readAllBytes();
            }
            // Регистрировать текстуру можно только в потоке рендера.
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null) {
                loading = false;
                return;
            }
            mc.execute(() -> register(bytes));
        } catch (Exception error) {
            Logger.warn("[DiscordAvatar] не удалось загрузить аватарку: " + error.getMessage());
            loadedKey = "";
            loading = false;
        }
    }

    private static void register(byte[] bytes) {
        try (InputStream stream = new ByteArrayInputStream(bytes)) {
            NativeImage image = NativeImage.read(stream);
            MinecraftClient.getInstance().getTextureManager().registerTexture(TEXTURE_ID, new NativeImageBackedTexture(() -> "wios", image));
            ready = true;
        } catch (Exception error) {
            Logger.warn("[DiscordAvatar] картинка не читается: " + error.getMessage());
            loadedKey = "";
        } finally {
            loading = false;
        }
    }
}
