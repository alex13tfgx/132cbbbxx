/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package code.wiosvisual.utils.theme;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.display.hud.Notifications;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.sound.SoundManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;

public class ThemeManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String THEME_EXTENSION = ".json";
    private static final String DEFAULT_THEME_NAME = "default_dark";
    private static final long THEME_TRANSITION_DURATION_MS = 520L;
    private static boolean colorAnimationsEnabled = false;
    private static final String PRIMARY_COLOR_KEY = "\u041f\u0435\u0440\u0432\u044b\u0439 \u0446\u0432\u0435\u0442";
    private static final String SECONDARY_COLOR_KEY = "\u0412\u0442\u043e\u0440\u043e\u0439 \u0446\u0432\u0435\u0442";
    private static final String BACKGROUND_GUI_KEY = "\u0426\u0432\u0435\u0442 \u0437\u0430\u0434\u043d\u0435\u0433\u043e \u0444\u043e\u043d\u0430";
    private static final String OFF_MODULE_COLOR_KEY = "\u0426\u0432\u0435\u0442 \u0432\u044b\u043a\u043b. \u043c\u043e\u0434\u0443\u043b\u0435\u0439";
    private static final String BACKGROUND_SETTINGS_KEY = "\u0426\u0432\u0435\u0442 \u043d\u0430\u0441\u0442\u0440\u043e\u0435\u043a \u043c\u043e\u0434\u0443\u043b\u044f";
    private static final String TEXT_COLOR_KEY = "\u0426\u0432\u0435\u0442 \u0442\u0435\u043a\u0441\u0442\u0430";
    private static final String LEGACY_PRIMARY_COLOR_KEY = "\u0420\u045f\u0420\u00b5\u0421\u0402\u0420\u0406\u0421\u2039\u0420\u2116 \u0421\u2020\u0420\u0406\u0420\u00b5\u0421\u201a";
    private static final String LEGACY_SECONDARY_COLOR_KEY = "\u0420\u2019\u0421\u201a\u0420\u0455\u0421\u0402\u0420\u0455\u0420\u2116 \u0421\u2020\u0420\u0406\u0420\u00b5\u0421\u201a";
    private static final String LEGACY_BACKGROUND_GUI_KEY = "\u0420\u00a6\u0420\u0406\u0420\u00b5\u0421\u201a \u0420\u00b7\u0420\u00b0\u0420\u0491\u0420\u0405\u0420\u00b5\u0420\u0456\u0420\u0455 \u0421\u201e\u0420\u0455\u0420\u0405\u0420\u00b0";
    private static final String LEGACY_OFF_MODULE_COLOR_KEY = "\u0420\u00a6\u0420\u0406\u0420\u00b5\u0421\u201a \u0420\u0406\u0421\u2039\u0420\u0454\u0420\u00bb. \u0420\u0458\u0420\u0455\u0420\u0491\u0421\u0453\u0420\u00bb\u0420\u00b5\u0420\u2116";
    private static final String LEGACY_BACKGROUND_SETTINGS_KEY = "\u0420\u00a6\u0420\u0406\u0420\u00b5\u0421\u201a \u0420\u0405\u0420\u00b0\u0421\u0403\u0421\u201a\u0421\u0402\u0420\u0455\u0420\u00b5\u0420\u0454 \u0420\u0458\u0420\u0455\u0420\u0491\u0421\u0453\u0420\u00bb\u0421\u040f";
    private static final String LEGACY_TEXT_COLOR_KEY = "\u0420\u00a6\u0420\u0406\u0420\u00b5\u0421\u201a \u0421\u201a\u0420\u00b5\u0420\u0454\u0421\u0403\u0421\u201a\u0420\u00b0";
    private static final String LEGACY2_PRIMARY_COLOR_KEY = "\u0420\u00a0\u0421\u045f\u0420\u00a0\u0412\u00b5\u0420\u040e\u0420\u201a\u0420\u00a0\u0420\u2020\u0420\u040e\u0432\u0402\u2116\u0420\u00a0\u0432\u201e\u2013 \u0420\u040e\u0432\u0402\u00a0\u0420\u00a0\u0420\u2020\u0420\u00a0\u0412\u00b5\u0420\u040e\u0432\u0402\u0459";
    private static final String LEGACY2_SECONDARY_COLOR_KEY = "\u0420\u00a0\u0432\u0402\u2122\u0420\u040e\u0432\u0402\u0459\u0420\u00a0\u0421\u2022\u0420\u040e\u0420\u201a\u0420\u00a0\u0421\u2022\u0420\u00a0\u0432\u201e\u2013 \u0420\u040e\u0432\u0402\u00a0\u0420\u00a0\u0420\u2020\u0420\u00a0\u0412\u00b5\u0420\u040e\u0432\u0402\u0459";
    private static final String LEGACY2_BACKGROUND_GUI_KEY = "\u0420\u00a0\u0412\u00a6\u0420\u00a0\u0420\u2020\u0420\u00a0\u0412\u00b5\u0420\u040e\u0432\u0402\u0459 \u0420\u00a0\u0412\u00b7\u0420\u00a0\u0412\u00b0\u0420\u00a0\u0422\u2018\u0420\u00a0\u0420\u2026\u0420\u00a0\u0412\u00b5\u0420\u00a0\u0421\u2013\u0420\u00a0\u0421\u2022 \u0420\u040e\u0432\u0402\u045b\u0420\u00a0\u0421\u2022\u0420\u00a0\u0420\u2026\u0420\u00a0\u0412\u00b0";
    private static final String LEGACY2_OFF_MODULE_COLOR_KEY = "\u0420\u00a0\u0412\u00a6\u0420\u00a0\u0420\u2020\u0420\u00a0\u0412\u00b5\u0420\u040e\u0432\u0402\u0459 \u0420\u00a0\u0420\u2020\u0420\u040e\u0432\u0402\u2116\u0420\u00a0\u0421\u201d\u0420\u00a0\u0412\u00bb. \u0420\u00a0\u0421\u0098\u0420\u00a0\u0421\u2022\u0420\u00a0\u0422\u2018\u0420\u040e\u0421\u201c\u0420\u00a0\u0412\u00bb\u0420\u00a0\u0412\u00b5\u0420\u00a0\u0432\u201e\u2013";
    private static final String LEGACY2_BACKGROUND_SETTINGS_KEY = "\u0420\u00a0\u0412\u00a6\u0420\u00a0\u0420\u2020\u0420\u00a0\u0412\u00b5\u0420\u040e\u0432\u0402\u0459 \u0420\u00a0\u0420\u2026\u0420\u00a0\u0412\u00b0\u0420\u040e\u0420\u0453\u0420\u040e\u0432\u0402\u0459\u0420\u040e\u0420\u201a\u0420\u00a0\u0421\u2022\u0420\u00a0\u0412\u00b5\u0420\u00a0\u0421\u201d \u0420\u00a0\u0421\u0098\u0420\u00a0\u0421\u2022\u0420\u00a0\u0422\u2018\u0420\u040e\u0421\u201c\u0420\u00a0\u0412\u00bb\u0420\u040e\u0420\u040f";
    private static final String LEGACY2_TEXT_COLOR_KEY = "\u0420\u00a0\u0412\u00a6\u0420\u00a0\u0420\u2020\u0420\u00a0\u0412\u00b5\u0420\u040e\u0432\u0402\u0459 \u0420\u040e\u0432\u0402\u0459\u0420\u00a0\u0412\u00b5\u0420\u00a0\u0421\u201d\u0420\u040e\u0420\u0453\u0420\u040e\u0432\u0402\u0459\u0420\u00a0\u0412\u00b0";
    /** Single client accent: everything in the HUD/GUI derives from this one purple. */
    public static final int ACCENT_PURPLE = new Color(150, 80, 245, 255).getRGB();
    /** Pure black client background. */
    public static final int BLACK_GUI = new Color(0, 0, 0, 255).getRGB();
    /** Near-black panel background (settings windows). */
    public static final int BLACK_PANEL = new Color(12, 12, 13, 255).getRGB();
    /** Near-black background of disabled modules. */
    public static final int BLACK_MODULE = new Color(30, 30, 30, 255).getRGB();
    public static final ColorSetting primaryColor = new AnimatedColorSetting("\u041e\u0441\u043d\u043e\u0432\u043d\u043e\u0439 \u0446\u0432\u0435\u0442", "\u041e\u0441\u043d\u043e\u0432\u043d\u043e\u0439 \u0446\u0432\u0435\u0442 \u043a\u043b\u0438\u0435\u043d\u0442\u0430").setColor(ACCENT_PURPLE).presets(-9659651, -7569409, -23178, -33925);
    public static final ColorSetting secondaryColor = new MirroredColorSetting("\u0412\u0442\u043e\u0440\u0438\u0447\u043d\u044b\u0439 \u0446\u0432\u0435\u0442", "\u0412\u0442\u043e\u0440\u0438\u0447\u043d\u044b\u0439 \u0446\u0432\u0435\u0442 \u043a\u043b\u0438\u0435\u043d\u0442\u0430 (\u0437\u0435\u0440\u043a\u0430\u043b\u043e \u043e\u0441\u043d\u043e\u0432\u043d\u043e\u0433\u043e)");
    public static final ColorSetting BackgroundGui = new AnimatedColorSetting("\u0426\u0432\u0435\u0442 \u0437\u0430\u0434\u043d\u0435\u0433\u043e \u0444\u043e\u043d\u0430", "\u0426\u0432\u0435\u0442 \u0437\u0430\u0434\u043d\u0435\u0433\u043e \u0444\u043e\u043d\u0430 \u0432 GUI").setColor(BLACK_GUI);
    public static final ColorSetting offModuleColor = new AnimatedColorSetting("\u0426\u0432\u0435\u0442 \u0432\u044b\u043a\u043b. \u043c\u043e\u0434\u0443\u043b\u0435\u0439", "\u0426\u0432\u0435\u0442 \u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d\u043d\u044b\u0445 \u043c\u043e\u0434\u0443\u043b\u0435\u0439 \u0432 GUI").setColor(BLACK_MODULE);
    public static final ColorSetting BackgroundSettings = new AnimatedColorSetting("\u0426\u0432\u0435\u0442 \u043d\u0430\u0441\u0442\u0440\u043e\u0435\u043a \u043c\u043e\u0434\u0443\u043b\u044f", "\u0426\u0432\u0435\u0442 \u0444\u043e\u043d\u0430 \u043d\u0430\u0441\u0442\u0440\u043e\u0435\u043a \u043c\u043e\u0434\u0443\u043b\u044f").setColor(BLACK_PANEL);
    public static final ColorSetting textColor = new AnimatedColorSetting("\u0426\u0432\u0435\u0442 \u0442\u0435\u043a\u0441\u0442\u0430", "\u041e\u0441\u043d\u043e\u0432\u043d\u043e\u0439 \u0446\u0432\u0435\u0442 \u0442\u0435\u043a\u0441\u0442\u0430 \u0432 GUI").setColor(new Color(255, 255, 255, 255).getRGB());
    private static String currentThemeName = "default_dark";
    private static boolean liquidGlassCustomMode = false;
    private static float liquidGlassGlobalAlphaMin = 0.85f;
    private static float liquidGlassBaseAlpha = 0.97f;
    private static float liquidGlassFresnelAlpha = 1.0f;
    private static float liquidGlassFresnelMix = 0.08f;
    private static float liquidGlassSmoothness = 0.62f;
    private static float liquidGlassDistortStrength = 0.016f;
    private static float liquidGlassCornerSmoothness = 2.0f;
    public static final SliderSettings liquidGlassGlobalAlphaMinSetting = new SliderSettings("\u041c\u0438\u043d. \u043f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c", "\u041c\u0438\u043d\u0438\u043c\u0430\u043b\u044c\u043d\u0430\u044f \u043e\u0431\u0449\u0430\u044f \u043f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c \u0436\u0438\u0434\u043a\u043e\u0433\u043e \u0441\u0442\u0435\u043a\u043b\u0430").setValue(liquidGlassGlobalAlphaMin).range(0.5f, 1.0f).step(0.01f);
    public static final SliderSettings liquidGlassBaseAlphaSetting = new SliderSettings("Alpha", "\u0411\u0430\u0437\u043e\u0432\u0430\u044f \u043f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c \u0436\u0438\u0434\u043a\u043e\u0433\u043e \u0441\u0442\u0435\u043a\u043b\u0430").setValue(liquidGlassBaseAlpha).range(0.5f, 1.0f).step(0.01f);
    public static final SliderSettings liquidGlassFresnelAlphaSetting = new SliderSettings("Edge Alpha", "\u041f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c \u0431\u043b\u0438\u043a\u0430 \u043f\u043e \u043a\u0440\u0430\u044f\u043c").setValue(liquidGlassFresnelAlpha).range(0.5f, 1.0f).step(0.01f);
    public static final SliderSettings liquidGlassFresnelMixSetting = new SliderSettings("Edge Mix", "\u0421\u0438\u043b\u0430 \u0441\u0432\u0435\u0442\u043b\u043e\u0433\u043e \u0431\u043b\u0438\u043a\u0430 \u043f\u043e \u043a\u0440\u0430\u044f\u043c").setValue(liquidGlassFresnelMix).range(0.0f, 0.6f).step(0.01f);
    public static final SliderSettings liquidGlassSmoothnessSetting = new SliderSettings("Smooth", "\u041c\u044f\u0433\u043a\u043e\u0441\u0442\u044c \u043a\u0440\u0430\u0435\u0432 \u0436\u0438\u0434\u043a\u043e\u0433\u043e \u0441\u0442\u0435\u043a\u043b\u0430").setValue(liquidGlassSmoothness).range(0.1f, 1.0f).step(0.01f);
    public static final SliderSettings liquidGlassDistortStrengthSetting = new SliderSettings("Distort", "\u0421\u0438\u043b\u0430 \u0438\u0441\u043a\u0430\u0436\u0435\u043d\u0438\u044f \u0436\u0438\u0434\u043a\u043e\u0433\u043e \u0441\u0442\u0435\u043a\u043b\u0430").setValue(liquidGlassDistortStrength).range(0.0f, 0.06f).step(0.001f);
    public static final SliderSettings liquidGlassCornerSmoothnessSetting = new SliderSettings("Corners", "\u0421\u0433\u043b\u0430\u0436\u0438\u0432\u0430\u043d\u0438\u0435 \u0443\u0433\u043b\u043e\u0432 \u0436\u0438\u0434\u043a\u043e\u0433\u043e \u0441\u0442\u0435\u043a\u043b\u0430").setValue(liquidGlassCornerSmoothness).range(1.0f, 10.0f).step(0.1f);


    /**
     * Светлый оттенок основного цвета клиента. Клиент настраивается ОДНИМ цветом,
     * а второй стоп градиента выводится отсюда — так HUD остаётся объёмным,
     * но настройка по-прежнему одна. Альфа исходного цвета сохраняется.
     */
    public static int accentShade(int color) {
        int alpha = color >> 24 & 0xFF;
        float[] hsb = Color.RGBtoHSB(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, null);
        float saturation = ThemeManager.clampf(hsb[1] * 0.74f, 0.0f, 1.0f);
        float brightness = ThemeManager.clampf(hsb[2] * 1.16f + 0.10f, 0.0f, 1.0f);
        return alpha << 24 | Color.HSBtoRGB(hsb[0], saturation, brightness) & 0xFFFFFF;
    }

    /** Светлый оттенок текущего основного цвета — второй стоп всех градиентов клиента. */
    public static int accentShade() {
        return ThemeManager.accentShade(primaryColor.getColor());
    }

    public static File getThemeDir() {
        try {
            File filesDir = Wiosvisual.getInstance().getClientInfoProvider().filesDir();
            File themeDir = new File(filesDir, "themes");
            if (!themeDir.exists()) {
                themeDir.mkdirs();
            }
            return themeDir;
        }
        catch (Exception e) {
            return new File("Wiosvisual/files/themes");
        }
    }

    public static boolean isLiquidGlassTheme() {
        return false;
    }

    public static String getCurrentThemeName() {
        return currentThemeName;
    }

    private static void initializeDefaultThemes() {
        ThemeManager.createDefault_darkTheme();
    }

    private static void createDefault_darkTheme() {
        try {
            JsonObject theme = new JsonObject();
            theme.addProperty("name", DEFAULT_THEME_NAME);
            theme.addProperty("version", "1.0");
            theme.addProperty("timestamp", (Number)System.currentTimeMillis());
            JsonObject colors = new JsonObject();
            colors.addProperty(PRIMARY_COLOR_KEY, (Number)ACCENT_PURPLE);
            colors.addProperty(SECONDARY_COLOR_KEY, (Number)ACCENT_PURPLE);
            colors.addProperty(BACKGROUND_GUI_KEY, (Number)BLACK_GUI);
            colors.addProperty(OFF_MODULE_COLOR_KEY, (Number)BLACK_MODULE);
            colors.addProperty(BACKGROUND_SETTINGS_KEY, (Number)BLACK_PANEL);
            colors.addProperty(TEXT_COLOR_KEY, (Number)new Color(255, 255, 255, 255).getRGB());
            theme.add("colors", (JsonElement)colors);
            theme.add("liquidGlass", (JsonElement)ThemeManager.createLiquidGlassJson());
            File themeFile = new File(ThemeManager.getThemeDir(), "default_dark.json");
            String fileName = themeFile.getAbsolutePath();
            try (FileWriter writer = new FileWriter(fileName);){
                GSON.toJson((JsonElement)theme, (Appendable)writer);
            }
            System.out.println("Default theme created");
        }
        catch (IOException e) {
            System.err.println("Failed to create default theme: " + e.getMessage());
        }
    }

    private static void loadDefaultTheme() {
        if (ThemeManager.themeExists("default")) {
            ThemeManager.loadTheme("default");
            System.out.println("Default theme loaded");
        } else if (ThemeManager.themeExists(DEFAULT_THEME_NAME)) {
            ThemeManager.loadTheme(DEFAULT_THEME_NAME);
            System.out.println("Default dark theme loaded");
        }
    }

    public static boolean saveTheme(String themeName) {
        try {
            JsonObject theme = new JsonObject();
            theme.addProperty("name", themeName);
            theme.addProperty("version", "1.0");
            theme.addProperty("timestamp", (Number)System.currentTimeMillis());
            JsonObject colors = new JsonObject();
            colors.addProperty(PRIMARY_COLOR_KEY, (Number)primaryColor.getColor());
            colors.addProperty(SECONDARY_COLOR_KEY, (Number)secondaryColor.getColor());
            colors.addProperty(BACKGROUND_GUI_KEY, (Number)BackgroundGui.getColor());
            colors.addProperty(OFF_MODULE_COLOR_KEY, (Number)offModuleColor.getColor());
            colors.addProperty(BACKGROUND_SETTINGS_KEY, (Number)BackgroundSettings.getColor());
            colors.addProperty(TEXT_COLOR_KEY, (Number)textColor.getColor());
            theme.add("colors", (JsonElement)colors);
            theme.add("liquidGlass", (JsonElement)ThemeManager.createLiquidGlassJson());
            File themeFile = new File(ThemeManager.getThemeDir(), themeName + THEME_EXTENSION);
            try (FileWriter writer = new FileWriter(themeFile);){
                GSON.toJson((JsonElement)theme, (Appendable)writer);
            }
            if (!themeName.equals("default")) {
                SoundManager.playSound(SoundManager.DISABLE_MODULE);
                Notifications.getInstance().addList("\u0420\u045e\u0420\u00b5\u0420\u0458\u0420\u00b0 \u0412\u00ab" + themeName + "\u0412\u00bb \u0421\u0403\u0420\u0455\u0421\u2026\u0421\u0402\u0420\u00b0\u0420\u0405\u0420\u00b5\u0420\u0405\u0420\u00b0!", 2000, false);
            }
            return true;
        }
        catch (IOException e) {
            System.err.println("Failed to save theme: " + e.getMessage());
            return false;
        }
    }

    public static boolean loadTheme(String themeName) {
        try {
            JsonObject theme;
            String resolvedThemeName = ThemeManager.normalizeThemeName(themeName);
            File themeFile = new File(ThemeManager.getThemeDir(), resolvedThemeName + THEME_EXTENSION);
            if (!themeFile.exists()) {
                System.err.println("Theme file not found: " + resolvedThemeName);
                return false;
            }
            String content = Files.readString(themeFile.toPath());
            if (content == null) {
                System.err.println("Theme file is empty: " + resolvedThemeName);
                return false;
            }
            String trimmed = content.trim();
            if (trimmed.isEmpty() || !trimmed.startsWith("{")) {
                System.err.println("Theme file is not valid JSON: " + resolvedThemeName);
                return false;
            }
            try {
                theme = JsonParser.parseString((String)trimmed).getAsJsonObject();
            }
            catch (Exception parseException) {
                System.err.println("Failed to parse theme JSON (" + resolvedThemeName + "): " + parseException.getMessage());
                return false;
            }
            if (theme.has("colors")) {
                JsonObject colors = theme.getAsJsonObject("colors");
                ThemeManager.applyThemeColor(colors, primaryColor, PRIMARY_COLOR_KEY, LEGACY_PRIMARY_COLOR_KEY, LEGACY2_PRIMARY_COLOR_KEY);
                ThemeManager.applyThemeColor(colors, BackgroundGui, BACKGROUND_GUI_KEY, LEGACY_BACKGROUND_GUI_KEY, LEGACY2_BACKGROUND_GUI_KEY);
                ThemeManager.applyThemeColor(colors, offModuleColor, OFF_MODULE_COLOR_KEY, LEGACY_OFF_MODULE_COLOR_KEY, LEGACY2_OFF_MODULE_COLOR_KEY);
                ThemeManager.applyThemeColor(colors, BackgroundSettings, BACKGROUND_SETTINGS_KEY, LEGACY_BACKGROUND_SETTINGS_KEY, LEGACY2_BACKGROUND_SETTINGS_KEY);
                ThemeManager.applyThemeColor(colors, textColor, TEXT_COLOR_KEY, LEGACY_TEXT_COLOR_KEY, LEGACY2_TEXT_COLOR_KEY);
            }
            if (theme.has("liquidGlass")) {
                JsonObject glass = theme.getAsJsonObject("liquidGlass");
                if (glass.has("customMode")) {
                    liquidGlassCustomMode = glass.get("customMode").getAsBoolean();
                }
                if (glass.has("globalAlphaMin")) {
                    liquidGlassGlobalAlphaMin = ThemeManager.clampf(glass.get("globalAlphaMin").getAsFloat(), 0.5f, 1.0f);
                }
                if (glass.has("baseAlpha")) {
                    liquidGlassBaseAlpha = ThemeManager.clampf(glass.get("baseAlpha").getAsFloat(), 0.5f, 1.0f);
                }
                if (glass.has("fresnelAlpha")) {
                    liquidGlassFresnelAlpha = ThemeManager.clampf(glass.get("fresnelAlpha").getAsFloat(), 0.5f, 1.0f);
                }
                if (glass.has("fresnelMix")) {
                    liquidGlassFresnelMix = ThemeManager.clampf(glass.get("fresnelMix").getAsFloat(), 0.0f, 0.6f);
                }
                if (glass.has("smoothness")) {
                    liquidGlassSmoothness = ThemeManager.clampf(glass.get("smoothness").getAsFloat(), 0.1f, 1.0f);
                }
                if (glass.has("distortStrength")) {
                    liquidGlassDistortStrength = ThemeManager.clampf(glass.get("distortStrength").getAsFloat(), 0.0f, 0.06f);
                }
                if (glass.has("cornerSmoothness")) {
                    liquidGlassCornerSmoothness = ThemeManager.clampf(glass.get("cornerSmoothness").getAsFloat(), 1.0f, 10.0f);
                }
            } else {
                ThemeManager.resetLiquidGlassDefaults();
            }
            currentThemeName = resolvedThemeName;
            return true;
        }
        catch (IOException e) {
            System.err.println("Failed to load theme: " + e.getMessage());
            return false;
        }
    }

    public static void saveDefaultTheme() {
        ThemeManager.saveTheme("default");
    }

    private static void applyThemeColor(JsonObject colors, ColorSetting setting, String ... keys) {
        for (String key : keys) {
            JsonElement value = colors.get(key);
            if (value == null || value.isJsonNull()) continue;
            setting.setColor(value.getAsInt());
            return;
        }
    }

    public static String[] getAvailableThemes() {
        File themeDir = ThemeManager.getThemeDir();
        if (!themeDir.exists() || !themeDir.isDirectory()) {
            return new String[0];
        }
        File[] files = themeDir.listFiles((dir, name) -> name.endsWith(THEME_EXTENSION));
        if (files == null) {
            return new String[0];
        }
        ArrayList<ThemeInfo> themes = new ArrayList<ThemeInfo>();
        for (File file : files) {
            String fileName = file.getName();
            String themeName = fileName.substring(0, fileName.length() - THEME_EXTENSION.length());
            if (themeName.equals("default") || themeName.equals("default_light") || themeName.equals("default_liquidglass")) continue;
            long timestamp = ThemeManager.getThemeTimestamp(file);
            themes.add(new ThemeInfo(themeName, timestamp));
        }
        themes.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));
        return (String[])themes.stream().map(t -> t.name).toArray(String[]::new);
    }

    private static long getThemeTimestamp(File themeFile) {
        try {
            String content = Files.readString(themeFile.toPath());
            JsonObject theme = JsonParser.parseString((String)content).getAsJsonObject();
            if (theme.has("timestamp")) {
                return theme.get("timestamp").getAsLong();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return themeFile.lastModified();
    }

    public static boolean deleteTheme(String themeName) {
        if (themeName.equals(DEFAULT_THEME_NAME)) {
            System.out.println("Cannot delete default theme: " + themeName);
            return false;
        }
        try {
            File themeFile = new File(ThemeManager.getThemeDir(), themeName + THEME_EXTENSION);
            if (themeFile.exists()) {
                boolean deleted = themeFile.delete();
                if (deleted) {
                    System.out.println("Theme deleted: " + themeName);
                    SoundManager.playSound(SoundManager.DISABLE_MODULE);
                    Notifications.getInstance().addList("\u0420\u045e\u0420\u00b5\u0420\u0458\u0420\u00b0 \u0412\u00ab" + themeName + "\u0412\u00bb \u0421\u0453\u0420\u0491\u0420\u00b0\u0420\u00bb\u0420\u00b5\u0420\u0405\u0420\u00b0!", 2000, true);
                }
                return deleted;
            }
            return false;
        }
        catch (Exception e) {
            System.err.println("Failed to delete theme: " + e.getMessage());
            return false;
        }
    }

    public static boolean themeExists(String themeName) {
        File themeFile = new File(ThemeManager.getThemeDir(), ThemeManager.normalizeThemeName(themeName) + THEME_EXTENSION);
        return themeFile.exists();
    }

    private static String normalizeThemeName(String themeName) {
        if (themeName == null) {
            return DEFAULT_THEME_NAME;
        }
        if ("default_blur".equalsIgnoreCase(themeName) || "default_light".equalsIgnoreCase(themeName) || "default_liquidglass".equalsIgnoreCase(themeName)) {
            return DEFAULT_THEME_NAME;
        }
        return themeName;
    }

    public static void recreateDefaultThemes() {
        ThemeManager.createDefault_darkTheme();
        System.out.println("Default themes recreated with original colors");
    }

    public static boolean isLiquidGlassCustomMode() {
        return liquidGlassCustomMode;
    }

    public static void setLiquidGlassCustomMode(boolean value) {
        liquidGlassCustomMode = value;
    }

    public static float getLiquidGlassGlobalAlphaMin() {
        return liquidGlassGlobalAlphaMinSetting.getValue();
    }

    public static void setLiquidGlassGlobalAlphaMin(float value) {
        liquidGlassGlobalAlphaMin = ThemeManager.setSliderValue(liquidGlassGlobalAlphaMinSetting, value, 0.5f, 1.0f);
    }

    public static float getLiquidGlassBaseAlpha() {
        return liquidGlassBaseAlphaSetting.getValue();
    }

    public static void setLiquidGlassBaseAlpha(float value) {
        liquidGlassBaseAlpha = ThemeManager.setSliderValue(liquidGlassBaseAlphaSetting, value, 0.5f, 1.0f);
    }

    public static float getLiquidGlassFresnelAlpha() {
        return liquidGlassFresnelAlphaSetting.getValue();
    }

    public static void setLiquidGlassFresnelAlpha(float value) {
        liquidGlassFresnelAlpha = ThemeManager.setSliderValue(liquidGlassFresnelAlphaSetting, value, 0.5f, 1.0f);
    }

    public static float getLiquidGlassFresnelMix() {
        return liquidGlassFresnelMixSetting.getValue();
    }

    public static void setLiquidGlassFresnelMix(float value) {
        liquidGlassFresnelMix = ThemeManager.setSliderValue(liquidGlassFresnelMixSetting, value, 0.0f, 0.6f);
    }

    public static float getLiquidGlassSmoothness() {
        return liquidGlassSmoothnessSetting.getValue();
    }

    public static void setLiquidGlassSmoothness(float value) {
        liquidGlassSmoothness = ThemeManager.setSliderValue(liquidGlassSmoothnessSetting, value, 0.1f, 1.0f);
    }

    public static float getLiquidGlassDistortStrength() {
        return liquidGlassDistortStrengthSetting.getValue();
    }

    public static void setLiquidGlassDistortStrength(float value) {
        liquidGlassDistortStrength = ThemeManager.setSliderValue(liquidGlassDistortStrengthSetting, value, 0.0f, 0.06f);
    }

    public static float getLiquidGlassCornerSmoothness() {
        return liquidGlassCornerSmoothnessSetting.getValue();
    }

    public static void setLiquidGlassCornerSmoothness(float value) {
        liquidGlassCornerSmoothness = ThemeManager.setSliderValue(liquidGlassCornerSmoothnessSetting, value, 1.0f, 10.0f);
    }

    public static void resetLiquidGlassDefaults() {
        liquidGlassCustomMode = false;
        ThemeManager.setLiquidGlassGlobalAlphaMin(0.85f);
        ThemeManager.setLiquidGlassBaseAlpha(0.97f);
        ThemeManager.setLiquidGlassFresnelAlpha(1.0f);
        ThemeManager.setLiquidGlassFresnelMix(0.08f);
        ThemeManager.setLiquidGlassSmoothness(0.62f);
        ThemeManager.setLiquidGlassDistortStrength(0.016f);
        ThemeManager.setLiquidGlassCornerSmoothness(2.0f);
    }

    private static JsonObject createLiquidGlassJson() {
        JsonObject glass = new JsonObject();
        glass.addProperty("customMode", Boolean.valueOf(liquidGlassCustomMode));
        glass.addProperty("globalAlphaMin", (Number)Float.valueOf(ThemeManager.getLiquidGlassGlobalAlphaMin()));
        glass.addProperty("baseAlpha", (Number)Float.valueOf(ThemeManager.getLiquidGlassBaseAlpha()));
        glass.addProperty("fresnelAlpha", (Number)Float.valueOf(ThemeManager.getLiquidGlassFresnelAlpha()));
        glass.addProperty("fresnelMix", (Number)Float.valueOf(ThemeManager.getLiquidGlassFresnelMix()));
        glass.addProperty("smoothness", (Number)Float.valueOf(ThemeManager.getLiquidGlassSmoothness()));
        glass.addProperty("distortStrength", (Number)Float.valueOf(ThemeManager.getLiquidGlassDistortStrength()));
        glass.addProperty("cornerSmoothness", (Number)Float.valueOf(ThemeManager.getLiquidGlassCornerSmoothness()));
        return glass;
    }

    public static float getThemeTransitionProgress() {
        float progress = 1.0f;
        progress = Math.min(progress, ThemeManager.getColorTransitionProgress(primaryColor));
        progress = Math.min(progress, ThemeManager.getColorTransitionProgress(BackgroundGui));
        progress = Math.min(progress, ThemeManager.getColorTransitionProgress(offModuleColor));
        progress = Math.min(progress, ThemeManager.getColorTransitionProgress(BackgroundSettings));
        progress = Math.min(progress, ThemeManager.getColorTransitionProgress(textColor));
        return progress;
    }

    public static boolean isThemeTransitioning() {
        return ThemeManager.getThemeTransitionProgress() < 1.0f;
    }

    private static float getColorTransitionProgress(ColorSetting setting) {
        if (setting instanceof AnimatedColorSetting) {
            AnimatedColorSetting animated = (AnimatedColorSetting)setting;
            return animated.getTransitionProgress();
        }
        return 1.0f;
    }

    private static int blendColor(int from, int to, float progress) {
        int a = ThemeManager.lerpChannel(from >>> 24 & 0xFF, to >>> 24 & 0xFF, progress);
        int r = ThemeManager.lerpChannel(from >>> 16 & 0xFF, to >>> 16 & 0xFF, progress);
        int g = ThemeManager.lerpChannel(from >>> 8 & 0xFF, to >>> 8 & 0xFF, progress);
        int b = ThemeManager.lerpChannel(from & 0xFF, to & 0xFF, progress);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int lerpChannel(int from, int to, float progress) {
        return Math.round((float)from + (float)(to - from) * progress);
    }

    private static float easeOutCubic(float progress) {
        float inverted = 1.0f - ThemeManager.clampf(progress, 0.0f, 1.0f);
        return 1.0f - inverted * inverted * inverted;
    }

    private static float setSliderValue(SliderSettings setting, float value, float min, float max) {
        float clamped = ThemeManager.clampf(value, min, max);
        setting.setValue(clamped);
        return clamped;
    }

    private static float clampf(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    static {
        try {
            Files.createDirectories(ThemeManager.getThemeDir().toPath(), new FileAttribute[0]);
            ThemeManager.initializeDefaultThemes();
            ThemeManager.loadDefaultTheme();
            colorAnimationsEnabled = true;
        }
        catch (IOException e) {
            System.err.println("Failed to create theme directory: " + e.getMessage());
        }
    }

    private static class ThemeInfo {
        String name;
        long timestamp;

        ThemeInfo(String name, long timestamp) {
            this.name = name;
            this.timestamp = timestamp;
        }
    }

    private static class AnimatedColorSetting
    extends ColorSetting {
        private int startColor;
        private int targetColor;
        private long transitionStartedAt;

        private AnimatedColorSetting(String name, String description) {
            super(name, description);
        }

        @Override
        public int getColor() {
            if (!colorAnimationsEnabled || this.getTransitionProgress() >= 1.0f) {
                return super.getColor();
            }
            return ThemeManager.blendColor(this.startColor, this.targetColor, ThemeManager.easeOutCubic(this.getTransitionProgress()));
        }

        @Override
        public ColorSetting setColor(int color) {
            if (!colorAnimationsEnabled) {
                super.setColor(color);
                this.startColor = color;
                this.targetColor = color;
                this.transitionStartedAt = 0L;
                return this;
            }
            this.startColor = this.getColor();
            this.targetColor = color;
            this.transitionStartedAt = System.currentTimeMillis();
            super.setColor(color);
            return this;
        }

        private float getTransitionProgress() {
            if (this.transitionStartedAt <= 0L) {
                return 1.0f;
            }
            return ThemeManager.clampf((float)(System.currentTimeMillis() - this.transitionStartedAt) / 520.0f, 0.0f, 1.0f);
        }
    }

    /**
     * Client colour that is not editable on its own: it always reports {@link #primaryColor}, so the
     * whole HUD/GUI runs on a single accent. Module-owned {@link ColorSetting}s are untouched.
     */
    private static class MirroredColorSetting
    extends ColorSetting {
        private MirroredColorSetting(String name, String description) {
            super(name, description);
        }

        @Override
        public int getColor() {
            return ThemeManager.accentShade(primaryColor.getColor());
        }

        @Override
        public int getColorWithAlpha() {
            return ThemeManager.accentShade(primaryColor.getColorWithAlpha());
        }

        @Override
        public ColorSetting setColor(int color) {
            return this;
        }

        @Override
        public void setHue(float hue) {
        }

        @Override
        public void setSaturation(float saturation) {
        }

        @Override
        public void setBrightness(float brightness) {
        }

        @Override
        public void setAlpha(float alpha) {
        }

        @Override
        public float getHue() {
            return primaryColor.getHue();
        }

        @Override
        public float getSaturation() {
            return primaryColor.getSaturation();
        }

        @Override
        public float getBrightness() {
            return primaryColor.getBrightness();
        }

        @Override
        public float getAlpha() {
            return primaryColor.getAlpha();
        }

        @Override
        public int[] getPresets() {
            return primaryColor.getPresets();
        }
    }
}
