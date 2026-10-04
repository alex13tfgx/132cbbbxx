package guardlisten;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;

/** Локальные аккаунты лаунчера (пароли хранятся только в виде PBKDF2-хэша). */
final class Accounts {
    static final class User {
        final String login, role;
        User(String login, String role) { this.login = login; this.role = role; }
    }

    private final Path file;
    private Map<String, Object> data = new LinkedHashMap<>();

    Accounts(Path root) {
        file = root.resolve("accounts.json");
        try {
            if (Files.exists(file)) data = Json.obj(Json.parse(Files.readString(file)));
        } catch (Exception ignored) { }
        data.putIfAbsent("users", new ArrayList<>());
    }

    private List<Object> users() { return Json.list(data.get("users")); }

    private Map<String, Object> find(String login) {
        for (Object o : users()) {
            Map<String, Object> m = Json.obj(o);
            if (login.equalsIgnoreCase(Json.str(m, "login"))) return m;
        }
        return null;
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, Json.write(data));
        } catch (Exception ignored) { }
    }

    /** @return текст ошибки или null при успехе */
    String register(String login, String pass, String pass2) {
        login = login.trim();
        if (login.length() < 3) return "Логин слишком короткий (минимум 3 символа)";
        if (pass.length() < 6) return "Пароль слишком короткий (минимум 6 символов)";
        if (!pass.equals(pass2)) return "Пароли не совпадают";
        if (find(login) != null) return "Такой аккаунт уже существует";
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("login", login);
        m.put("salt", Base64.getEncoder().encodeToString(salt));
        m.put("hash", hash(pass, salt));
        m.put("role", users().isEmpty() ? "Administrator" : "User");
        users().add(m);
        data.put("session", login);
        save();
        return null;
    }

    String login(String login, String pass) {
        Map<String, Object> m = find(login.trim());
        if (m == null) return "Аккаунт не найден";
        byte[] salt = Base64.getDecoder().decode(Json.str(m, "salt"));
        if (!hash(pass, salt).equals(Json.str(m, "hash"))) return "Неверный пароль";
        data.put("session", Json.str(m, "login"));
        save();
        return null;
    }

    User session() {
        String s = Json.str(data, "session");
        Map<String, Object> m = s == null ? null : find(s);
        return m == null ? null : new User(Json.str(m, "login"), Json.str(m, "role"));
    }

    void logout() { data.remove("session"); save(); }

    boolean hasUsers() { return !users().isEmpty(); }

    private static String hash(String pass, byte[] salt) {
        try {
            byte[] h = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(new PBEKeySpec(pass.toCharArray(), salt, 120_000, 256)).getEncoded();
            return Base64.getEncoder().encodeToString(h);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
