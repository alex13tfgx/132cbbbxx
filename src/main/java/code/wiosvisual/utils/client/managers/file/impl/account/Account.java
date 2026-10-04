/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.client.managers.file.impl.account;

public class Account {
    public String name;
    public boolean starred;
    public float starAnim;
    public boolean active;
    boolean premium;
    String refreshToken;
    public String uuid;
    String accessToken;
    /** Added-on timestamp shown by the ported alt manager. Null for entries saved before it existed. */
    public String date;

    public Account(String name, boolean starred, boolean premium, String refreshToken, String uuid, String accessToken) {
        this.name = name;
        this.starred = starred;
        this.premium = premium;
        this.refreshToken = refreshToken;
        this.uuid = uuid;
        this.accessToken = accessToken;
        this.starAnim = starred ? 1.0f : 0.0f;
    }
}
