/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.features.impl.movement.autoMystGlider;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventStatusTracker {
    private static volatile long eventStartTime = -1L;
    private static volatile String eventStatus = null;
    private static volatile boolean eventInProgress = false;
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)\\s*(?:\u043c\u0438\u043d|\u043c\u0438\u043d\u0443\u0442|m|min)\\s*(?:\u0438|\u0438)?\\s*(\\d+)?\\s*(?:\u0441\u0435\u043a|\u0441\u0435\u043a\u0443\u043d\u0434|s|sec)?");
    private static final Pattern SECONDS_PATTERN = Pattern.compile("(\\d+)\\s*(?:\u0441\u0435\u043a|\u0441\u0435\u043a\u0443\u043d\u0434|s|sec)");

    public static void updateEventStatus(String message) {
        long delayMs;
        if (message == null || message.isEmpty()) {
            return;
        }
        String lower = message.toLowerCase();
        if (lower.contains("\u0438\u0432\u0435\u043d\u0442 \u0443\u0436\u0435 \u043d\u0430\u0447\u0430\u043b\u0441\u044f") || lower.contains("event already started")) {
            eventInProgress = true;
            eventStatus = "\u0418\u0432\u0435\u043d\u0442 \u0443\u0436\u0435 \u0438\u0434\u0435\u0442";
            eventStartTime = -1L;
            return;
        }
        if ((lower.contains("\u0447\u0435\u0440\u0435\u0437") || lower.contains("in")) && (delayMs = EventStatusTracker.parseDelay(message)) > 0L) {
            eventInProgress = false;
            eventStartTime = System.currentTimeMillis() + delayMs;
            eventStatus = message;
            return;
        }
    }

    private static long parseDelay(String message) {
        Matcher matcher = TIME_PATTERN.matcher(message);
        if (matcher.find()) {
            int minutes = Integer.parseInt(matcher.group(1));
            int seconds = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
            return ((long)minutes * 60L + (long)seconds) * 1000L;
        }
        matcher = SECONDS_PATTERN.matcher(message);
        if (matcher.find()) {
            int seconds = Integer.parseInt(matcher.group(1));
            return (long)seconds * 1000L;
        }
        return -1L;
    }

    public static String getDisplayStatus() {
        if (eventInProgress) {
            return "\u0418\u0432\u0435\u043d\u0442 \u0443\u0436\u0435 \u0438\u0434\u0435\u0442";
        }
        if (eventStartTime > 0L) {
            long remaining = eventStartTime - System.currentTimeMillis();
            if (remaining <= 0L) {
                eventInProgress = true;
                eventStartTime = -1L;
                return "\u0418\u0432\u0435\u043d\u0442 \u0443\u0436\u0435 \u0438\u0434\u0435\u0442";
            }
            long seconds = remaining / 1000L;
            long minutes = seconds / 60L;
            long secs = seconds % 60L;
            if (minutes > 0L) {
                return String.format("\u0418\u0432\u0435\u043d\u0442 \u0447\u0435\u0440\u0435\u0437 %d:%02d", minutes, secs);
            }
            return String.format("\u0418\u0432\u0435\u043d\u0442 \u0447\u0435\u0440\u0435\u0437 %ds", secs);
        }
        return null;
    }

    public static void reset() {
        eventStartTime = -1L;
        eventStatus = null;
        eventInProgress = false;
    }

    public static boolean hasActiveStatus() {
        if (eventInProgress) {
            return true;
        }
        if (eventStartTime > 0L) {
            long remaining = eventStartTime - System.currentTimeMillis();
            return remaining > 0L;
        }
        return false;
    }
}
