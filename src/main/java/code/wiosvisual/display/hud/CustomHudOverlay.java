/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_332
 *  net.minecraft.class_9779
 */
package code.wiosvisual.display.hud;

import code.wiosvisual.display.hud.HudDraggableRenderer;
import code.wiosvisual.events.render.DrawEvent;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.GuiRenderState;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.post.KawaseBlur;
import code.wiosvisual.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class CustomHudOverlay
implements QuickImports {
    private CustomHudOverlay() {
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter, boolean renderDraggables) {
        if (context == null || drawEngine == null || tickCounter == null || CustomHudOverlay.mc.player == null || CustomHudOverlay.mc.world == null) {
            return;
        }
        if (CustomHudOverlay.mc.currentScreen != null && !PlayerInteractionHelper.isChat(CustomHudOverlay.mc.currentScreen)) {
            return;
        }
        SafeEvents.dispatch(() -> new DrawEvent(context, drawEngine, tickCounter.getTickProgress(false)));
        Render2D.onRender(context);
        if (renderDraggables) {
            HudDraggableRenderer.render(context);
        }
        GuiRenderState.restoreAfterCustomDraw();
    }
}
