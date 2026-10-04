/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_303
 *  net.minecraft.class_338
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import code.wiosvisual.utils.client.chat.ChatBrandShimmer;
import code.wiosvisual.utils.interactions.path.BaritoneHelper;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ChatHud.class})
public class ChatHudMixin {
    @Inject(method={"addMessage(Lnet/minecraft/text/Text;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void onAddMessage(Text message, CallbackInfo ci) {
        if (BaritoneHelper.isSuppressChat() && BaritoneHelper.isBaritoneMessage(message.getString())) {
            ci.cancel();
        }
    }

    @Inject(method={"addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void onAddHudLine(ChatHudLine message, CallbackInfo ci) {
        if (BaritoneHelper.isSuppressChat() && BaritoneHelper.isBaritoneMessage(message.content().getString())) {
            ci.cancel();
        }
    }

    /**
     * Строки сообщений в ChatHud.render: префикс клиента перекрашивается каждый кадр.
     * Имена intermediary и remap=false — в проекте готовый refmap без этих имён.
     */
    @ModifyArg(method={"method_1805(Lnet/minecraft/class_332;IIIZ)V"}, at=@At(value="INVOKE", target="Lnet/minecraft/class_332;method_35720(Lnet/minecraft/class_327;Lnet/minecraft/class_5481;III)I", remap=false), index=1, remap=false)
    private OrderedText wiosvisual$shimmerBrand(OrderedText line) {
        return ChatBrandShimmer.wrap(line);
    }
}
