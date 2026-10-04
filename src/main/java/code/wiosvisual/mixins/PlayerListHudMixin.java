/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.mojang.authlib.GameProfile
 *  net.minecraft.class_124
 *  net.minecraft.class_2561
 *  net.minecraft.class_268
 *  net.minecraft.class_269
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_355
 *  net.minecraft.class_5250
 *  net.minecraft.class_640
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package code.wiosvisual.mixins;

import code.wiosvisual.emotions.ClientBadgeRenderer;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={PlayerListHud.class})
public abstract class PlayerListHudMixin {
    private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");

    @Shadow
    protected abstract List<PlayerListEntry> collectPlayerEntries();

    @Shadow
    public abstract Text getPlayerName(PlayerListEntry var1);

    @Inject(method={"collectPlayerEntries"}, at={@At(value="RETURN")}, cancellable=true)
    private void addVanishedEntries(CallbackInfoReturnable<List<PlayerListEntry>> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<PlayerListEntry> originalList = cir.getReturnValue();
        ArrayList<PlayerListEntry> vanishedList = new ArrayList<PlayerListEntry>();
        Scoreboard scoreboard = client.world.getScoreboard();
        ArrayList<Team> teams = new ArrayList<Team>(scoreboard.getTeams());
        teams.sort(Comparator.comparing(Team::getName));
        Collection<PlayerListEntry> online = client.player.networkHandler.getPlayerList();
        for (Team team : teams) {
            boolean present;
            String name;
            Collection members = team.getPlayerList();
            if (members.size() != 1 || !NAME_PATTERN.matcher(name = (String)members.iterator().next()).matches() || (present = online.stream().anyMatch(e -> e.getProfile() != null && name.equals(e.getProfile().name())))) continue;
            MutableText displayName = Text.empty().append((Text)Text.literal((String)"[").formatted(Formatting.GRAY)).append((Text)Text.literal((String)"V").formatted(Formatting.RED)).append((Text)Text.literal((String)"] ").formatted(Formatting.GRAY)).append(team.getPrefix()).append((Text)Text.literal((String)name).formatted(Formatting.GRAY));
            GameProfile fakeProfile = new GameProfile(UUID.randomUUID(), name);
            PlayerListEntry fake = new PlayerListEntry(fakeProfile, client.isInSingleplayer());
            fake.setDisplayName((Text)displayName);
            fake.setListOrder(Integer.MIN_VALUE);
            vanishedList.add(fake);
        }
        ArrayList<PlayerListEntry> finalList = new ArrayList<PlayerListEntry>();
        finalList.addAll(vanishedList);
        finalList.addAll(originalList);
        cir.setReturnValue(finalList);
    }

    @Redirect(method={"render"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"))
    private int Wiosvisual$renderPlayerNameWithClientBadge(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color, @Local PlayerListEntry entry) {
        GameProfile profile = entry.getProfile();
        if (profile == null || !ClientBadgeRenderer.hasCompatibleClient(profile.id())) {
            context.drawTextWithShadow(textRenderer, text, x, y, color);
            return x + textRenderer.getWidth(text);
        }
        int iconSize = 7;
        int gap = 2;
        ClientBadgeRenderer.drawRoundedBadge(context, x, (float)y + 1.0f, iconSize);
        context.drawTextWithShadow(textRenderer, text, x + iconSize + gap, y, color);
        return x + iconSize + gap + textRenderer.getWidth(text);
    }
}
