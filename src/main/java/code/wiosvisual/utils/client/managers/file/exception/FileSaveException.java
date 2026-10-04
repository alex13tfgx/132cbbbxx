/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.client.managers.file.exception;

import code.wiosvisual.utils.client.managers.file.exception.FileProcessingException;

public class FileSaveException
extends FileProcessingException {
    public FileSaveException(String message) {
        super(message);
    }

    public FileSaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
