/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.minecraft.UserApiService
 *  net.minecraft.class_310
 *  net.minecraft.class_320
 *  net.minecraft.class_5520
 *  net.minecraft.class_7574
 *  net.minecraft.class_7853
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Mutable
 *  org.spongepowered.asm.mixin.gen.Accessor
 *  org.spongepowered.asm.mixin.gen.Invoker
 */
package code.wiosvisual.mixins;

import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={MinecraftClient.class})
public interface IMinecraftClient {
    @Accessor(value="itemUseCooldown")
    public int getUseCooldown();

    @Accessor(value="itemUseCooldown")
    public void setUseCooldown(int var1);

    @Invoker(value="doItemUse")
    public void idoItemUse();

    @Invoker(value="doAttack")
    public boolean idoAttack();

    @Mutable
    @Accessor(value="profileKeys")
    public void setProfileKeys(ProfileKeys var1);

    @Mutable
    @Accessor(value="session")
    public void setSessionT(Session var1);

    @Mutable
    @Accessor
    public void setUserApiService(UserApiService var1);

    @Mutable
    @Accessor(value="socialInteractionsManager")
    public void setSocialInteractionsManagerT(SocialInteractionsManager var1);

    @Mutable
    @Accessor(value="abuseReportContext")
    public void setAbuseReportContextT(AbuseReportContext var1);
}
