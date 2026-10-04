/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1306
 *  net.minecraft.class_3532
 *  net.minecraft.class_4587
 *  net.minecraft.class_7833
 */
package code.wiosvisual.features.impl.render;

import code.wiosvisual.events.item.HandAnimationEvent;
import code.wiosvisual.events.item.SwingDurationEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.MultiSelectSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class SwingAnimations
extends Module {
    private boolean swimmingAnimation = true;
    private boolean climbAndCrawl = true;
    private boolean mb3DCompat = false;
    private final SelectSetting hmiAnimationType = new SelectSetting("Вид анимации", "Вид анимации").value("Классик", "Шарп").visible(this::isHmiMode);
    private final MultiSelectSetting hmiClassicSwing = new MultiSelectSetting("Стиль удара", "Какие удары использовать в классическом режиме").value("Через лево", "Через право").selected("Через лево", "Через право").visible(() -> this.isHmiMode() && this.hmiAnimationType.isSelected("Классик"));
    private final SliderSettings hmiSmoothness = new SliderSettings("Плавность", "Плавность движения удара. 1 — как сейчас, 100 — максимально плавно").setValue(1.0f).range(1.0f, 100.0f).step(1.0f).visible(this::isHmiMode);
    private final BooleanSetting swingEnabled = new BooleanSetting("Анимация свинга", "Включить анимацию свинга").setValue(true);
    private final SelectSetting swingType = new SelectSetting("Тип свинга", "Тип анимации свинга").value("Smooth", "Static", "Down", "DropDown", "Poke", "SelfBack", "Feast", "ToBack", "Block", "Akrien", "Break", "Pander", "Slant", "Chop", "Swipe", "Twist", "Smooth 2", "Power", "Stab", "Helix", "Default", "HMI", "Attack Indicator").visible(this.swingEnabled::isValue);
    private final BooleanSetting onlySwing = new BooleanSetting("Только при ударе", "Вне удара рука стоит в обычной позиции").setValue(false).visible(() -> this.isVanillaMode() && !this.swingType.isSelected("Attack Indicator"));
    private final SliderSettings swingStrength = new SliderSettings("Сила анимации", "Сила анимации свинга").setValue(1.0f).range(0.1f, 3.0f).step(0.01f).visible(() -> this.isVanillaMode() && !this.swingType.isSelected("Pander"));
    private final SliderSettings corner = new SliderSettings("Угол DropDown", "Угол для DropDown").setValue(12.0f).range(1.0f, 360.0f).step(1.0f).visible(() -> this.isVanillaMode() && this.swingType.isSelected("DropDown"));
    private final SliderSettings slant = new SliderSettings("Наклон DropDown", "Наклон для DropDown").setValue(12.0f).range(1.0f, 360.0f).step(1.0f).visible(() -> this.isVanillaMode() && this.swingType.isSelected("DropDown"));
    private final BooleanSetting smoothEnabled = new BooleanSetting("Плавная анимация", "Замедление анимации").setValue(false).visible(() -> !this.isHmiMode());
    private final SliderSettings slowAnimationSpeed = new SliderSettings("Скорость анимации", "Скорость плавной анимации").setValue(12.0f).range(1.0f, 50.0f).step(1.0f).visible(() -> !this.isHmiMode() && this.smoothEnabled.isValue());

    public SwingAnimations() {
        super("SwingAnimations", "Swing Animations", ModuleCategory.RENDER);
        this.setup(this.swingEnabled, this.swingType, this.hmiAnimationType, this.hmiClassicSwing, this.hmiSmoothness, this.onlySwing, this.swingStrength, this.corner, this.slant, this.smoothEnabled, this.slowAnimationSpeed);
    }

    public static SwingAnimations getInstance() {
        return Instance.get(SwingAnimations.class);
    }

    /** HMI — отдельный режим в списке «Тип свинга»: руки рисует свой рендер. */
    public boolean isHmiMode() {
        return this.swingEnabled != null && this.swingType != null && this.swingEnabled.isValue() && this.swingType.isSelected("HMI");
    }

    /** Обычные режимы свинга, которые двигают руку через HandAnimationEvent. */
    private boolean isVanillaMode() {
        return this.swingEnabled.isValue() && !this.swingType.isSelected("HMI");
    }

    public boolean shouldRestartSwingOnRetrigger() {
        return this.isState() && (this.isHmiMode() || this.smoothEnabled.isValue());
    }

    public boolean isSharpAnimation() {
        return this.hmiAnimationType.isSelected("\u0428\u0430\u0440\u043f");
    }

    public boolean shouldAlternateClassicSwordSwing() {
        return !this.isSharpAnimation() && this.hmiClassicSwing.isSelected("\u0427\u0435\u0440\u0435\u0437 \u043b\u0435\u0432\u043e") && this.hmiClassicSwing.isSelected("\u0427\u0435\u0440\u0435\u0437 \u043f\u0440\u0430\u0432\u043e");
    }

    public boolean resolveClassicSwordLeft(boolean alternateLeft) {
        boolean allowLeft = this.hmiClassicSwing.isSelected("\u0427\u0435\u0440\u0435\u0437 \u043b\u0435\u0432\u043e");
        boolean allowRight = this.hmiClassicSwing.isSelected("\u0427\u0435\u0440\u0435\u0437 \u043f\u0440\u0430\u0432\u043e");
        if (allowLeft && allowRight) {
            return alternateLeft;
        }
        if (allowLeft) {
            return true;
        }
        if (allowRight) {
            return false;
        }
        return alternateLeft;
    }

    public float getHmiSmoothness() {
        return this.hmiSmoothness.getValue();
    }

    public int getHmiSwingDuration() {
        float value = this.hmiSmoothness.getValue();
        if (value <= 1.0f) {
            return 6;
        }
        return Math.round(MathHelper.lerp((float)((value - 1.0f) / 99.0f), (float)6.0f, (float)80.0f));
    }

    public float remapHmiSwingProgress(float progress) {
        float t = MathHelper.clamp((float)progress, (float)0.0f, (float)1.0f);
        float value = this.hmiSmoothness.getValue();
        if (value <= 1.0f) {
            return t;
        }
        float amount = (value - 1.0f) / 99.0f;
        float eased = t * t * (3.0f - 2.0f * t);
        return MathHelper.lerp((float)(amount * 0.35f), (float)t, (float)eased);
    }

    public boolean isSwimmingAnimation() {
        return this.swimmingAnimation;
    }

    public boolean isClimbAndCrawl() {
        return this.climbAndCrawl;
    }

    public boolean isMb3DCompat() {
        return this.mb3DCompat;
    }

    @EventHandler
    public void onSwingDuration(SwingDurationEvent e) {
        if (!this.isState()) {
            return;
        }
        if (this.isHmiMode()) {
            if (this.hmiSmoothness.getValue() > 1.0f) {
                e.setAnimation(this.getHmiSwingDuration());
                e.cancel();
            }
            return;
        }
        if (this.smoothEnabled.isValue()) {
            e.setAnimation(this.slowAnimationSpeed.getValue());
            e.cancel();
        }
    }

    @EventHandler
    public void onHandAnimation(HandAnimationEvent e) {
        if (!this.isState() || this.isHmiMode() || !this.swingEnabled.isValue()) {
            return;
        }
        if (SwingAnimations.mc.player != null) {
            Arm expectedSwingArm = SwingAnimations.mc.player.getMainArm();
            if (e.getArm() != expectedSwingArm) {
                return;
            }
        }
        MatrixStack matrices = e.getMatrices();
        float swingProgress = e.getSwingProgress();
        float equipProgress = e.getEquipProgress();
        int i = e.getArm() == Arm.RIGHT ? 1 : -1;
        float strength = this.swingStrength.getValue();
        float sin1 = MathHelper.sin((float)(swingProgress * swingProgress * (float)Math.PI));
        float sin2 = MathHelper.sin((float)(MathHelper.sqrt((float)swingProgress) * (float)Math.PI));
        float sinSmooth = (float)(Math.sin((double)swingProgress * Math.PI) * 0.5);
        if (SwingAnimations.isExcludedSwingItem(e.getStack())) {
            return;
        }
        if (this.swingType.isSelected("Attack Indicator")) {
            this.applyAttackIndicator(matrices, i, sin2, strength);
            e.cancel();
            return;
        }
        if (this.onlySwing.isValue() && swingProgress == 0.0f) {
            matrices.translate((float)i * 0.56f, -0.52f, -0.72f);
            e.cancel();
            return;
        }
        boolean handled = true;
        switch (this.swingType.getSelected()) {
            case "Chop": {
                matrices.translate(0.56f * (float)i, -0.44f, -0.72f);
                matrices.translate(0.0f, 0.33f * -0.6f, 0.0f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0f * (float)i));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * -20.0f * (float)i * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0f * strength));
                matrices.translate(0.4f, 0.2f, 0.2f);
                matrices.translate(-0.5f, 0.08f, 0.0f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0f));
                break;
            }
            case "Twist": {
                matrices.translate((float)i * 0.56f, -0.36f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(80.0f * (float)i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -90.0f * strength));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((sin1 - sin2) * 60.0f * (float)i * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0f));
                matrices.translate(0.0f, -0.1f, 0.05f);
                break;
            }
            case "Swipe": {
                matrices.translate(0.56f * (float)i, -0.32f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(70.0f * (float)i));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0f * (float)i));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * sin1 * -5.0f * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * sin1 * -120.0f * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0f));
                break;
            }
            case "Default": {
                matrices.translate((float)i * 0.56f, -0.52f - sin2 * 0.5f * strength, -0.72f);
                break;
            }
            case "Smooth 2": {
                matrices.translate((float)i * 0.56f, -0.42f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0f * strength));
                matrices.translate(0.0f, -0.1f, 0.0f);
                break;
            }
            case "Power": {
                matrices.translate((float)i * 0.56f, -0.32f, -0.72f);
                matrices.translate(-sinSmooth * sinSmooth * sin1 * (float)i * strength, 0.0f, 0.0f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(61.0f * (float)i));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * strength));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * sin1 * -5.0f * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * sin1 * -30.0f * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sinSmooth * -60.0f * strength));
                break;
            }
            case "Stab": {
                matrices.translate((float)i * 0.66f, -0.32f, -0.92f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0f * (float)i));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 90.0f * (float)i * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -15.0f * strength));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(50.0f * (float)i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(70.0f * (float)i));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(2.0f * (float)i));
                break;
            }
            case "Helix": {
                matrices.translate((float)i * 0.66f, -0.32f, -0.92f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35.0f * (float)i));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 15.0f * (float)i * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -55.0f * strength));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(50.0f * (float)i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(70.0f * (float)i));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(2.0f * (float)i));
                break;
            }
            case "Down": {
                matrices.translate((float)i * 0.56f, -0.32f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(76 * i)));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5.0f * strength));
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100.0f * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155.0f * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0f));
                break;
            }
            case "Poke": {
                float anim = (float)Math.sin((double)swingProgress * 1.5707963267948966 * 2.0);
                float tilt = strength / 3.0f;
                matrices.translate((float)i * 0.56f, -0.52f, -0.72f);
                matrices.translate(0.0f, 0.0f, tilt * -anim);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(75.0f * (float)i));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((-75.0f * (strength / 4.0f) * anim - 60.0f) * (float)i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-75.0f));
                break;
            }
            case "Static": {
                matrices.translate((float)i * 0.56f, -0.42f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -60.0f * strength));
                matrices.translate(0.0, -0.1, 0.0);
                break;
            }
            case "Feast": {
                matrices.translate((float)i * 0.56f, -0.32f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(30 * i)));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 75.0f * (float)i * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -65.0f * strength));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(30 * i)));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(35 * i)));
                break;
            }
            case "Akrien": {
                matrices.translate((float)i * 0.65f, -0.32f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(76 * i)));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5.0f * strength));
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100.0f * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155.0f * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 25.0f * strength));
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -25.0f * strength));
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin1 * 15.0f * strength));
                matrices.translate(sin2 * 0.18f * strength, sin2 * 0.59f * strength, 0.0f);
                break;
            }
            case "Smooth": {
                this.applySwingOffset(matrices, i, swingProgress, strength);
                break;
            }
            case "Block": {
                if (swingProgress > 0.0f) {
                    float g = MathHelper.sin((float)(MathHelper.sqrt((float)swingProgress) * (float)Math.PI));
                    matrices.translate(0.56f * (float)i, equipProgress * -0.2f - 0.5f, -0.7f);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(45 * i)));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -85.0f * strength));
                    matrices.translate(-0.1f * (float)i, 0.28f, 0.2f);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-85.0f));
                    break;
                }
                float n = -0.4f * MathHelper.sin((float)(MathHelper.sqrt((float)swingProgress) * (float)Math.PI));
                float m = 0.2f * MathHelper.sin((float)(MathHelper.sqrt((float)swingProgress) * ((float)Math.PI * 2)));
                float f1 = -0.2f * MathHelper.sin((float)(swingProgress * (float)Math.PI));
                matrices.translate(n * (float)i * strength, m * strength, f1 * strength);
                this.applyEquipOffset(matrices, i, equipProgress);
                this.applySwingOffset(matrices, i, swingProgress, strength);
                break;
            }
            case "ToBack": {
                float g = MathHelper.sin((float)(MathHelper.sqrt((float)swingProgress) * (float)Math.PI));
                matrices.translate(0.65f * (float)i, -0.45f, -0.9f);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50.0f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((-30.0f * (1.0f - g * strength) - 30.0f) * (float)i));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110.0f * (float)i));
                break;
            }
            case "SelfBack": {
                float anim = (float)Math.sin((double)swingProgress * 1.5707963267948966 * 2.0);
                matrices.translate(0.65f * (float)i, -0.3f, -0.8f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(90 * i)));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-70 * i)));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0f - 60.0f * strength * anim));
                break;
            }
            case "Break": 
            case "\u0411\u0440\u0438\u043a": {
                matrices.translate(0.66f * (float)i, -0.3f, -0.38f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(270 * i)));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * 10.0f * strength));
                matrices.scale(0.5f, 0.5f, 0.5f);
                matrices.translate(-0.1f * (float)i, 0.2f, 0.0f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-10.0f * (float)i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-105.0f * (float)i));
                break;
            }
            case "DropDown": {
                float anim = (float)Math.sin((double)swingProgress * 1.5707963267948966 * 2.0);
                this.applyEquipOffset(matrices, i, 0.0f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(80.0f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.corner.getValue()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-this.slant.getValue() * anim * strength));
                break;
            }
            case "Pander": {
                float panderAnim = MathHelper.sin((float)(swingProgress * (float)Math.PI));
                float panderF = 1.0f - equipProgress;
                matrices.translate((float)i * 0.56f, -0.52f, -0.72f);
                matrices.translate((0.3f - panderAnim * 0.15f) * (float)i, 0.2f - panderF * 0.12f, -0.15f - panderAnim * 0.13f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((76.0f - 10.0f * panderAnim) * (float)i));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((-16.0f - 8.0f * panderAnim) * (float)i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-83.0f - 26.0f * panderAnim));
                break;
            }
            case "Slant": {
                float anim = (float)Math.sin((double)swingProgress * 1.5707963267948966 * 2.0);
                float rotate = 35.0f * strength;
                matrices.translate((float)i * 0.56f, -0.52f, -0.72f);
                matrices.translate(0.0f, 0.0f, -0.3f * anim * strength);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(anim * -rotate));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(anim * rotate));
                break;
            }
            default: {
                handled = false;
            }
        }
        if (handled) {
            e.cancel();
        }
    }

    /**
     * Attack Indicator: после удара рука опускается и отводится назад, а по мере
     * восстановления силы удара плавно возвращается. Поднялась полностью — можно бить.
     */
    private void applyAttackIndicator(MatrixStack matrices, int i, float sin2, float strength) {
        float cooldown = SwingAnimations.mc.player == null ? 1.0f : MathHelper.clamp(SwingAnimations.mc.player.getAttackCooldownProgress(tickCounter.getTickProgress(false)), 0.0f, 1.0f);
        float inv = 1.0f - cooldown;
        float down = inv * inv * (3.0f - 2.0f * inv) * strength;
        matrices.translate((float)i * 0.56f, -0.52f - 0.32f * down, -0.72f + 0.12f * down);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-25.0f * down));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * 12.0f * down));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -55.0f * strength));
    }

    /** Арбалет и трезубец держатся по-своему — кастомный свинг ломает их позу. */
    private static boolean isExcludedSwingItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && (stack.getItem() instanceof CrossbowItem || stack.getItem() instanceof TridentItem);
    }

    private void applyEquipOffset(MatrixStack matrices, int i, float equipProgress) {
        matrices.translate((float)i * 0.56f, -0.52f + equipProgress * -0.6f, -0.72f);
    }

    private void applySwingOffset(MatrixStack matrices, int i, float swingProgress, float strength) {
        float f = MathHelper.sin((float)(swingProgress * swingProgress * (float)Math.PI));
        matrices.translate(0.56f * (float)i, -0.52f, -0.72f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * (45.0f + f * -20.0f * strength)));
        float g = MathHelper.sin((float)(MathHelper.sqrt((float)swingProgress) * (float)Math.PI));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)i * g * -20.0f * strength));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0f * strength));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * -45.0f));
    }
}
