package code.wiosvisual.utils.client.net;

import java.util.Locale;

/** Роли пользователя. Порядок объявления задаёт приоритет сортировки в списках. */
public enum ClientRole {
    DEV("Dev", 0xFFFF4D6D, 100),
    OWNER("Owner", 0xFFFFB020, 90),
    YOUTUBE("YouTube", 0xFFFF3D3D, 60),
    TIKTOK("TikTok", 0xFF25F4EE, 55),
    USER("User", 0xFF9AA3B2, 10);

    private final String label;
    private final int color;
    private final int weight;

    ClientRole(String label, int color, int weight) {
        this.label = label;
        this.color = color;
        this.weight = weight;
    }

    public String label() {
        return this.label;
    }

    public int color() {
        return this.color;
    }

    public int weight() {
        return this.weight;
    }

    public boolean isStaff() {
        return this == DEV || this == OWNER;
    }

    public static ClientRole of(String raw) {
        if (raw == null || raw.isBlank()) {
            return USER;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (ClientRole role : values()) {
            if (role.name().equals(normalized)) {
                return role;
            }
        }
        return USER;
    }
}
