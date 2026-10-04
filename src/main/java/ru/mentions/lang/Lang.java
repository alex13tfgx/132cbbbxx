package ru.mentions.lang;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Переключатель языка главного меню (RU/EN); выбор хранится в папке конфигов игры. */
public final class Lang {
    private static final Path LANG_FILE = Path.of("config", "mentions_language.txt");
    private static boolean english = readSaved();

    private Lang() {}

    public static boolean isEnglish() {
        return english;
    }

    public static String tag() {
        return english ? "EN" : "RU";
    }

    public static void toggle() {
        english = !english;
        try {
            Files.createDirectories(LANG_FILE.getParent());
            Files.writeString(LANG_FILE, english ? "EN" : "RU", StandardCharsets.UTF_8);
        }
        catch (Exception ignored) {
            // Язык просто не запомнится до следующего запуска.
        }
    }

    public static String pick(String ru, String en) {
        return english ? en : ru;
    }

    private static boolean readSaved() {
        try {
            return Files.exists(LANG_FILE) && Files.readString(LANG_FILE, StandardCharsets.UTF_8).trim().equalsIgnoreCase("EN");
        }
        catch (Exception e) {
            return false;
        }
    }
}
