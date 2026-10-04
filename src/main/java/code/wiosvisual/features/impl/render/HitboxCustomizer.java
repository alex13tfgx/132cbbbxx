/*
 * Хитбоксы живых сущностей: та же коробка, что рисует ваниль по F3+B,
 * только своим цветом и с заливкой. Ванильные белые хитбоксы при этом
 * скрываются (HitboxSuppressMixin). Друзья из .friend подсвечиваются отдельным цветом.
 */
package code.wiosvisual.features.impl.render;

import code.wiosvisual.utils.client.server.ServerRestrictions;

import code.wiosvisual.common.repository.friend.FriendUtils;
import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.geometry.Render3D;
import code.wiosvisual.utils.math.calc.Calculate;
import code.wiosvisual.utils.math.projection.Projection;
import java.awt.Color;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;

public class HitboxCustomizer
extends Module {
    private final BooleanSetting fill = new BooleanSetting("Заливка", "Полупрозрачная заливка коробки").setValue(true);
    private final BooleanSetting clientColor = new BooleanSetting("Цвет клиента", "Брать цвет из темы").setValue(true);
    private final ColorSetting customColor = new ColorSetting("Цвет хитбокса", "Свой цвет обводки и заливки").value(Color.WHITE.getRGB()).visible(() -> !this.clientColor.isValue());
    private final SliderSettings lineWidth = new SliderSettings("Толщина линий", "Толщина обводки").setValue(1.0f).range(0.5f, 4.0f).step(0.1f);
    private final BooleanSetting friendHighlight = new BooleanSetting("Подсвечивать друзей", "Друзья из .friend рисуются отдельным цветом").setValue(true);
    private final ColorSetting friendColor = new ColorSetting("Цвет друзей", "Цвет хитбокса друзей").value(new Color(85, 255, 85).getRGB()).visible(() -> this.friendHighlight.isValue());

    public HitboxCustomizer() {
        super("HitboxCustomizer", "Hitbox Customizer", ModuleCategory.RENDER);
        this.setup(this.fill, this.clientColor, this.customColor, this.lineWidth, this.friendHighlight, this.friendColor);
    }

    public static HitboxCustomizer getInstance() {
        return Instance.get(HitboxCustomizer.class);
    }

    /** Ванильные хитбоксы скрываются, пока модуль включён. */
    public static boolean suppressesVanillaHitboxes() {
        HitboxCustomizer module = HitboxCustomizer.getInstance();
        return module != null && module.isState();
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        boolean firstPerson = mc.options.getPerspective().isFirstPerson();
        int baseColor = this.clientColor.isValue() ? ColorAssist.getClientColor() : this.customColor.getColor();
        float width = this.lineWidth.getValue();
        boolean fillBox = this.fill.isValue();
        boolean highlightFriends = this.friendHighlight.isValue() && !ServerRestrictions.isForbiddenHere(ServerRestrictions.Feature.FRIEND_HIGHLIGHT);
        int friendRgb = this.friendColor.getColor();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity) || entity == mc.player && firstPerson) continue;
            if (entity.isInvisible() && entity != mc.player) continue;
            Box box = entity.getBoundingBox().offset(Calculate.interpolate(entity).subtract(new net.minecraft.util.math.Vec3d(entity.getX(), entity.getY(), entity.getRandomBodyY())));
            if (!Projection.canSee(box)) continue;
            int color = highlightFriends && FriendUtils.isFriend(entity) ? friendRgb : baseColor;
            Render3D.drawBox(box, color, width, true, fillBox, true);
        }
    }
}
