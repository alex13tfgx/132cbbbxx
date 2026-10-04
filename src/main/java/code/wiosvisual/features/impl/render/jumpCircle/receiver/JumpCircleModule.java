/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1657
 */
package code.wiosvisual.features.impl.render.jumpCircle.receiver;

import code.wiosvisual.events.player.JumpEvent;
import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.features.impl.render.jumpCircle.data.JumpCircleStorage;
import code.wiosvisual.features.impl.render.jumpCircle.handler.JumpCircleHandler;
import code.wiosvisual.features.impl.render.jumpCircle.updater.JumpCircleUpdater;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.RadioSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.color.ColorAssist;
import net.minecraft.entity.player.PlayerEntity;

public class JumpCircleModule
extends Module {
    private final SliderSettings lifeTime = new SliderSettings("\u0412\u0440\u0435\u043c\u044f \u0436\u0438\u0437\u043d\u0438", "").range(500, 1500).setInteger(true);
    private final SliderSettings size = new SliderSettings("\u0420\u0430\u0437\u043c\u0435\u0440", "").range(1, 3).step(0.5f);
    public static RadioSetting colorMode = new RadioSetting("\u0420\u0435\u0436\u0438\u043c \u0446\u0432\u0435\u0442\u0430", "\u0412\u044b\u0431\u043e\u0440 \u0438\u0441\u0442\u043e\u0447\u043d\u0438\u043a\u0430 \u0446\u0432\u0435\u0442\u0430", new String[]{"\u041a\u043b\u0438\u0435\u043d\u0442\u0441\u043a\u0438\u0439", "\u041a\u0430\u0441\u0442\u043e\u043c"}, "\u041a\u043b\u0438\u0435\u043d\u0442\u0441\u043a\u0438\u0439");
    public static ColorSetting color = new ColorSetting("\u041a\u0430\u0441\u0442\u043e\u043c\u043d\u044b\u0439 \u0446\u0432\u0435\u0442", "\u041a\u0430\u0441\u0442\u043e\u043c\u043d\u044b\u0439 \u0446\u0432\u0435\u0442 \u043a\u0440\u0443\u0433\u0430").value(ColorAssist.getClientColor()).visible(() -> colorMode.get().equals("\u041a\u0430\u0441\u0442\u043e\u043c"));
    private final JumpCircleStorage storage = new JumpCircleStorage(this);
    private final JumpCircleHandler handler = new JumpCircleHandler(this);
    private final JumpCircleUpdater updater = new JumpCircleUpdater(this);

    public JumpCircleModule() {
        super("JumpCircle", "Jump Circle", ModuleCategory.RENDER);
        this.setup(this.lifeTime, this.size, colorMode, color);
    }

    @EventHandler
    private void jumpEventReceive(JumpEvent jumpEvent) {
        PlayerEntity player = jumpEvent.getPlayer();
        this.storage.add(player);
    }

    @EventHandler
    private void renderEventReceive(WorldRenderEvent renderEvent) {
        if (this.storage.isPresent()) {
            this.updater.update();
            this.handler.provideRender(renderEvent.getStack(), renderEvent.getPartialTicks());
        }
    }

    public SliderSettings getLifeTime() {
        return this.lifeTime;
    }

    public SliderSettings getSize() {
        return this.size;
    }

    public JumpCircleStorage getStorage() {
        return this.storage;
    }

    public JumpCircleHandler getHandler() {
        return this.handler;
    }

    public JumpCircleUpdater getUpdater() {
        return this.updater;
    }
}
