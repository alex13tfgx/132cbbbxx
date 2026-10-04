/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager$class_4534
 *  com.mojang.blaze3d.platform.GlStateManager$class_4535
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_10444
 *  net.minecraft.class_1058
 *  net.minecraft.class_1087
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1937
 *  net.minecraft.class_3532
 *  net.minecraft.class_425
 *  net.minecraft.class_4587
 *  net.minecraft.class_811
 *  net.minecraft.class_9848
 */
package code.wiosvisual.utils.display.item;

import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public final class ItemRender
implements QuickImports {
    public static final HashMap<ItemStack, CachedSprite> SPRITE_CACHE = new HashMap();

    public static void drawItem(MatrixStack matrix, ItemStack stack, float x, float y, boolean count, boolean bar) {
        CachedSprite cachedSprite = ItemRender.getSpriteTexture(stack);
        if (cachedSprite != null) {
            Render2D.drawSprite(matrix, cachedSprite.sprite, x, y, 16.0f, 16);
            if (stack != null && stack.hasGlint()) {
                ItemRender.drawGlint(matrix, cachedSprite.sprite, x, y);
            }
        }
        if (count) {
            ItemRender.drawCount(matrix, stack, x, y);
        }
        if (bar) {
            ItemRender.drawBar(matrix, stack, x, y);
        }
    }

    private static void drawGlint(MatrixStack matrix, Sprite sprite, float x, float y) {
        long now = System.currentTimeMillis();
        // TODO(port 1.21.11): раньше блик рисовался аддитивным блендом (SRC_ALPHA, ONE) через
        // удалённый RenderSystem.blendFunc; сейчас нижележащий слой использует TRANSLUCENT —
        // визуально чуть слабее, проверить в игре при необходимости.
        ItemRender.drawGlintLayer(matrix, sprite, x, y, (float)(now % 1300L) / 1300.0f, false, 230, 150, 255, 0.95f);
        ItemRender.drawGlintLayer(matrix, sprite, x, y, (float)(now % 1900L) / 1900.0f, true, 150, 90, 255, 0.85f);
        ItemRender.drawGlintLayer(matrix, sprite, x, y, (float)(now % 850L) / 850.0f, false, 255, 255, 255, 0.5f);
    }

    private static void drawGlintLayer(MatrixStack matrix, Sprite sprite, float x, float y, float phase, boolean reversed, int r, int g, int b, float maxIntensity) {
        int tl = ItemRender.glintColor(ItemRender.cornerDiagonal(0.0f, 0.0f, reversed), phase, r, g, b, maxIntensity);
        int bl = ItemRender.glintColor(ItemRender.cornerDiagonal(0.0f, 1.0f, reversed), phase, r, g, b, maxIntensity);
        int br = ItemRender.glintColor(ItemRender.cornerDiagonal(1.0f, 1.0f, reversed), phase, r, g, b, maxIntensity);
        int tr = ItemRender.glintColor(ItemRender.cornerDiagonal(1.0f, 0.0f, reversed), phase, r, g, b, maxIntensity);
        Render2D.drawSpriteColored(matrix, sprite, x, y, 16.0f, 16.0f, tl, bl, br, tr);
    }

    private static float cornerDiagonal(float u, float v, boolean reversed) {
        return reversed ? (u + (1.0f - v)) * 0.5f : (u + v) * 0.5f;
    }

    private static int glintColor(float diagonal, float phase, int r, int g, int b, float maxIntensity) {
        float band = 0.5f + 0.5f * MathHelper.cos((float)((float)(Math.PI * 2 * (double)(diagonal - phase))));
        float intensity = (0.25f + 0.75f * band * band) * maxIntensity;
        int alpha = MathHelper.clamp((int)((int)(intensity * 255.0f)), (int)0, (int)255);
        return ColorHelper.getArgb((int)alpha, (int)r, (int)g, (int)b);
    }

    private static void drawCount(MatrixStack matrix, ItemStack stack, float x, float y) {
        Object text;
        int count = stack.getCount();
        Object object = text = count > 1 ? "" + count : "";
        if (!((String)text).isEmpty()) {
            FontRenderer font = Fonts.getSize(16);
            font.drawString(matrix, (String)text, x + 16.0f - font.getStringWidth((String)text), y + 11.0f, ColorAssist.getText());
        }
    }

    private static void drawBar(MatrixStack matrix, ItemStack stack, float x, float y) {
        if (stack.isItemBarVisible()) {
            rectangle.render(ShapeProperties.create(matrix, x + 1.5f, y + 13.0f, 13.0, 2.0).color(-16777216).build());
            rectangle.render(ShapeProperties.create(matrix, x + 1.5f, y + 13.0f, stack.getItemBarStep(), 1.0).color(ColorHelper.fullAlpha((int)stack.getItemBarColor())).build());
        }
    }

    private static CachedSprite getSpriteTexture(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (stack.isOf(Items.COMPASS)) {
            return null;
        }
        if (mc == null || mc.getItemModelManager() == null) {
            return null;
        }
        if (mc.getOverlay() instanceof SplashOverlay) {
            return null;
        }
        return SPRITE_CACHE.computeIfAbsent(stack, key -> {
            ItemRenderState state = new ItemRenderState();
            try {
                mc.getItemModelManager().update(state, stack, ItemDisplayContext.GUI, (World)ItemRender.mc.world, null, 0);
            }
            catch (Throwable ignored) {
                return null;
            }
            Sprite sprite = ItemRender.getFirstParticleSprite(state);
            if (sprite != null) {
                int glId = code.wiosvisual.utils.display.render.systemrender.renderers.impl.BuiltTexture.id(mc.getTextureManager().getTexture(sprite.getAtlasId()));
                return new CachedSprite(sprite, glId, 0xFFFFFF);
            }
            return null;
        });
    }

    private static Sprite getFirstParticleSprite(ItemRenderState state) {
        if (state == null || state.layerCount == 0) {
            return null;
        }
        // Порт 1.21.11: у class_10446 больше нет поля модели; спрайт частиц лежит прямо в field_56966.
        return state.layers[0].particle;
    }

    private ItemRender() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public record CachedSprite(Sprite sprite, int glId, int color) {
    }
}
