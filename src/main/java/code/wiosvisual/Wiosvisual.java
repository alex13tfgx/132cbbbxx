/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.eventbus.EventBus
 *  net.fabricmc.api.ModInitializer
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.class_310
 */
package code.wiosvisual;

import code.wiosvisual.utils.client.server.ServerRestrictions;

import code.wiosvisual.commands.CommandDispatcher;
import code.wiosvisual.commands.manager.CommandRepository;
import code.wiosvisual.common.repository.box.BoxESPRepository;
import code.wiosvisual.common.repository.macro.MacroRepository;
import code.wiosvisual.common.repository.rct.RCTRepository;
import code.wiosvisual.common.repository.way.WayRepository;
import code.wiosvisual.features.impl.misc.HolyWorldAutoJoin;
import code.wiosvisual.features.module.ModuleProvider;
import code.wiosvisual.features.module.ModuleRepository;
import code.wiosvisual.features.module.ModuleSwitcher;
import code.wiosvisual.main.client.ClientInfo;
import code.wiosvisual.main.client.ClientInfoProvider;
import code.wiosvisual.main.listener.ListenerRepository;
import code.wiosvisual.utils.client.discord.DiscordPresence;
import code.wiosvisual.utils.client.net.PresenceService;
import code.wiosvisual.utils.client.auth.TelegramAuthManager;
import code.wiosvisual.utils.client.bind.BindScrollBridge;
import code.wiosvisual.utils.client.logs.Logger;
import code.wiosvisual.utils.client.managers.api.draggable.DraggableRepository;
import code.wiosvisual.utils.client.managers.event.EventManager;
import code.wiosvisual.utils.client.managers.event.EventWarmup;
import code.wiosvisual.utils.client.managers.file.DirectoryCreator;
import code.wiosvisual.utils.client.managers.file.FileController;
import code.wiosvisual.utils.client.managers.file.FileRepository;
import code.wiosvisual.utils.client.managers.file.exception.FileProcessingException;
import code.wiosvisual.utils.client.managers.file.impl.AccountFile;
import code.wiosvisual.utils.client.managers.file.impl.account.AccountRepository;
import code.wiosvisual.utils.client.sound.SoundManager;
import code.wiosvisual.utils.config.ConfigManager;
import code.wiosvisual.utils.connection.cloud.CloudConfigWebSocketClient;
import code.wiosvisual.utils.display.scissor.ScissorAssist;
import code.wiosvisual.utils.features.aura.FreeLookStorage;
import com.google.common.eventbus.EventBus;
import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

