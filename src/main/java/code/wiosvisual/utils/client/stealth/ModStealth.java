/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2596
 *  net.minecraft.class_2817
 *  net.minecraft.class_2960
 *  net.minecraft.class_8710
 *  net.minecraft.class_8710$class_9154
 */
package code.wiosvisual.utils.client.stealth;

import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.util.Identifier;

public final class ModStealth {
    public static final String MOD_ID = "renderextras";
    public static final String LEGACY_MOD_ID = "wiosvisual";
    public static final String CLIENT_BRAND = "vanilla";
    private static final String[] BLOCKED_CHANNEL_FRAGMENTS = new String[]{"wiosvisual", "renderextras", "wiosvisual", "modlist", "mod_list", "mods_list", "fabric-mod", "fabricloader", "dreamix", "vmbridge"};

    public static boolean shouldBlockOutgoing(Packet<?> packet) {
        if (!(packet instanceof CustomPayloadC2SPacket)) {
            return false;
        }
        CustomPayloadC2SPacket customPayload = (CustomPayloadC2SPacket)packet;
        CustomPayload payload = customPayload.payload();
        if (payload == null) {
            return false;
        }
        CustomPayload.Id id = payload.getId();
        if (id == null) {
            return false;
        }
        Identifier identifier = id.id();
        if (identifier == null) {
            return false;
        }
        return ModStealth.containsBlockedMarker(identifier.toString()) || ModStealth.containsBlockedMarker(payload.getClass().getName());
    }

    private static boolean containsBlockedMarker(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String lower = value.toLowerCase();
        for (String fragment : BLOCKED_CHANNEL_FRAGMENTS) {
            if (!lower.contains(fragment)) continue;
            return true;
        }
        return false;
    }

    private ModStealth() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
