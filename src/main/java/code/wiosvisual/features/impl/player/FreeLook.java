/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_3532
 *  net.minecraft.class_5498
 */
package code.wiosvisual.features.impl.player;

import code.wiosvisual.events.keyboard.KeyEvent;
import code.wiosvisual.events.keyboard.MouseRotationEvent;
import code.wiosvisual.events.render.CameraEvent;
import code.wiosvisual.events.render.FovEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BindSetting;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.features.aura.utils.MathAngle;
import code.wiosvisual.utils.features.aura.warp.Turns;
import code.wiosvisual.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;

public class FreeLook
extends Module {
    private Perspective perspective;
    private Turns angle;
    public static BindSetting freeLookSetting = new BindSetting("\u0421\u0432\u043e\u0431\u043e\u0434\u043d\u044b\u0439 \u043e\u0431\u0437\u043e\u0440", "\u041a\u043b\u0430\u0432\u0438\u0448\u0430 \u0434\u043b\u044f \u0441\u0432\u043e\u0431\u043e\u0434\u043d\u043e\u0433\u043e \u043e\u0431\u0437\u043e\u0440\u0430");

    public FreeLook() {
        super("FreeLook", "Free Look", ModuleCategory.RENDER);
        this.setup(freeLookSetting);
        this.angle = null;
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (e.isKeyDown(freeLookSetting.getKey())) {
            this.perspective = FreeLook.mc.options.getPerspective();
            if (this.angle == null) {
                this.angle = MathAngle.cameraAngle();
            }
        }
    }

    @EventHandler
    public void onFov(FovEvent e) {
        if (PlayerInteractionHelper.isKey(freeLookSetting)) {
            if (FreeLook.mc.options.getPerspective().isFirstPerson()) {
                FreeLook.mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            }
            if (this.angle == null) {
                this.angle = MathAngle.cameraAngle();
            }
        } else if (this.perspective != null) {
            FreeLook.mc.options.setPerspective(this.perspective);
            this.perspective = null;
            this.angle = null;
        }
    }

    @EventHandler
    public void onMouseRotation(MouseRotationEvent e) {
        if (PlayerInteractionHelper.isKey(freeLookSetting)) {
            if (this.angle == null) {
                this.angle = MathAngle.cameraAngle();
            }
            this.angle.setYaw(this.angle.getYaw() + e.getCursorDeltaX() * 0.15f);
            this.angle.setPitch(MathHelper.clamp((float)(this.angle.getPitch() + e.getCursorDeltaY() * 0.15f), (float)-90.0f, (float)90.0f));
            e.cancel();
        } else {
            this.angle = null;
        }
    }

    @EventHandler
    public void onCamera(CameraEvent e) {
        if (PlayerInteractionHelper.isKey(freeLookSetting) && this.angle != null) {
            e.setAngle(this.angle);
            e.cancel();
        }
    }
}
