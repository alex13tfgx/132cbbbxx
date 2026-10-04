/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_303
 *  net.minecraft.class_338
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Invoker
 */
package code.wiosvisual.mixins.acc;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={ChatHud.class})
public interface InsertChatMixin {
    @Invoker(value="addMessage")
    public void invokeAddMessage(ChatHudLine var1);

    @Invoker(value="addVisibleMessage")
    public void invokeAddVisibleMessage(ChatHudLine var1);
}
