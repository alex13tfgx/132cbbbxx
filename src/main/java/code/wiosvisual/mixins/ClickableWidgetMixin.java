/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_332
 *  net.minecraft.class_339
 *  net.minecraft.class_342
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ClickableWidget.class})
public abstract class ClickableWidgetMixin {
    @Unique
    private float Wiosvisual$hoverAnim;
    @Unique
    private float Wiosvisual$focusAnim;
    @Unique
    private boolean Wiosvisual$pushed;

    @Shadow
    public abstract boolean isHovered();

    @Shadow
    public abstract int getX();

    @Shadow
    public abstract int getY();

    @Shadow
    public abstract int getWidth();

    @Shadow
    public abstract int getHeight();

    @Inject(method={"render"}, at={@At(value="HEAD")})
    private void Wiosvisual$animateWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        float focusScale;
        boolean isTextField = (Object)this instanceof TextFieldWidget;
        boolean focused = isTextField && ((TextFieldWidget)(Object)this).isFocused();
        float targetHover = this.isHovered() ? 1.0f : 0.0f;
        this.Wiosvisual$hoverAnim += (targetHover - this.Wiosvisual$hoverAnim) * 0.22f;
        this.Wiosvisual$focusAnim += ((focused ? 1.0f : 0.0f) - this.Wiosvisual$focusAnim) * 0.14f;
        float hoverLift = -1.4f * this.Wiosvisual$hoverAnim;
        float f = focusScale = isTextField ? 1.0f + 0.006f * this.Wiosvisual$focusAnim : 1.0f;
        if (Math.abs(hoverLift) < 0.01f && Math.abs(focusScale - 1.0f) < 0.001f) {
            this.Wiosvisual$pushed = false;
            return;
        }
        float centerX = (float)this.getX() + (float)this.getWidth() / 2.0f;
        float centerY = (float)this.getY() + (float)this.getHeight() / 2.0f;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0.0f, hoverLift);
        context.getMatrices().translate(centerX, centerY);
        context.getMatrices().scale(focusScale, focusScale);
        context.getMatrices().translate(-centerX, -centerY);
        this.Wiosvisual$pushed = true;
    }

    @Inject(method={"render"}, at={@At(value="RETURN")})
    private void Wiosvisual$restoreWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!this.Wiosvisual$pushed) {
            return;
        }
        context.getMatrices().popMatrix();
        this.Wiosvisual$pushed = false;
    }
}
