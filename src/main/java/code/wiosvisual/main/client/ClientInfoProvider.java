/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.main.client;

import java.io.File;

public interface ClientInfoProvider {
    public String userName();

    public String clientName();

    public String role();

    public String getFullInfo();

    public File clientDir();

    public File filesDir();

    public File configsDir();
}
