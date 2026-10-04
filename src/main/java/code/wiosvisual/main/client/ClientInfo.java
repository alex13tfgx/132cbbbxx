/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.main.client;

import code.wiosvisual.main.client.ClientInfoProvider;
import code.wiosvisual.utils.client.chat.StringHelper;
import java.io.File;

public record ClientInfo(String clientName, String userName, String role, File clientDir, File filesDir, File configsDir) implements ClientInfoProvider
{
    @Override
    public String getFullInfo() {
        return String.format("Welcome! Client: %s Version: %s Branch: %s", this.clientName, "\u0447\u0438\u0442\u0430\u043a \u0432\u0440\u043e\u0434\u0435", StringHelper.getUserRole());
    }
}
