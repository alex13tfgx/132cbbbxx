/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_10142
 *  net.minecraft.class_10156
 *  net.minecraft.class_310
 *  net.minecraft.class_377
 *  net.minecraft.class_378
 *  net.minecraft.class_380
 */
package code.wiosvisual.utils.display.render;

import code.wiosvisual.mixins.accessor.FontManagerAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.FontManager;

public final class GuiRenderState {
    private GuiRenderState() {
    }

    public static void restoreAfterCustomDraw() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        // Порт 1.21.11: ручное GL-состояние/шейдер (blend/depth/cull/setShader) удалены —
        // состояние теперь запечено в RenderPipeline, отдельная настройка не нужна.
        GuiRenderState.restoreFontAtlasesNearest();
    }

    public static void restoreFontAtlasesNearest() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        FontManager fontManager = client.fontManager;
        if (fontManager == null) {
            return;
        }
    }
}
