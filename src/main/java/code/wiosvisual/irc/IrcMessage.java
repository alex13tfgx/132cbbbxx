/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.irc;

public record IrcMessage(String id, String senderName, String text, long timestamp, boolean direct, boolean outgoing, boolean mentioned) {
    public static IrcMessage outgoing(String id, String senderName, String text, long timestamp, boolean direct, boolean mentioned) {
        return new IrcMessage(id, senderName, text, timestamp, direct, true, mentioned);
    }

    public static IrcMessage incoming(String id, String senderName, String text, long timestamp, boolean direct, boolean mentioned) {
        return new IrcMessage(id, senderName, text, timestamp, direct, false, mentioned);
    }
}
