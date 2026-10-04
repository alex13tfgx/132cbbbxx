/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.features.impl.render;

import code.wiosvisual.events.render.AspectRatioEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.managers.event.EventHandler;

public class AspectRatio
extends Module {
    private final SliderSettings ratioSetting = new SliderSettings("\u0421\u043e\u043e\u0442\u043d\u043e\u0448\u0435\u043d\u0438\u0435", "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430 \u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f \u0441\u043e\u043e\u0442\u043d\u043e\u0448\u0435\u043d\u0438\u044f \u0441\u0442\u043e\u0440\u043e\u043d").setValue(1.78f).range(0.5f, 3.0f).step(0.01f);

    public AspectRatio() {
        super("AspectRatio", "Aspect Ratio", ModuleCategory.RENDER);
        this.setup(this.ratioSetting);
    }

    public static AspectRatio getInstance() {
        return code.wiosvisual.utils.client.Instance.get(AspectRatio.class);
    }

    /**
     * Во сколько раз сжать руки по горизонтали, чтобы они менялись вместе с миром: рука рисуется
     * отдельной проекцией с родным соотношением экрана, поэтому подгоняем её масштабом.
     */
    public static float handScaleX() {
        AspectRatio module = getInstance();
        if (module == null || !module.isState()) {
            return 1.0f;
        }
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        float height = mc.getWindow().getFramebufferHeight();
        if (height <= 0.0f) {
            return 1.0f;
        }
        float nativeRatio = (float)mc.getWindow().getFramebufferWidth() / height;
        float custom = Math.max(0.1f, module.ratioSetting.getValue());
        return nativeRatio / custom;
    }

    @EventHandler
    public void onAspectRatio(AspectRatioEvent e) {
        e.setRatio(this.ratioSetting.getValue());
        e.cancel();
    }
}
