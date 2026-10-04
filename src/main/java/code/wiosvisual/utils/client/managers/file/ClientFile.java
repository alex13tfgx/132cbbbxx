/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.client.managers.file;

import code.wiosvisual.utils.client.managers.file.exception.FileLoadException;
import code.wiosvisual.utils.client.managers.file.exception.FileSaveException;
import java.io.File;

public abstract class ClientFile {
    private final String name;

    public abstract void saveToFile(File var1) throws FileSaveException;

    public abstract void loadFromFile(File var1) throws FileLoadException;

    public void saveToFile(File path, String fileName) throws FileSaveException {
    }

    public void loadFromFile(File path, String fileName) throws FileLoadException {
    }

    public String getName() {
        return this.name;
    }

    public ClientFile(String name) {
        this.name = name;
    }
}
