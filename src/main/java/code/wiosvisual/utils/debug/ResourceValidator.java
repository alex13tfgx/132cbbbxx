/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_3300
 */
package code.wiosvisual.utils.debug;

import code.wiosvisual.utils.client.logs.Logger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public class ResourceValidator {
    private static final List<String> CRITICAL_FONT_RESOURCES = List.of("mre:fonts/medium.json", "mre:fonts/medium.png", "mre:fonts/regular.json", "mre:fonts/regular.png", "mre:fonts/bold.json", "mre:fonts/bold.png", "mre:fonts/suisseintlmedium.json", "mre:fonts/suisseintlmedium.png", "mre:fonts/suisseintlregular.json", "mre:fonts/suisseintlregular.png", "mre:fonts/wiosvisualicons.json", "mre:fonts/wiosvisualicons.png");
    private static final List<String> CRITICAL_SHADER_RESOURCES = List.of("mre:shaders/core/msdf_font.json", "mre:shaders/core/msdf_font.fsh", "mre:shaders/core/msdf_font.vsh", "mre:shaders/core/rectangle.json", "mre:shaders/core/rectangle.fsh", "mre:shaders/core/rectangle.vsh");

    public static boolean validateCriticalResources() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            Logger.warn("[ResourceValidator] MinecraftClient not initialized yet, skipping validation");
            return true;
        }
        ResourceManager resourceManager = client.getResourceManager();
        if (resourceManager == null) {
            Logger.warn("[ResourceValidator] ResourceManager not ready yet, skipping validation");
            return true;
        }
        ArrayList<String> missingResources = new ArrayList<String>();
        for (String resource : CRITICAL_FONT_RESOURCES) {
            if (ResourceValidator.resourceExists(resourceManager, resource)) continue;
            missingResources.add(resource);
        }
        for (String resource : CRITICAL_SHADER_RESOURCES) {
            if (ResourceValidator.resourceExists(resourceManager, resource)) continue;
            missingResources.add(resource);
        }
        if (!missingResources.isEmpty()) {
            Logger.error("[ResourceValidator] CRITICAL: Missing " + missingResources.size() + " resources!");
            for (String missing : missingResources) {
                Logger.error("[ResourceValidator]   - Missing: " + missing);
            }
            return false;
        }
        Logger.info("[ResourceValidator] All critical resources validated successfully!");
        return true;
    }

    private static boolean resourceExists(ResourceManager resourceManager, String resourcePath) {
        try {
            Identifier id = Identifier.of((String)resourcePath);
            return resourceManager.getResource(id).isPresent();
        }
        catch (Exception e) {
            Logger.warn("[ResourceValidator] Failed to check resource: " + resourcePath);
            return false;
        }
    }

    public static void debugFontResources() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getResourceManager() == null) {
            Logger.warn("[ResourceValidator] Cannot debug fonts - client not ready");
            return;
        }
        ResourceManager resourceManager = client.getResourceManager();
        Logger.info("[ResourceValidator] === Font Resources Debug ===");
        Iterator<String> iterator = CRITICAL_FONT_RESOURCES.iterator();
        while (iterator.hasNext()) {
            String resource;
            boolean exists = ResourceValidator.resourceExists(resourceManager, resource = iterator.next());
            Logger.info("[ResourceValidator] " + resource + " : " + (exists ? "PRESENT" : "MISSING"));
        }
        Logger.info("[ResourceValidator] === End Debug ===");
    }
}
