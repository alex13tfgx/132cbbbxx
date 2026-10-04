/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.client.managers.file;

import code.wiosvisual.utils.client.logs.Logger;
import code.wiosvisual.utils.client.managers.file.ClientFile;
import code.wiosvisual.utils.client.managers.file.exception.FileLoadException;
import code.wiosvisual.utils.client.managers.file.exception.FileSaveException;
import code.wiosvisual.utils.client.managers.file.impl.ModuleFile;
import code.wiosvisual.utils.theme.ThemeManager;
import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FileController {
    private final List<ClientFile> clientFiles;
    private final File directory;
    private final File moduleConfigDirectory;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public FileController(List<ClientFile> clientFiles, File directory, File moduleConfigDirectory) {
        this.clientFiles = clientFiles;
        this.directory = directory;
        this.moduleConfigDirectory = moduleConfigDirectory;
        this.startAutoSave();
    }

    public void startAutoSave() {
        Logger.info("Auto-save system started! (Every 5 minutes)");
        this.scheduler.scheduleAtFixedRate(() -> {
            try {
                Logger.info("Saving with auto-save.");
                this.saveFiles();
            }
            catch (FileSaveException e) {
                Logger.error("Failed to auto-save files: " + e.getMessage());
            }
        }, 5L, 5L, TimeUnit.MINUTES);
    }

    public void stopAutoSave() {
        Logger.info("Auto-save shutdown!");
        this.scheduler.shutdown();
        try {
            if (!this.scheduler.awaitTermination(1L, TimeUnit.MINUTES)) {
                this.scheduler.shutdownNow();
            }
        }
        catch (InterruptedException e) {
            this.scheduler.shutdownNow();
        }
    }

    public void saveFiles() throws FileSaveException {
        if (this.clientFiles.isEmpty()) {
            Logger.warn("No files to save from directory: " + this.directory.getPath());
            return;
        }
        for (ClientFile clientFile : this.clientFiles) {
            if (clientFile instanceof ModuleFile) continue;
            try {
                clientFile.saveToFile(this.directory);
            }
            catch (FileSaveException e) {
                throw new FileSaveException("Failed to save file: " + clientFile.getName(), e);
            }
        }
        try {
            this.saveFile("default.json");
        }
        catch (FileSaveException e) {
            Logger.error("Failed to auto-save default config: " + e.getMessage());
        }
        try {
            ThemeManager.saveTheme("default");
        }
        catch (Exception e) {
            Logger.error("Failed to auto-save default theme: " + e.getMessage());
        }
    }

    public void loadFiles() throws FileLoadException {
        if (this.clientFiles.isEmpty()) {
            Logger.warn("No files to load from directory: " + this.directory.getPath());
            return;
        }
        for (ClientFile clientFile : this.clientFiles) {
            try {
                if (clientFile instanceof ModuleFile) continue;
                clientFile.loadFromFile(this.directory);
            }
            catch (FileLoadException e) {
                Logger.warn("Failed to load file: " + clientFile.getName() + " - " + e.getMessage());
            }
        }
    }

    public void saveFile(String fileName) throws FileSaveException {
        for (ClientFile clientFile : this.clientFiles) {
            if (!(clientFile instanceof ModuleFile)) continue;
            try {
                clientFile.saveToFile(this.moduleConfigDirectory, fileName);
            }
            catch (FileSaveException e) {
                throw new FileSaveException("Failed to save file: " + fileName, e);
            }
        }
    }

    public void loadFile(String fileName) throws FileLoadException {
        for (ClientFile clientFile : this.clientFiles) {
            if (!(clientFile instanceof ModuleFile)) continue;
            try {
                clientFile.loadFromFile(this.moduleConfigDirectory, fileName);
            }
            catch (FileLoadException e) {
                throw new FileLoadException("Failed to load file: " + fileName, e);
            }
        }
    }

    public void resetConfig() throws FileLoadException, FileSaveException {
        for (ClientFile clientFile : this.clientFiles) {
            if (!(clientFile instanceof ModuleFile)) continue;
            ModuleFile moduleFile = (ModuleFile)clientFile;
            moduleFile.resetConfiguration(this.moduleConfigDirectory);
            return;
        }
        throw new FileSaveException("ModuleFile not found", null);
    }

    public void saveFile(Class<? extends ClientFile> fileClass) {
        this.clientFiles.stream().filter(fileClass::isInstance).findFirst().ifPresent(file -> {
            try {
                file.saveToFile(this.directory);
            }
            catch (FileSaveException e) {
                Logger.error("Failed to save file on-demand: " + file.getName(), e);
            }
        });
    }
}
