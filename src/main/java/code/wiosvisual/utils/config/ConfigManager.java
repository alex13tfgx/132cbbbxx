/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.config;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.display.hud.Notifications;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.utils.client.managers.file.exception.FileLoadException;
import code.wiosvisual.utils.client.managers.file.exception.FileSaveException;
import code.wiosvisual.utils.client.sound.SoundManager;
import java.io.File;
import java.util.ArrayList;

public class ConfigManager {
    private static final String CONFIG_EXTENSION = ".json";
    /** \u041a\u043e\u043d\u0444\u0438\u0433, \u043a\u043e\u0442\u043e\u0440\u044b\u0439 \u0441\u0435\u0439\u0447\u0430\u0441 \u043f\u0440\u0438\u043c\u0435\u043d\u0451\u043d (\u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043d \u0438\u043b\u0438 \u0442\u043e\u043b\u044c\u043a\u043e \u0447\u0442\u043e \u0441\u043e\u0445\u0440\u0430\u043d\u0451\u043d) \u2014 \u0434\u043b\u044f UI (\u00ab\u0410\u043a\u0442\u0438\u0432\u043d\u043e\u00bb). */
    private static String activeConfig = "default";

    public static String getActiveConfig() {
        return activeConfig;
    }

    public static boolean saveConfig(String configName) {
        try {
            Wiosvisual.getInstance().getFileController().saveFile(configName + CONFIG_EXTENSION);
            System.out.println("Config saved: " + configName);
            activeConfig = configName;
            if (!configName.equals("default")) {
                Hud hud = Hud.getInstance();
                if (hud != null) {
                    SoundManager.playSound(SoundManager.ENABLE_MODULE);
                }
                Notifications.getInstance().addList("\u041a\u043e\u043d\u0444\u0438\u0433 \u00ab" + configName + "\u00bb \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d!", 2000, false);
            }
            return true;
        }
        catch (FileSaveException e) {
            System.err.println("Failed to save config: " + e.getMessage());
            return false;
        }
    }

    public static boolean loadConfig(String configName) {
        try {
            Wiosvisual.getInstance().getFileController().loadFile(configName + CONFIG_EXTENSION);
            activeConfig = configName;
            return true;
        }
        catch (FileLoadException e) {
            System.err.println("Failed to load config: " + e.getMessage());
            return false;
        }
    }

    public static void saveDefaultConfig() {
        try {
            Wiosvisual.getInstance().getFileController().saveFile("default.json");
        }
        catch (FileSaveException e) {
            System.err.println("Failed to auto-save default config: " + e.getMessage());
        }
    }

    public static String[] getAvailableConfigs() {
        File[] configFiles = Wiosvisual.getInstance().getClientInfoProvider().configsDir().listFiles();
        if (configFiles == null) {
            return new String[0];
        }
        ArrayList<String> configs = new ArrayList<String>();
        for (File configFile : configFiles) {
            if (!configFile.isFile() || !configFile.getName().endsWith(CONFIG_EXTENSION)) continue;
            String configName = configFile.getName().replace(CONFIG_EXTENSION, "");
            configs.add(configName);
        }
        return configs.toArray(new String[0]);
    }

    public static boolean deleteConfig(String configName) {
        try {
            File configFile = new File(Wiosvisual.getInstance().getClientInfoProvider().configsDir(), configName + CONFIG_EXTENSION);
            if (configFile.exists()) {
                boolean deleted = configFile.delete();
                if (deleted) {
                    Hud hud = Hud.getInstance();
                    if (hud != null) {
                        SoundManager.playSound(SoundManager.DISABLE_MODULE);
                    }
                    Notifications.getInstance().addList("\u041a\u043e\u043d\u0444\u0438\u0433 \u00ab" + configName + "\u00bb \u0443\u0434\u0430\u043b\u0435\u043d!", 2000, true);
                }
                return deleted;
            }
            return false;
        }
        catch (Exception e) {
            System.err.println("Failed to delete config: " + e.getMessage());
            return false;
        }
    }

    public static boolean configExists(String configName) {
        File configFile = new File(Wiosvisual.getInstance().getClientInfoProvider().configsDir(), configName + CONFIG_EXTENSION);
        return configFile.exists();
    }

    public static void loadDefaultConfig() {
        if (ConfigManager.configExists("default")) {
            ConfigManager.loadConfig("default");
            System.out.println("Default config loaded");
        } else {
            System.out.println("Default config not found, creating new one");
            ConfigManager.saveDefaultConfig();
        }
    }

    public static boolean resetConfig() {
        try {
            Wiosvisual.getInstance().getFileController().resetConfig();
            return true;
        }
        catch (FileLoadException | FileSaveException e) {
            System.err.println("Failed to reset config: " + e.getMessage());
            return false;
        }
    }
}
