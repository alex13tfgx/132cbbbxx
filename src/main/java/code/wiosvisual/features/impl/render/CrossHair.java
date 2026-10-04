/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_3966
 */
package code.wiosvisual.features.impl.render;

import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.math.calc.Calculate;
import net.minecraft.util.hit.EntityHitResult;

public class CrossHair
extends Module {
    private float red = 0.0f;
    private final SliderSettings attackSetting = new SliderSettings("\u041e\u0442\u0441\u0442\u0443\u043f \u043f\u0440\u0438 \u0430\u0442\u0430\u043a\u0435", "\u041e\u0442\u0441\u0442\u0443\u043f \u0434\u043b\u044f \u043a\u0443\u043b\u0434\u0430\u0443\u043d\u0430 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430").setValue(10.0f).range(0, 20);
    private final SliderSettings indentSetting = new SliderSettings("\u041e\u0442\u0441\u0442\u0443\u043f", "\u041e\u0442\u0441\u0442\u0443\u043f \u043e\u0442 \u0446\u0435\u043d\u0442\u0440\u0430 \u044d\u043a\u0440\u0430\u043d\u0430").setValue(0.0f).range(0, 5);
    private final SliderSettings size1Setting = new SliderSettings("\u0428\u0438\u0440\u0438\u043d\u0430", "\u0428\u0438\u0440\u0438\u043d\u0430 \u043f\u0440\u0438\u0446\u0435\u043b\u0430").setValue(4.0f).range(2, 10);
    private final SliderSettings size2Setting = new SliderSettings("\u0412\u044b\u0441\u043e\u0442\u0430", "\u0412\u044b\u0441\u043e\u0442\u0430 \u043f\u0440\u0438\u0446\u0435\u043b\u0430").setValue(1.0f).range(1, 4);

    public static CrossHair getInstance() {
        return Instance.get(CrossHair.class);
    }

    public CrossHair() {
        super("CrossHair", "Crosshair", ModuleCategory.RENDER);
        this.setup(this.attackSetting, this.indentSetting, this.size1Setting, this.size2Setting);
    }

    public void onRenderCrossHair() {
        this.red = Calculate.interpolateSmooth(2.0, this.red, CrossHair.mc.crosshairTarget instanceof EntityHitResult ? 5.0f : 1.0f);
        int firstColor = ColorAssist.multRed(ColorAssist.WHITE, this.red);
        int secondColor = ColorAssist.BLACK;
        float x = (float)window.getScaledWidth() / 2.0f;
        float y = (float)window.getScaledHeight() / 2.0f;
        float cooldown = (float)this.attackSetting.getInt() - (float)this.attackSetting.getInt() * CrossHair.mc.player.getAttackCooldownProgress(tickCounter.getTickProgress(false));
        float size = this.size1Setting.getValue();
        float size2 = this.size2Setting.getValue();
        float offset = size2 / 2.0f;
        float indent = (float)this.indentSetting.getInt() + cooldown;
        this.renderMain(x, y, size, size2, 1.0f, indent, offset, secondColor);
        this.renderMain(x, y, size, size2, 0.0f, indent, offset, firstColor);
    }

    private void renderMain(float x, float y, float size, float size2, float padding, float indent, float offset, int color) {
        Render2D.drawQuad(x - offset - padding / 2.0f, y - size - indent - padding / 2.0f, size2 + padding, size + padding, color);
        Render2D.drawQuad(x - offset - padding / 2.0f, y + indent - padding / 2.0f, size2 + padding, size + padding, color);
        Render2D.drawQuad(x - size - indent - padding / 2.0f, y - offset - padding / 2.0f, size + padding, size2 + padding, color);
        Render2D.drawQuad(x + indent - padding / 2.0f, y - offset - padding / 2.0f, size + padding, size2 + padding, color);
    }
}
