/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1294
 */
package code.wiosvisual.features.impl.render;

import code.wiosvisual.utils.client.server.ServerRestrictions;

import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.MultiSelectSetting;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import java.util.ArrayList;
import net.minecraft.entity.effect.StatusEffects;

public class NoRender
extends Module {
    public final MultiSelectSetting modeSetting = new MultiSelectSetting("\u042d\u043b\u0435\u043c\u0435\u043d\u0442\u044b", "\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u044d\u043b\u0435\u043c\u0435\u043d\u0442\u044b \u0434\u043b\u044f \u0438\u0433\u043d\u043e\u0440\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u044f").value("\u041e\u0433\u043e\u043d\u044c", "\u041f\u043b\u043e\u0445\u0438\u0435 \u044d\u0444\u0444\u0435\u043a\u0442\u044b", "\u0422\u044c\u043c\u0430", "\u041e\u0432\u0435\u0440\u043b\u0435\u0439 \u0431\u043b\u043e\u043a\u043e\u0432", "\u0422\u0440\u044f\u0441\u043a\u0430 \u044d\u043a\u0440\u0430\u043d\u0430", "\u0410\u043d\u0438\u043c\u0430\u0446\u0438\u044f \u0442\u043e\u0442\u0435\u043c\u0430", "\u0411\u043e\u0441\u0441\u0431\u0430\u0440", "\u0422\u0435\u043d\u0438", "\u0414\u044b\u043c", "\u0412\u0438\u043d\u044c\u0435\u0442\u043a\u0430", "\u0421\u0442\u0440\u0435\u043b\u044b \u0432 \u0438\u0433\u0440\u043e\u043a\u0435", "\u0421\u0432\u0435\u0447\u0435\u043d\u0438\u0435 \u0438\u0433\u0440\u043e\u043a\u043e\u0432", "\u041e\u0432\u0435\u0440\u043b\u0435\u0439 \u043b\u0430\u0432\u044b", "\u041e\u0432\u0435\u0440\u043b\u0435\u0439 \u0432\u043e\u0434\u044b", "\u041f\u043e\u0433\u043e\u0434\u0430", "\u0421\u043a\u043e\u0440\u0431\u043e\u0440\u0434", "\u0421\u0435\u0440\u0434\u0446\u0430 \u0438\u0441\u0441\u0443\u0448\u0435\u043d\u0438\u044f", "\u041f\u0443\u0437\u044b\u0440\u044c\u043a\u0438 \u0432\u043e\u0437\u0434\u0443\u0445\u0430", "\u0410\u0440\u043c\u043e\u0440\u0441\u0442\u0435\u043d\u0434\u044b", "\u0414\u0440\u0443\u0437\u044c\u044f", "\u0422\u0430\u0439\u0442\u043b\u044b", "\u0420\u0430\u0441\u0442\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0441\u0442\u044c", "\u0423\u0434\u043e\u0447\u043a\u0430").selected("\u041e\u0433\u043e\u043d\u044c", "\u041f\u043b\u043e\u0445\u0438\u0435 \u044d\u0444\u0444\u0435\u043a\u0442\u044b", "\u041e\u0432\u0435\u0440\u043b\u0435\u0439 \u0431\u043b\u043e\u043a\u043e\u0432", "\u0411\u043e\u0441\u0441\u0431\u0430\u0440");

    public static NoRender getInstance() {
        return Instance.get(NoRender.class);
    }

    public NoRender() {
        super("NoRender", "No Render", ModuleCategory.RENDER);
        this.setup(this.modeSetting);
        this.setState(true);
    }

    public static void enforceDefaults() {
        NoRender noRender = NoRender.getInstance();
        if (noRender == null) {
            return;
        }
        if (!noRender.isState()) {
            noRender.setState(true);
        }
        if (!noRender.modeSetting.isSelected("\u0411\u043e\u0441\u0441\u0431\u0430\u0440")) {
            ArrayList<String> selected = new ArrayList<String>(noRender.modeSetting.getSelected());
            selected.add("\u0411\u043e\u0441\u0441\u0431\u0430\u0440");
            noRender.modeSetting.setSelected(selected);
        }
    }

    public boolean shouldHideTotem() {
        return this.isState() && this.modeSetting.isSelected("\u0410\u043d\u0438\u043c\u0430\u0446\u0438\u044f \u0442\u043e\u0442\u0435\u043c\u0430");
    }

    public boolean shouldHideBossbar() {
        return this.isState() && this.modeSetting.isSelected("\u0411\u043e\u0441\u0441\u0431\u0430\u0440");
    }

    public boolean shouldHideShadows() {
        return this.isState() && this.modeSetting.isSelected("\u0422\u0435\u043d\u0438");
    }

    public boolean shouldHideSmoke() {
        return this.isState() && this.modeSetting.isSelected("\u0414\u044b\u043c");
    }

    public boolean shouldHideVignette() {
        return this.isState() && this.modeSetting.isSelected("\u0412\u0438\u043d\u044c\u0435\u0442\u043a\u0430");
    }

    public boolean shouldHideStuckArrows() {
        return this.isState() && this.modeSetting.isSelected("\u0421\u0442\u0440\u0435\u043b\u044b \u0432 \u0438\u0433\u0440\u043e\u043a\u0435");
    }

    public boolean shouldHidePlayerGlow() {
        return this.isState() && this.modeSetting.isSelected("\u0421\u0432\u0435\u0447\u0435\u043d\u0438\u0435 \u0438\u0433\u0440\u043e\u043a\u043e\u0432");
    }

    public boolean shouldHideLavaOverlay() {
        return this.isState() && this.modeSetting.isSelected("\u041e\u0432\u0435\u0440\u043b\u0435\u0439 \u043b\u0430\u0432\u044b");
    }

    public boolean shouldHideWaterOverlay() {
        return this.isState() && this.modeSetting.isSelected("\u041e\u0432\u0435\u0440\u043b\u0435\u0439 \u0432\u043e\u0434\u044b");
    }

    public boolean shouldHideWeather() {
        return this.isState() && this.modeSetting.isSelected("\u041f\u043e\u0433\u043e\u0434\u0430");
    }

    public boolean shouldHideScoreboard() {
        return this.isState() && this.modeSetting.isSelected("\u0421\u043a\u043e\u0440\u0431\u043e\u0440\u0434");
    }

    public boolean shouldHideWitherHearts() {
        return this.isState() && this.modeSetting.isSelected("\u0421\u0435\u0440\u0434\u0446\u0430 \u0438\u0441\u0441\u0443\u0448\u0435\u043d\u0438\u044f");
    }

    public boolean shouldHideAirBubbles() {
        return this.isState() && this.modeSetting.isSelected("\u041f\u0443\u0437\u044b\u0440\u044c\u043a\u0438 \u0432\u043e\u0437\u0434\u0443\u0445\u0430");
    }

    public boolean shouldHideArmorStands() {
        return this.isState() && this.modeSetting.isSelected("\u0410\u0440\u043c\u043e\u0440\u0441\u0442\u0435\u043d\u0434\u044b");
    }

    public boolean shouldHideBadEffects() {
        return this.isState() && !ServerRestrictions.isForbiddenHere(ServerRestrictions.Feature.REMOVE_BAD_EFFECTS) && this.modeSetting.isSelected("\u041f\u043b\u043e\u0445\u0438\u0435 \u044d\u0444\u0444\u0435\u043a\u0442\u044b");
    }

    public boolean shouldHideDarkness() {
        return this.isState() && !ServerRestrictions.isForbiddenHere(ServerRestrictions.Feature.REMOVE_DARKNESS) && this.modeSetting.isSelected("\u0422\u044c\u043c\u0430");
    }

    public boolean shouldHideFriends() {
        return this.isState() && this.modeSetting.isSelected("\u0414\u0440\u0443\u0437\u044c\u044f");
    }

    public boolean shouldHideTitles() {
        return this.isState() && this.modeSetting.isSelected("\u0422\u0430\u0439\u0442\u043b\u044b");
    }

    public boolean shouldHideVegetation() {
        return this.isState() && !ServerRestrictions.isForbiddenHere(ServerRestrictions.Feature.REMOVE_VEGETATION) && this.modeSetting.isSelected("\u0420\u0430\u0441\u0442\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0441\u0442\u044c");
    }

    public boolean shouldHideFishingRod() {
        return this.isState() && this.modeSetting.isSelected("\u0423\u0434\u043e\u0447\u043a\u0430");
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (this.shouldHideDarkness() && NoRender.mc.player != null && NoRender.mc.player.hasStatusEffect(StatusEffects.DARKNESS)) {
            NoRender.mc.player.removeStatusEffect(StatusEffects.DARKNESS);
        }
    }
}
