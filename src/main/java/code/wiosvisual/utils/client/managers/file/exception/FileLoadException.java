/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.client.managers.file.exception;

import code.wiosvisual.utils.client.managers.file.exception.FileProcessingException;

public class FileLoadException
extends FileProcessingException {
    public FileLoadException(String message) {
        super(message);
    }

    public FileLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
