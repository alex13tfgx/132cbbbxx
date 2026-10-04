package code.wiosvisual.utils.client.net;

/** Строка списка «кто сейчас онлайн», как её отдаёт бэкенд. */
public record OnlineUser(String login, String displayName, ClientRole role, String ingameName, String server) {
    /** Ник аккаунта — тот же, что на сайте и в Discord. */
    public String nameForList() {
        if (this.displayName != null && !this.displayName.isBlank()) {
            return this.displayName;
        }
        return this.ingameName == null || this.ingameName.isBlank() ? this.login : this.ingameName;
    }
}
