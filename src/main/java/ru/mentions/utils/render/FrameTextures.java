package ru.mentions.utils.render;

import java.io.InputStream;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

public final class FrameTextures {

    private static final Identifier[] FRAME = new Identifier[]{
            Identifier.of("client", "textures/frame/menu.png"),
            Identifier.of("client", "textures/frame/menu2.png"),
            Identifier.of("client", "textures/frame/menu3.png"),
            Identifier.of("client", "textures/frame/menu4.png"),
            Identifier.of("client", "textures/frame/rectgui.png"),
            Identifier.of("client", "textures/frame/rectthemegui.png"),
            Identifier.of("client", "textures/frame/background.png"),
            Identifier.of("client", "textures/tg.png"),
    };

    private static boolean loaded = false;

    private FrameTextures() {
    }

    public static void ensure() {
        if (loaded) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getResourceManager() == null) return;

        for (Identifier id : FRAME) {
            try {
                Optional<Resource> res = mc.getResourceManager().getResource(id);
                if (res.isEmpty()) continue;
                try (InputStream in = res.get().getInputStream()) {
                    NativeImage image = NativeImage.read(in);
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> id.toString(), image);
                    texture.upload();
                    mc.getTextureManager().registerTexture(id, texture);
                }
            } catch (Exception ignored) {
            }
        }
        loaded = true;
    }
}