public class Wiosvisual
implements ModInitializer {
    static Wiosvisual instance;
    private EventManager eventManager = new EventManager();
    private EventBus eventBus = new EventBus();
    private ModuleRepository moduleRepository;
    private ModuleSwitcher moduleSwitcher;
    private BindScrollBridge bindScrollBridge = new BindScrollBridge();
    private CommandRepository commandRepository;
    private CommandDispatcher commandDispatcher;
    private BoxESPRepository boxESPRepository = new BoxESPRepository(this.eventManager);
    private MacroRepository macroRepository = new MacroRepository(this.eventManager);
    private WayRepository wayRepository = new WayRepository(this.eventManager);
    private RCTRepository RCTRepository = new RCTRepository(this.eventManager);
    private HolyWorldAutoJoin holyWorldAutoJoin;
    private ModuleProvider moduleProvider;
    private DraggableRepository draggableRepository;
    private FileRepository fileRepository;
    private FileController fileController;
    private ScissorAssist scissorManager = new ScissorAssist();
    private ClientInfoProvider clientInfoProvider;
    private ListenerRepository listenerRepository;
    private CloudConfigWebSocketClient cloudConfigClient;
    private AccountRepository accountRepository;
    private String nativeUsername = Wiosvisual.getMinecraftUsernameStatic();
    private String nativeUserIdentifier = "1337";
    private String nativeUserRole = "admin";
    private boolean initialized;
    private ScheduledExecutorService reconnectScheduler;
    private boolean reconnecting = false;

    public void onInitialize() {
        instance = this;
        EventWarmup.warmup();
        this.initNativeAPI();
        this.initClientInfoProvider();
        this.initModules();
        this.initDraggable();
        this.initFileManager();
        this.initCommands();
        this.initListeners();
        this.initHolyWorldAutoJoin();
        new FreeLookStorage();
        SoundManager.init();
        DiscordPresence.init();
        PresenceService.init();
        this.initialized = true;
    }

    public void syncSiteIdentity() {
        TelegramAuthManager.ensureInitialized();
        PresenceService.getInstance().reconnect();
        if (TelegramAuthManager.isAuthorized()) {
            this.nativeUsername = TelegramAuthManager.getDisplayNameOrFallback(this.nativeUsername);
            this.nativeUserIdentifier = TelegramAuthManager.getUidOrFallback(this.nativeUserIdentifier);
            this.nativeUserRole = TelegramAuthManager.getRoleOrFallback(this.nativeUserRole);
            if (this.clientInfoProvider != null) {
                this.clientInfoProvider = new ClientInfo("Wiosvisual", this.nativeUsername, this.nativeUserRole, this.clientInfoProvider.clientDir(), this.clientInfoProvider.filesDir(), this.clientInfoProvider.configsDir());
            }
        }
    }

    private void initNativeAPI() {
        for (BridgeDefinition definition : BridgeDefinition.ordered()) {
            try {
                Object userRole;
                Object userId;
                Class<?> bridgeClass = Class.forName(definition.className());
                Object userName = this.invokeBridgeMethod(bridgeClass, definition.userNameMethod());
                if (userName != null) {
                    this.nativeUsername = userName.toString();
                }
                if ((userId = this.invokeBridgeMethod(bridgeClass, definition.userIdMethod())) != null) {
                    this.nativeUserIdentifier = String.valueOf(userId);
                }
                this.nativeUserRole = (userRole = this.invokeBridgeMethod(bridgeClass, definition.userRoleMethod())) != null ? userRole.toString() : "admin";
                this.logBridgeStatus(bridgeClass, definition.className());
                return;
            }
            catch (ClassNotFoundException bridgeClass) {
            }
            catch (Exception e) {
                Logger.error("Failed to initialize native bridge " + definition.className() + ": " + e.getMessage());
            }
        }
        this.nativeUsername = this.getMinecraftUsername();
        this.nativeUserIdentifier = "0";
        this.nativeUserRole = "admin";
        Logger.info("Native bridge fallback active: using pure Java defaults");
    }

    private Object invokeBridgeMethod(Class<?> bridgeClass, String methodName) throws Exception {
        Method method = bridgeClass.getMethod(methodName, new Class[0]);
        return method.invoke(null, new Object[0]);
    }

    private void logBridgeStatus(Class<?> bridgeClass, String bridgeName) {
        try {
            Method loadedMethod = bridgeClass.getMethod("isNativeLibraryLoaded", new Class[0]);
            Method sourceMethod = bridgeClass.getMethod("getNativeLoadSource", new Class[0]);
            Object loaded = loadedMethod.invoke(null, new Object[0]);
            Object source = sourceMethod.invoke(null, new Object[0]);
            Logger.info("Native bridge active: " + bridgeName + ", nativeLoaded=" + String.valueOf(loaded) + ", source=" + String.valueOf(source));
        }
        catch (Exception ignored) {
            Logger.info("Native bridge active: " + bridgeName);
        }
    }

    private String getMinecraftUsername() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.getSession() != null) {
                return mc.getSession().getUsername();
            }
        }
        catch (Exception e) {
            Logger.error("Failed to get Minecraft username: " + e.getMessage());
        }
        return "t.me/Wiosvisual";
    }

    private static String getMinecraftUsernameStatic() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.getSession() != null) {
                return mc.getSession().getUsername();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return "t.me/Wiosvisual";
    }

    private void initWebSocketClient() {
        try {
            if (this.cloudConfigClient != null) {
                this.cloudConfigClient.connect();
            }
        }
        catch (Exception e) {
            Logger.error("Failed to initialize WebSocket client: " + e.getMessage());
        }
    }

    private void initDraggable() {
        this.draggableRepository = new DraggableRepository();
        this.draggableRepository.setup();
    }

    private void initModules() {
        this.moduleRepository = new ModuleRepository();
        this.moduleRepository.setup();
        this.moduleProvider = new ModuleProvider(this.moduleRepository.modules());
        this.moduleSwitcher = new ModuleSwitcher(this.moduleRepository.modules(), this.eventManager);
        // InterFace заменяет старый HUD, поэтому включён по умолчанию (сохранённый конфиг может это изменить).
        for (code.wiosvisual.features.module.Module module : this.moduleRepository.modules()) {
            if (module instanceof ru.mentions.module.impl.display.InterFace
                    || module instanceof code.wiosvisual.features.impl.render.InvSpoofer
                    || module instanceof code.wiosvisual.features.impl.player.SaturationViewer) {
                module.setState(true);
            }
        }
        EventManager.register(this.bindScrollBridge);
        EventManager.register(ServerRestrictions.getInstance());
        // HolyWorld LiteAPI: каналы и проверка заблокированных функций (Feature Control).
        code.wiosvisual.utils.client.server.HolyWorldLiteApi.register();
        EventManager.register(code.wiosvisual.utils.client.server.HolyWorldLiteApi.getInstance());
    }

    private void initCommands() {
        this.commandRepository = new CommandRepository();
        this.commandDispatcher = new CommandDispatcher(this.eventManager);
    }

    private void initHolyWorldAutoJoin() {
        this.holyWorldAutoJoin = new HolyWorldAutoJoin();
        EventManager.register(this.holyWorldAutoJoin);
    }

    private void initClientInfoProvider() {
        try {
            File gameDir = FabricLoader.getInstance().getGameDir().toFile();
            File clientDirectory = new File(gameDir, "Wiosvisual");
            File filesDirectory = new File(clientDirectory, "files");
            File moduleFilesDirectory = new File(filesDirectory, "config");
            this.clientInfoProvider = new ClientInfo("Wiosvisual", this.nativeUsername, this.nativeUserRole, clientDirectory, filesDirectory, moduleFilesDirectory);
        }
        catch (Exception e) {
            Logger.error("Failed to initialize client info provider: " + e.getMessage());
        }
    }

    private void initFileManager() {
        if (this.clientInfoProvider == null) {
            Logger.error("ClientInfoProvider is null, cannot initialize file manager");
            return;
        }
        DirectoryCreator directoryCreator = new DirectoryCreator();
        directoryCreator.createDirectories(this.clientInfoProvider.clientDir(), this.clientInfoProvider.filesDir(), this.clientInfoProvider.configsDir());
        this.fileRepository = new FileRepository();
        this.fileRepository.setup(this);
        this.accountRepository = new AccountRepository();
        this.fileRepository.getClientFiles().add(new AccountFile(this.accountRepository));
        this.fileController = new FileController(this.fileRepository.getClientFiles(), this.clientInfoProvider.filesDir(), this.clientInfoProvider.configsDir());
        try {
            this.fileController.loadFiles();
            TelegramAuthManager.ensureInitialized();
            this.syncSiteIdentity();
            ConfigManager.loadDefaultConfig();
        }
        catch (FileProcessingException e) {
            Logger.error("Failed to load files: " + e.getMessage());
        }
    }

    private void initListeners() {
        this.listenerRepository = new ListenerRepository();
        this.listenerRepository.setup();
    }

    public String getNativeUsername() {
        return TelegramAuthManager.getDisplayNameOrFallback(this.nativeUsername);
    }

    public String getNativeUserIdentifier() {
        return TelegramAuthManager.getUidOrFallback(this.nativeUserIdentifier);
    }

    public String getNativeUserRole() {
        return TelegramAuthManager.getRoleOrFallback(this.nativeUserRole);
    }

    public EventManager getEventManager() {
        return this.eventManager;
    }

    public EventBus getEventBus() {
        return this.eventBus;
    }

    public ModuleRepository getModuleRepository() {
        return this.moduleRepository;
    }

    public ModuleSwitcher getModuleSwitcher() {
        return this.moduleSwitcher;
    }

    public BindScrollBridge getBindScrollBridge() {
        return this.bindScrollBridge;
    }

    public CommandRepository getCommandRepository() {
        return this.commandRepository;
    }

    public CommandDispatcher getCommandDispatcher() {
        return this.commandDispatcher;
    }

    public BoxESPRepository getBoxESPRepository() {
        return this.boxESPRepository;
    }

    public MacroRepository getMacroRepository() {
        return this.macroRepository;
    }

    public WayRepository getWayRepository() {
        return this.wayRepository;
    }

    public RCTRepository getRCTRepository() {
        return this.RCTRepository;
    }

    public HolyWorldAutoJoin getHolyWorldAutoJoin() {
        return this.holyWorldAutoJoin;
    }

    public ModuleProvider getModuleProvider() {
        return this.moduleProvider;
    }

    public DraggableRepository getDraggableRepository() {
        return this.draggableRepository;
    }

    public FileRepository getFileRepository() {
        return this.fileRepository;
    }

    public FileController getFileController() {
        return this.fileController;
    }

    public ScissorAssist getScissorManager() {
        return this.scissorManager;
    }

    public ClientInfoProvider getClientInfoProvider() {
        return this.clientInfoProvider;
    }

    public ListenerRepository getListenerRepository() {
        return this.listenerRepository;
    }

    public CloudConfigWebSocketClient getCloudConfigClient() {
        return this.cloudConfigClient;
    }

    public AccountRepository getAccountRepository() {
        return this.accountRepository;
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public ScheduledExecutorService getReconnectScheduler() {
        return this.reconnectScheduler;
    }

    public boolean isReconnecting() {
        return this.reconnecting;
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void setModuleRepository(ModuleRepository moduleRepository) {
        this.moduleRepository = moduleRepository;
    }

    public void setModuleSwitcher(ModuleSwitcher moduleSwitcher) {
        this.moduleSwitcher = moduleSwitcher;
    }

    public void setBindScrollBridge(BindScrollBridge bindScrollBridge) {
        this.bindScrollBridge = bindScrollBridge;
    }

    public void setCommandRepository(CommandRepository commandRepository) {
        this.commandRepository = commandRepository;
    }

    public void setCommandDispatcher(CommandDispatcher commandDispatcher) {
        this.commandDispatcher = commandDispatcher;
    }

    public void setBoxESPRepository(BoxESPRepository boxESPRepository) {
        this.boxESPRepository = boxESPRepository;
    }

    public void setMacroRepository(MacroRepository macroRepository) {
        this.macroRepository = macroRepository;
    }

    public void setWayRepository(WayRepository wayRepository) {
        this.wayRepository = wayRepository;
    }

    public void setRCTRepository(RCTRepository RCTRepository2) {
        this.RCTRepository = RCTRepository2;
    }

    public void setHolyWorldAutoJoin(HolyWorldAutoJoin holyWorldAutoJoin) {
        this.holyWorldAutoJoin = holyWorldAutoJoin;
    }

    public void setModuleProvider(ModuleProvider moduleProvider) {
        this.moduleProvider = moduleProvider;
    }

    public void setDraggableRepository(DraggableRepository draggableRepository) {
        this.draggableRepository = draggableRepository;
    }

    public void setFileRepository(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public void setFileController(FileController fileController) {
        this.fileController = fileController;
    }

    public void setScissorManager(ScissorAssist scissorManager) {
        this.scissorManager = scissorManager;
    }

    public void setClientInfoProvider(ClientInfoProvider clientInfoProvider) {
        this.clientInfoProvider = clientInfoProvider;
    }

    public void setListenerRepository(ListenerRepository listenerRepository) {
        this.listenerRepository = listenerRepository;
    }

    public void setCloudConfigClient(CloudConfigWebSocketClient cloudConfigClient) {
        this.cloudConfigClient = cloudConfigClient;
    }

    public void setAccountRepository(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public void setNativeUsername(String nativeUsername) {
        this.nativeUsername = nativeUsername;
    }

    public void setNativeUserIdentifier(String nativeUserIdentifier) {
        this.nativeUserIdentifier = nativeUserIdentifier;
    }

    public void setNativeUserRole(String nativeUserRole) {
        this.nativeUserRole = nativeUserRole;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public void setReconnectScheduler(ScheduledExecutorService reconnectScheduler) {
        this.reconnectScheduler = reconnectScheduler;
    }

    public void setReconnecting(boolean reconnecting) {
        this.reconnecting = reconnecting;
    }

    public static Wiosvisual getInstance() {
        return instance;
    }

    private record BridgeDefinition(String className, String userNameMethod, String userIdMethod, String userRoleMethod) {
        private static List<BridgeDefinition> ordered() {
            return List.of(new BridgeDefinition("ru.dreamix.fabricloader.VMBridge", "getUserName", "getUserID", "getUserRole"));
        }
    }
}
