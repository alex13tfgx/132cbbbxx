/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.authlib.minecraft.UserApiService
 *  net.minecraft.class_310
 *  net.minecraft.class_320
 *  net.minecraft.class_320$class_321
 *  net.minecraft.class_5520
 *  net.minecraft.class_7569
 *  net.minecraft.class_7574
 *  net.minecraft.class_7853
 */
package code.wiosvisual.utils.client;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.display.screens.mainmenu.MainMenu;
import code.wiosvisual.mixins.IMinecraftClient;
import code.wiosvisual.utils.client.managers.file.impl.AccountFile;
import code.wiosvisual.utils.client.managers.file.impl.account.Account;
import code.wiosvisual.utils.client.managers.file.impl.account.AccountRepository;
import code.wiosvisual.utils.theme.ThemeManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.minecraft.UserApiService;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;

public final class FirstLaunchManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "AuthInfo.json";

    private FirstLaunchManager() {
    }

    public static File getAuthInfoFile() {
        try {
            File filesDir = Wiosvisual.getInstance().getClientInfoProvider().filesDir();
            if (!filesDir.exists()) {
                filesDir.mkdirs();
            }
            return new File(filesDir, FILE_NAME);
        }
        catch (Exception e) {
            File fallbackDir = new File("Wiosvisual/files");
            if (!fallbackDir.exists()) {
                fallbackDir.mkdirs();
            }
            return new File(fallbackDir, FILE_NAME);
        }
    }

    public static boolean shouldShowFirstLaunchAnimation() {
        return !FirstLaunchManager.getAuthInfoFile().exists();
    }

    public static AuthInfo loadAuthInfo() {
        AuthInfo authInfo;
        File file = FirstLaunchManager.getAuthInfoFile();
        if (!file.exists()) {
            return null;
        }
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            authInfo = (AuthInfo)GSON.fromJson((Reader)reader, AuthInfo.class);
        }
        catch (Exception e) {
            return null;
        }
        return authInfo;
    }

    public static void completeFirstLaunch(String nickname, boolean darkTheme) {
        String normalizedNickname = FirstLaunchManager.sanitizeNickname(nickname);
        String themeName = "default_dark";
        FirstLaunchManager.applyTheme(themeName);
        FirstLaunchManager.setDefaultGuiScaleIfNeeded();
        FirstLaunchManager.saveAuthInfo(normalizedNickname, themeName);
        FirstLaunchManager.registerOfflineAccount(normalizedNickname);
        FirstLaunchManager.applySession(normalizedNickname);
    }

    public static void applySavedPreferences() {
        AuthInfo authInfo = FirstLaunchManager.loadAuthInfo();
        if (authInfo == null) {
            return;
        }
        String nickname = FirstLaunchManager.sanitizeNickname(authInfo.nickname);
        FirstLaunchManager.applyTheme("default_dark");
        FirstLaunchManager.registerOfflineAccount(nickname);
        Account active = FirstLaunchManager.findActiveAccount();
        if (active != null) {
            FirstLaunchManager.applySession(active.name, active.uuid);
        } else {
            FirstLaunchManager.applySession(nickname);
        }
    }

    private static Account findActiveAccount() {
        try {
            AccountRepository repository = Wiosvisual.getInstance().getAccountRepository();
            if (repository == null) {
                return null;
            }
            for (Account account : repository.accountList) {
                if (account == null || !account.active) continue;
                return account;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    private static void saveAuthInfo(String nickname, String themeName) {
        JsonObject json = new JsonObject();
        json.addProperty("nickname", nickname);
        json.addProperty("theme", themeName);
        json.addProperty("completedAt", (Number)System.currentTimeMillis());
        File file = FirstLaunchManager.getAuthInfoFile();
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8);){
            GSON.toJson((JsonElement)json, (Appendable)writer);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static void applyTheme(String themeName) {
        ThemeManager.loadTheme(themeName);
        MainMenu.INSTANCE.isDarkMode = true;
        MainMenu.INSTANCE.themeIcon = "I";
    }

    private static void setDefaultGuiScaleIfNeeded() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.options != null && (Integer)client.options.getGuiScale().getValue() == 0) {
                client.options.getGuiScale().setValue(2);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static void registerOfflineAccount(String nickname) {
        try {
            AccountRepository repository = Wiosvisual.getInstance().getAccountRepository();
            boolean exists = repository.accountList.stream().anyMatch(account -> account.name.equalsIgnoreCase(nickname));
            if (!exists) {
                String offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nickname).getBytes(StandardCharsets.UTF_8)).toString();
                repository.accountList.add(0, new Account(nickname, true, false, null, offlineUuid, "0"));
            }
            if (Wiosvisual.getInstance().getFileController() != null) {
                Wiosvisual.getInstance().getFileController().saveFile(AccountFile.class);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static void applySession(String nickname) {
        String offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nickname).getBytes(StandardCharsets.UTF_8)).toString();
        FirstLaunchManager.applySession(nickname, offlineUuid);
    }

    private static void applySession(String nickname, String uuid) {
        try {
            UUID parsedUuid;
            try {
                parsedUuid = UUID.fromString(uuid);
            }
            catch (Exception e) {
                parsedUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nickname).getBytes(StandardCharsets.UTF_8));
            }
            Session session = new Session(nickname, parsedUuid, "0", Optional.empty(), Optional.empty());
            IMinecraftClient client = (IMinecraftClient)MinecraftClient.getInstance();
            client.setSessionT(session);
            /* TODO port 1.21.11: GameProfile.getProperties() недоступен */
            UserApiService apiService = UserApiService.OFFLINE;
            client.setUserApiService(apiService);
            client.setSocialInteractionsManagerT(new SocialInteractionsManager(MinecraftClient.getInstance(), apiService));
            client.setProfileKeys(ProfileKeys.create((UserApiService)apiService, (Session)session, (Path)MinecraftClient.getInstance().runDirectory.toPath()));
            client.setAbuseReportContextT(AbuseReportContext.create((ReporterEnvironment)ReporterEnvironment.ofIntegratedServer(), (UserApiService)apiService));
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static String sanitizeNickname(String nickname) {
        if (nickname == null) {
            return "Player";
        }
        String cleaned = nickname.replaceAll("[^A-Za-z0-9_]", "");
        if (cleaned.length() < 3) {
            cleaned = "Player";
        }
        if (cleaned.length() > 16) {
            cleaned = cleaned.substring(0, 16);
        }
        return cleaned;
    }

    public static class AuthInfo {
        public String nickname;
        public String theme;
        public long completedAt;
    }
}
