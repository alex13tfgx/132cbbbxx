package code.wiosvisual.features.impl.render;

import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.esp.OutlineRenderer;
import java.awt.Color;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Свечение по контуру игрока. В отличие от ванильного Glowing обводка честно
 * перекрывается стенами: она рисуется отдельным проходом с проверкой глубины.
 */
public class BodyOutline
extends Module {
    public final SliderSettings thickness = new SliderSettings("Толщина", "Насколько широкий ободок").range(1.01f, 1.15f).step(0.005f).setValue(1.05f);
    public final SliderSettings opacity = new SliderSettings("Прозрачность", "Плотность свечения").range(0.0f, 1.0f).step(0.05f).setValue(1.0f);
    public final BooleanSetting self = new BooleanSetting("Свой игрок", "Обводить также себя").setValue(true);
    public final BooleanSetting clientColor = new BooleanSetting("Цвет клиента", "Брать цвет из темы клиента").setValue(false);
    public final ColorSetting color = new ColorSetting("Цвет", "Цвет обводки").value(new Color(64, 224, 224).getRGB()).presets(new Color(64, 224, 224).getRGB(), new Color(120, 180, 255).getRGB(), new Color(150, 80, 245).getRGB(), new Color(255, 255, 255).getRGB()).visible(() -> !this.clientColor.isValue());

    public BodyOutline() {
        super("BodyOutline", "Body Outline", ModuleCategory.RENDER);
        this.setup(this.thickness, this.opacity, this.self, this.clientColor, this.color);
    }

    public static BodyOutline getInstance() {
        return Instance.get(BodyOutline.class);
    }

    @Override
    public void deactivate() {
        OutlineRenderer.clear();
        super.deactivate();
    }

    /** Нужно ли обводить эту сущность прямо сейчас. */
    public static boolean shouldOutline(Entity entity) {
        BodyOutline instance = BodyOutline.getInstance();
        if (instance == null || !instance.isState() || !(entity instanceof PlayerEntity)) {
            return false;
        }
        if (entity.isInvisible() || !entity.isAlive()) {
            return false;
        }
        if (instance.opacity.getValue() <= 0.01f) {
            return false;
        }
        if (entity == mc.player) {
            // Своя обводка от первого лица залила бы весь экран.
            return instance.self.isValue() && mc.options != null && !mc.options.getPerspective().isFirstPerson();
        }
        return true;
    }

    public static int outlineColor() {
        BodyOutline instance = BodyOutline.getInstance();
        if (instance == null) {
            return -1;
        }
        int rgb = (instance.clientColor.isValue() ? ColorAssist.getHudColor() : instance.color.getColor()) & 0xFFFFFF;
        int alpha = (int)(255.0f * Math.max(0.0f, Math.min(1.0f, instance.opacity.getValue())));
        return alpha << 24 | rgb;
    }

    public static float outlineThickness() {
        BodyOutline instance = BodyOutline.getInstance();
        return instance == null ? 1.05f : instance.thickness.getValue();
    }

    /**
     * Очередь выгружается после отрисовки сущностей: буфер моделей у Minecraft отложенный,
     * и обводка, нарисованная раньше, оказалась бы под самими моделями.
     */
    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (OutlineRenderer.hasEntries()) {
            OutlineRenderer.renderAll();
        }
    }
}
