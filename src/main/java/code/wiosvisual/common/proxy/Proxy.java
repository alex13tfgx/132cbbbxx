/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.SerializedName
 */
package code.wiosvisual.common.proxy;

import com.google.gson.annotations.SerializedName;

public class Proxy {
    @SerializedName(value="IP:PORT")
    public String ipPort = "";
    public ProxyType type = ProxyType.SOCKS5;
    public String username = "";
    public String password = "";

    public Proxy() {
    }

    public Proxy(boolean isSocks4, String ipPort, String username, String password) {
        this.type = isSocks4 ? ProxyType.SOCKS4 : ProxyType.SOCKS5;
        this.ipPort = ipPort;
        this.username = username;
        this.password = password;
    }

    public int getPort() {
        if (this.ipPort == null || this.ipPort.isEmpty() || !this.ipPort.contains(":")) {
            return 0;
        }
        try {
            return Integer.parseInt(this.ipPort.split(":")[1]);
        }
        catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            return 0;
        }
    }

    public String getIp() {
        if (this.ipPort == null || this.ipPort.isEmpty() || !this.ipPort.contains(":")) {
            return "";
        }
        return this.ipPort.split(":")[0];
    }

    public static enum ProxyType {
        SOCKS4,
        SOCKS5;

    }
}
