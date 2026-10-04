/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.Local
 *  net.minecraft.class_2499
 *  net.minecraft.class_2520
 *  net.minecraft.class_641
 *  net.minecraft.class_642
 *  net.minecraft.class_642$class_8678
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package code.wiosvisual.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import code.wiosvisual.utils.client.SponsorServers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={ServerList.class})
public class ServerListMixin {
    @Shadow
    @Final
    private List<ServerInfo> servers;

    @Redirect(method={"saveFile"}, at=@At(value="INVOKE", target="Lnet/minecraft/nbt/NbtList;add(Ljava/lang/Object;)Z", ordinal=0))
    private boolean saveFileHook(NbtList instance, Object o, @Local(ordinal=0) ServerInfo info) {
        if (SponsorServers.contains(info)) {
            return true;
        }
        return instance.add((NbtElement)o);
    }
}
