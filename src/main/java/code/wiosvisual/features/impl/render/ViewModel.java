/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1268
 *  net.minecraft.class_1306
 *  net.minecraft.class_1764
 *  net.minecraft.class_4587
 */
package code.wiosvisual.features.impl.render;

import code.wiosvisual.events.item.HandOffsetEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

public class ViewModel
extends Module {
    private final SliderSettings mainHandXSetting = new SliderSettings("\u041e\u0441\u043d\u043e\u0432\u043d\u0430\u044f \u0440\u0443\u043a\u0430 X", "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430 \u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f X \u0434\u043b\u044f \u043e\u0441\u043d\u043e\u0432\u043d\u043e\u0439 \u0440\u0443\u043a\u0438").setValue(0.0f).range(-1.0f, 1.0f);
    private final SliderSettings mainHandYSetting = new SliderSettings("\u041e\u0441\u043d\u043e\u0432\u043d\u0430\u044f \u0440\u0443\u043a\u0430 Y", "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430 \u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f Y \u0434\u043b\u044f \u043e\u0441\u043d\u043e\u0432\u043d\u043e\u0439 \u0440\u0443\u043a\u0438").setValue(0.0f).range(-1.0f, 1.0f);
    private final SliderSettings mainHandZSetting = new SliderSettings("\u041e\u0441\u043d\u043e\u0432\u043d\u0430\u044f \u0440\u0443\u043a\u0430 Z", "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430 \u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f Z \u0434\u043b\u044f \u043e\u0441\u043d\u043e\u0432\u043d\u043e\u0439 \u0440\u0443\u043a\u0438").setValue(0.0f).range(-2.5f, 2.5f);
    private final SliderSettings offHandXSetting = new SliderSettings("\u0412\u0442\u043e\u0440\u043e\u0441\u0442\u0435\u043f\u0435\u043d\u043d\u0430\u044f \u0440\u0443\u043a\u0430 X", "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430 \u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f X \u0434\u043b\u044f \u0432\u0442\u043e\u0440\u043e\u0441\u0442\u0435\u043f\u0435\u043d\u043d\u043e\u0439 \u0440\u0443\u043a\u0438").setValue(0.0f).range(-1.0f, 1.0f);
    private final SliderSettings offHandYSetting = new SliderSettings("\u0412\u0442\u043e\u0440\u043e\u0441\u0442\u0435\u043f\u0435\u043d\u043d\u0430\u044f \u0440\u0443\u043a\u0430 Y", "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430 \u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f Y \u0434\u043b\u044f \u0432\u0442\u043e\u0440\u043e\u0441\u0442\u0435\u043f\u0435\u043d\u043d\u043e\u0439 \u0440\u0443\u043a\u0438").setValue(0.0f).range(-1.0f, 1.0f);
    private final SliderSettings offHandZSetting = new SliderSettings("\u0412\u0442\u043e\u0440\u043e\u0441\u0442\u0435\u043f\u0435\u043d\u043d\u0430\u044f \u0440\u0443\u043a\u0430 Z", "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430 \u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f Z \u0434\u043b\u044f \u0432\u0442\u043e\u0440\u043e\u0441\u0442\u0435\u043f\u0435\u043d\u043d\u043e\u0439 \u0440\u0443\u043a\u0438").setValue(0.0f).range(-2.5f, 2.5f);
    private final SliderSettings mainHandScaleSetting = new SliderSettings("\u0420\u0430\u0437\u043c\u0435\u0440 \u043e\u0441\u043d\u043e\u0432\u043d\u043e\u0439 \u0440\u0443\u043a\u0438", "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430 \u0440\u0430\u0437\u043c\u0435\u0440\u0430 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430 \u0432 \u043e\u0441\u043d\u043e\u0432\u043d\u043e\u0439 \u0440\u0443\u043a\u0435").setValue(1.0f).range(0.1f, 2.0f);
    private final SliderSettings offHandScaleSetting = new SliderSettings("\u0420\u0430\u0437\u043c\u0435\u0440 \u0432\u0442\u043e\u0440\u043e\u0439 \u0440\u0443\u043a\u0438", "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430 \u0440\u0430\u0437\u043c\u0435\u0440\u0430 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430 \u0432\u043e \u0432\u0442\u043e\u0440\u043e\u0441\u0442\u0435\u043f\u0435\u043d\u043d\u043e\u0439 \u0440\u0443\u043a\u0435").setValue(1.0f).range(0.1f, 2.0f);

    public ViewModel() {
        super("ViewModel", "View Model", ModuleCategory.RENDER);
        this.setup(this.mainHandXSetting, this.mainHandYSetting, this.mainHandZSetting, this.offHandXSetting, this.offHandYSetting, this.offHandZSetting, this.mainHandScaleSetting, this.offHandScaleSetting);
    }

    public void applyHandPosition(MatrixStack matrices, Arm arm) {
        if (arm == Arm.RIGHT) {
            matrices.translate(this.mainHandXSetting.getValue(), this.mainHandYSetting.getValue(), this.mainHandZSetting.getValue());
            float scale = this.mainHandScaleSetting.getValue();
            if (scale != 1.0f) {
                matrices.scale(scale, scale, scale);
            }
        } else {
            matrices.translate(this.offHandXSetting.getValue(), this.offHandYSetting.getValue(), this.offHandZSetting.getValue());
            float scale = this.offHandScaleSetting.getValue();
            if (scale != 1.0f) {
                matrices.scale(scale, scale, scale);
            }
        }
    }

    @EventHandler
    public void onHandOffset(HandOffsetEvent e) {
        Hand hand = e.getHand();
        if (hand.equals((Object)Hand.MAIN_HAND) && e.getStack().getItem() instanceof CrossbowItem) {
            return;
        }
        MatrixStack matrix = e.getMatrices();
        if (hand.equals((Object)Hand.MAIN_HAND)) {
            matrix.translate(this.mainHandXSetting.getValue(), this.mainHandYSetting.getValue(), this.mainHandZSetting.getValue());
            float scale = this.mainHandScaleSetting.getValue();
            if (scale != 1.0f) {
                matrix.scale(scale, scale, scale);
            }
        } else {
            matrix.translate(this.offHandXSetting.getValue(), this.offHandYSetting.getValue(), this.offHandZSetting.getValue());
            float scale = this.offHandScaleSetting.getValue();
            if (scale != 1.0f) {
                matrix.scale(scale, scale, scale);
            }
        }
    }
}
