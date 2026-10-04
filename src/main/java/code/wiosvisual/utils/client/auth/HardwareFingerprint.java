/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.client.auth;

import code.wiosvisual.utils.client.logs.Logger;
import java.io.File;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.UUID;

public final class HardwareFingerprint {
    private static volatile String cachedHwid;

    private HardwareFingerprint() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static String computeStableHwid() {
        String cached = cachedHwid;
        if (cached != null && !cached.isBlank()) {
            return cached;
        }
        Class<HardwareFingerprint> clazz = HardwareFingerprint.class;
        synchronized (HardwareFingerprint.class) {
            if (cachedHwid != null && !cachedHwid.isBlank()) {
                // ** MonitorExit[var1_1] (shouldn't be in output)
                return cachedHwid;
            }
            cachedHwid = HardwareFingerprint.buildHwid();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return cachedHwid;
        }
    }

    private static String buildHwid() {
        try {
            String material = HardwareFingerprint.buildFingerprintMaterial();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(material.getBytes(StandardCharsets.UTF_8));
            return HardwareFingerprint.bytesToUuid(hash).toString();
        }
        catch (Exception exception) {
            Logger.warn("[HardwareFingerprint] Fallback HWID used: " + exception.getMessage());
            String fallback = "Wiosvisual-HWID-FALLBACK|" + HardwareFingerprint.safe(System.getProperty("user.name")) + "|" + HardwareFingerprint.safe(System.getProperty("os.name"));
            return UUID.nameUUIDFromBytes(fallback.getBytes(StandardCharsets.UTF_8)).toString();
        }
    }

    private static String buildFingerprintMaterial() {
        StringJoiner joiner = new StringJoiner("|");
        HardwareFingerprint.append(joiner, "os.name", System.getProperty("os.name"));
        HardwareFingerprint.append(joiner, "os.version", System.getProperty("os.version"));
        HardwareFingerprint.append(joiner, "os.arch", System.getProperty("os.arch"));
        HardwareFingerprint.append(joiner, "java.vendor", System.getProperty("java.vendor"));
        HardwareFingerprint.append(joiner, "computer", System.getenv("COMPUTERNAME"));
        HardwareFingerprint.append(joiner, "hostname", System.getenv("HOSTNAME"));
        HardwareFingerprint.append(joiner, "domain", System.getenv("USERDOMAIN"));
        HardwareFingerprint.append(joiner, "proc.id", System.getenv("PROCESSOR_IDENTIFIER"));
        HardwareFingerprint.append(joiner, "proc.arch", System.getenv("PROCESSOR_ARCHITECTURE"));
        HardwareFingerprint.appendNetworkData(joiner);
        HardwareFingerprint.appendDiskData(joiner);
        return joiner.toString();
    }

    private static void appendNetworkData(StringJoiner joiner) {
        ArrayList<String> macs = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    byte[] hardwareAddress;
                    NetworkInterface networkInterface = interfaces.nextElement();
                    if (networkInterface == null || networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp() || (hardwareAddress = networkInterface.getHardwareAddress()) == null || hardwareAddress.length == 0) continue;
                    macs.add(networkInterface.getName() + "=" + HardwareFingerprint.toHex(hardwareAddress));
                }
            }
        }
        catch (Exception exception) {
            Logger.warn("[HardwareFingerprint] Network enumeration failed: " + exception.getMessage());
        }
        Collections.sort(macs);
        for (String string : macs) {
            HardwareFingerprint.append(joiner, "net", string);
        }
    }

    private static void appendDiskData(StringJoiner joiner) {
        try {
            File[] roots = File.listRoots();
            if (roots == null) {
                return;
            }
            ArrayList<String> rootData = new ArrayList<String>();
            for (File root : roots) {
                if (root == null) continue;
                rootData.add(root.getAbsolutePath() + ":" + root.getTotalSpace());
            }
            Collections.sort(rootData);
            for (String string : rootData) {
                HardwareFingerprint.append(joiner, "disk", string);
            }
        }
        catch (Exception exception) {
            Logger.warn("[HardwareFingerprint] Disk info unavailable: " + exception.getMessage());
        }
    }

    private static UUID bytesToUuid(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long msb = buffer.getLong();
        long lsb = buffer.getLong();
        return new UUID(msb, lsb);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format(Locale.ROOT, "%02x", value));
        }
        return builder.toString();
    }

    private static void append(StringJoiner joiner, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        joiner.add(key + "=" + value.trim());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
