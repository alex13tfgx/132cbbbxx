/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1291
 *  net.minecraft.class_1293
 *  net.minecraft.class_1294
 *  net.minecraft.class_1309
 *  net.minecraft.class_2561
 *  net.minecraft.class_2596
 *  net.minecraft.class_2678
 *  net.minecraft.class_2718
 *  net.minecraft.class_2724
 *  net.minecraft.class_2783
 *  net.minecraft.class_2960
 *  net.minecraft.class_332
 *  net.minecraft.class_4587
 *  net.minecraft.class_6880
 *  net.minecraft.class_7923
 */
package code.wiosvisual.display.hud;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.common.animation.Animation;
import code.wiosvisual.common.animation.Direction;
import code.wiosvisual.common.animation.implement.Decelerate;
import code.wiosvisual.common.animation.implement.EaseOut;
import code.wiosvisual.display.hud.HudListPanel;
import code.wiosvisual.display.hud.Notifications;
import code.wiosvisual.display.hud.TimerTextAnimator;
import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.utils.client.EffectNames;
import code.wiosvisual.utils.client.managers.api.draggable.AbstractDraggable;
import code.wiosvisual.utils.client.sound.SoundManager;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.BackgroundIcons;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import code.wiosvisual.utils.interactions.interact.PlayerInteractionHelper;
import code.wiosvisual.utils.theme.ThemeManager;
import java.awt.Color;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class Potions
extends AbstractDraggable {
    /** Панель тянется шире содержимого, чтобы строки не липли к краям. */
    private static final float ROW_EXTRA_WIDTH = 26.0f;

    private final List<Potion> list = new ArrayList<Potion>();
    private final List<Potion> renderSnapshot = new ArrayList<Potion>();
    private static final RegistryEntry<StatusEffect>[] NEGATIVE_EFFECTS = new RegistryEntry[]{StatusEffects.POISON, StatusEffects.WITHER, StatusEffects.NAUSEA, StatusEffects.BLINDNESS, StatusEffects.HUNGER, StatusEffects.SLOWNESS, StatusEffects.MINING_FATIGUE, StatusEffects.INSTANT_DAMAGE, StatusEffects.WEAKNESS, StatusEffects.LEVITATION, StatusEffects.UNLUCK, StatusEffects.BAD_OMEN};
    private long lastEffectChange = 0L;
    private RegistryEntry<StatusEffect> currentRandomEffect = StatusEffects.SPEED;
    private static List<RegistryEntry<StatusEffect>> cachedAllEffects;
    private static final Random RANDOM;
    private final FontRenderer fontMedium = Fonts.getSize(13, Fonts.Type.SuisseIntlMedium);
    private final FontRenderer fontHeader = Fonts.getSize(16, Fonts.Type.DEFAULT);
    private final FontRenderer fontLevel = Fonts.getSize(11, Fonts.Type.SuisseIntlMedium);
    private final FontRenderer fontDuration = Fonts.getSize(13, Fonts.Type.DEFAULT);
    private final FontRenderer fontPotionName = Fonts.getSize(14, Fonts.Type.SuisseIntlMedium);
    private final Map<String, StatusEffectInstance> activeEffects = new HashMap<String, StatusEffectInstance>();
    private final Map<String, Boolean> warnedEffects = new HashMap<String, Boolean>();

    public Potions() {
        super("Potions", 200, 40, 90, 23, true);
        this.scaleAnimation = new EaseOut().setValue(1.0).setMs(100);
    }

    @Override
    public boolean visible() {
        Hud.getInstance();
        if (!Hud.interfaceSettings.isSelected("Potions") || !Hud.getInstance().state) {
            return false;
        }
        return this.visibleEffects() || PlayerInteractionHelper.isChat(Potions.mc.currentScreen);
    }

    @Override
    public void tick() {
        long currentTime;
        this.list.removeIf(p -> p.anim.isFinished(Direction.BACKWARDS));
        // TODO(port 1.21.11): StatusEffectInstance.method_5585(update) стал серверным (ServerWorld,LivingEntity,Runnable); в HUD не тикаем.
        if (Hud.getInstance().notificationSettings.isSelected("Заканчивающемся зелье") && !PlayerInteractionHelper.nullCheck()) {
            ArrayList<String> effectsToRemove = new ArrayList<String>();
            for (Map.Entry<String, StatusEffectInstance> entry : this.activeEffects.entrySet()) {
                String effectKey = entry.getKey();
                StatusEffectInstance trackedEffect = entry.getValue();
                boolean stillActive = false;
                if (Potions.mc.player != null) {
                    for (StatusEffectInstance activeEffect : Potions.mc.player.getStatusEffects()) {
                        if (!activeEffect.getEffectType().getIdAsString().equals(trackedEffect.getEffectType().getIdAsString())) continue;
                        stillActive = true;
                        this.activeEffects.put(effectKey, activeEffect);
                        int remainingTicks = activeEffect.getDuration();
                        int warningTicks = 200;
                        boolean alreadyWarned = this.warnedEffects.getOrDefault(effectKey, false);
                        if (remainingTicks <= warningTicks && remainingTicks > 0 && !alreadyWarned) {
                            this.sendPotionEndingSoonNotification(activeEffect);
                            this.warnedEffects.put(effectKey, true);
                        }
                        if (remainingTicks <= warningTicks) break;
                        this.warnedEffects.remove(effectKey);
                        break;
                    }
                }
                if (stillActive) continue;
                this.sendPotionEndedNotification(trackedEffect);
                effectsToRemove.add(effectKey);
                this.warnedEffects.remove(effectKey);
            }
            effectsToRemove.forEach(this.activeEffects::remove);
        }
        if (this.list.isEmpty() && PlayerInteractionHelper.isChat(Potions.mc.currentScreen) && (currentTime = System.currentTimeMillis()) - this.lastEffectChange >= 1000L) {
            if (cachedAllEffects == null) {
                cachedAllEffects = new ArrayList<RegistryEntry<StatusEffect>>();
                for (Identifier id : Registries.STATUS_EFFECT.getIds()) {
                    Registries.STATUS_EFFECT.getEntry(id).ifPresent(cachedAllEffects::add);
                }
            }
            if (!cachedAllEffects.isEmpty()) {
                this.currentRandomEffect = cachedAllEffects.get(RANDOM.nextInt(cachedAllEffects.size()));
                this.lastEffectChange = currentTime;
            }
        }
    }

    @Override
    public void packet(PacketEvent e) {
        Packet<?> packet2 = e.getPacket();
        Objects.requireNonNull(packet2);
        Packet<?> packet3 = packet2;
        int n = 0;
        switch (code.wiosvisual.utils.TypeSwitch.typeSwitch(new Object[]{EntityStatusEffectS2CPacket.class, RemoveEntityStatusEffectS2CPacket.class, PlayerRespawnS2CPacket.class, GameJoinS2CPacket.class}, packet3, n)) {
            case 0: {
                EntityStatusEffectS2CPacket effect = (EntityStatusEffectS2CPacket)packet3;
                if (PlayerInteractionHelper.nullCheck() || effect.getEntityId() != Objects.requireNonNull(Potions.mc.player).getId()) break;
                RegistryEntry effectId = effect.getEffectId();
                String effectKey = effectId.getIdAsString();
                boolean isNewEffect = !this.activeEffects.containsKey(effectKey);
                this.list.stream().filter(p -> p.effect.getEffectType().getIdAsString().equals(effectKey)).forEach(s -> s.anim.setDirection(Direction.BACKWARDS));
                StatusEffectInstance newEffect = new StatusEffectInstance(effectId, effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon());
                this.list.add(new Potion(newEffect, new Decelerate().setMs(150).setValue(1.0)));
                this.activeEffects.put(effectKey, newEffect);
                if (!isNewEffect || !Hud.getInstance().notificationSettings.isSelected("Полученном зелье")) break;
                this.sendPotionReceivedNotification(newEffect);
                break;
            }
            case 1: {
                RemoveEntityStatusEffectS2CPacket effect = (RemoveEntityStatusEffectS2CPacket)packet3;
                String effectKey = effect.effect().getIdAsString();
                this.list.stream().filter(s -> s.effect.getEffectType().getIdAsString().equals(effectKey)).forEach(s -> s.anim.setDirection(Direction.BACKWARDS));
                StatusEffectInstance removedEffect = this.activeEffects.remove(effectKey);
                if (removedEffect != null && Hud.getInstance().notificationSettings.isSelected("Заканчивающемся зелье")) {
                    this.sendPotionEndedNotification(removedEffect);
                }
                this.warnedEffects.remove(effectKey);
                break;
            }
            case 2: {
                PlayerRespawnS2CPacket p2 = (PlayerRespawnS2CPacket)packet3;
                this.list.clear();
                this.activeEffects.clear();
                this.warnedEffects.clear();
                break;
            }
            case 3: {
                GameJoinS2CPacket p3 = (GameJoinS2CPacket)packet3;
                this.list.clear();
                this.activeEffects.clear();
                this.warnedEffects.clear();
                break;
            }
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        this.drawMode1(context);
    }

    private final DeltaHud.ListPanel deltaPanel = new DeltaHud.ListPanel();
    {
        this.deltaPanel.rightPill = true;
    }
    private final FontRenderer deltaHeaderFont = Fonts.getSize(14, Fonts.Type.DEFAULT);

    private void drawMode1(DrawContext context) {
        MatrixStack matrix = GuiCompat.stack(context);
        this.renderSnapshot.clear();
        this.renderSnapshot.addAll(this.list);
        List<DeltaHud.Row> rows = new ArrayList<DeltaHud.Row>();
        if (!this.visibleEffects() && PlayerInteractionHelper.isChat(Potions.mc.currentScreen)) {
            rows.add(new DeltaHud.Row(1.0f, "Speed II", "1:30", 0xFFFFFFFF, null));
        } else {
            for (Potion potion : this.renderSnapshot) {
                float animValue = potion.anim.getOutput().floatValue();
                if (!(animValue > 0.0f)) continue;
                StatusEffectInstance effect = potion.effect;
                if (EffectNames.hidden(effect)) continue;
                String name = EffectNames.of(effect.getEffectType());
                int amplifier = effect.getAmplifier();
                if (amplifier > 0) {
                    name = name + " " + roman(amplifier + 1);
                }
                String duration = this.getDuration(effect);
                if (duration.equals("**:**")) {
                    duration = "\u221e";
                }
                boolean bad = this.isBadEffect((RegistryEntry<StatusEffect>)effect.getEffectType());
                int alpha = 255;
                if (effect.getDuration() <= 200 && effect.getDuration() > 0) {
                    double output = 0.5 + 0.5 * Math.cos(Math.PI * 2 * (double)(System.currentTimeMillis() % 700L) / 700.0);
                    alpha = (int)(100.0 + 155.0 * output);
                } else if (effect.getDuration() == 0) {
                    alpha = 0;
                }
                int color = alpha << 24 | (bad ? 0xD74C4C : 0xFFFFFF);
                rows.add(new DeltaHud.Row(animValue, name, duration, color, null));
            }
        }
        float[] size = this.deltaPanel.draw(context, matrix, rectangle, this.getX(), this.getY(), BackgroundIcons.POTIONS,
                HudTheme.potionsIconBright(255), "Potions", this.deltaHeaderFont, this.fontMedium, rows);
        this.setWidth((int)Math.ceil(size[0]));
        this.setHeight((int)Math.ceil(size[1]));
    }

    /** Спрайты эффектов в 1.21.11 доступны только через атлас GUI, поэтому значок — скруглённый квадрат цвета эффекта. */
    private DeltaHud.IconDrawer effectSquare(int rgb) {
        return (ctx, m, x, y, size, a) -> {
            int c = Math.round(255.0f * a) << 24 | rgb & 0xFFFFFF;
            rectangle.render(ShapeProperties.create(m, x, y, size, size).round(2.5f).color(c, c, c, c).build());
            Rectangle.flushPending();
        };
    }

    private boolean visibleEffects() {
        return this.list.stream().anyMatch(p -> !EffectNames.hidden(p.effect));
    }

    private String getDuration(StatusEffectInstance pe) {
        int var1 = pe.getDuration();
        int mins = var1 / 1200;
        return EffectNames.isInfinite(pe) ? "**:**" : mins + ":" + String.format("%02d", var1 % 1200 / 20);
    }

    private static String roman(int level) {
        String[] table = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return level >= 1 && level <= table.length ? table[level - 1] : String.valueOf(level);
    }

    private boolean isBadEffect(RegistryEntry<StatusEffect> effect) {
        for (RegistryEntry<StatusEffect> negativeEffect : NEGATIVE_EFFECTS) {
            if (effect != negativeEffect) continue;
            return true;
        }
        return false;
    }

    private void sendPotionReceivedNotification(StatusEffectInstance effect) {
        if (EffectNames.hidden(effect)) return;
        String effectName = EffectNames.of(effect.getEffectType());
        String message = effectName + " получен!";
        Notifications.getInstance().addList((Text)Text.literal((String)message), 1000, (Object)SoundManager.ENABLE_MODULE, false, null);
        SoundManager.playSound(SoundManager.ENABLE_MODULE);
    }

    private void sendPotionEndedNotification(StatusEffectInstance effect) {
        if (EffectNames.hidden(effect)) return;
        String effectName = EffectNames.of(effect.getEffectType());
        String message = effectName + " закончился!";
        Notifications.getInstance().addList((Text)Text.literal((String)message), 1000, (Object)SoundManager.DISABLE_MODULE, false, null);
        SoundManager.playSound(SoundManager.DISABLE_MODULE);
    }

    private void sendPotionEndingSoonNotification(StatusEffectInstance effect) {
        if (EffectNames.hidden(effect)) return;
        String effectName = EffectNames.of(effect.getEffectType());
        String message = effectName + " закончится через 10 секунд!";
        Notifications.getInstance().addList((Text)Text.literal((String)message), 1000, null, false, null);
    }

    private void drawMode2(DrawContext context) {
        boolean showExamples;
        MatrixStack matrix = GuiCompat.stack(context);
        this.renderSnapshot.clear();
        this.renderSnapshot.addAll(this.list);
        FontRenderer titleFont = Fonts.getSize(14, Fonts.Type.BOLD);
        FontRenderer moduleFont = Fonts.getSize(13, Fonts.Type.BOLD);
        FontRenderer levelFont = Fonts.getSize(11, Fonts.Type.BOLD);
        FontRenderer durationFont = Fonts.getSize(12, Fonts.Type.BOLD);
        float posX = this.getX();
        float posY = this.getY();
        float panelWidth = 95.0f;
        float titleHeight = 24.0f;
        float potionHeight = 16.0f;
        float spacing = -3.5f;
        float titleSpacing = -1.5f;
        int potionCount = 0;
        for (Potion potion : this.renderSnapshot) {
            if (!(potion.anim.getOutput().floatValue() > 0.0f)) continue;
            ++potionCount;
        }
        boolean bl = showExamples = this.list.isEmpty() && PlayerInteractionHelper.isChat(Potions.mc.currentScreen);
        if (showExamples) {
            potionCount = 3;
        }
        float contentHeight = (float)potionCount * (potionHeight + spacing) - (potionCount > 0 ? spacing : 0.0f);
        float totalHeight = titleHeight + titleSpacing + 7.0f + contentHeight;
        this.setWidth((int)panelWidth);
        this.setHeight((int)totalHeight);
        rectangle.render(ShapeProperties.create(matrix, posX, posY, panelWidth, totalHeight).round(7.0f).color(ThemeManager.BackgroundGui.getColor()).build());
        rectangle.render(ShapeProperties.create(matrix, posX + 4.5f, posY + 4.5f, panelWidth - 9.0f, titleHeight - 9.0f).round(3.5f).color(ThemeManager.BackgroundSettings.getColor()).build());
        titleFont.drawString(matrix, "Potions", posX + 10.0f, posY + 10.0f, ThemeManager.textColor.getColor());
        Fonts.getSize(19, Fonts.Type.ICONS).drawString(matrix, "D", posX + panelWidth - 19.0f, posY + 9.5f, ThemeManager.textColor.getColor());
        float currentY = posY + titleHeight + titleSpacing;
        rectangle.render(ShapeProperties.create(matrix, posX + 4.5f, currentY, panelWidth - 9.0f, contentHeight).round(3.5f).color(ThemeManager.BackgroundSettings.getColor()).build());
        if (showExamples) {
            String[] exampleNames = new String[]{"Speed", "Strength", "Regen"};
            String[] exampleDurations = new String[]{"5:00", "3:30", "10:00"};
            for (int i = 0; i < 3; ++i) {
                this.drawPotionEntry(matrix, currentY, posX, potionHeight, moduleFont, levelFont, durationFont, exampleNames[i], exampleDurations[i], 0, 1.0f, false, this.currentRandomEffect);
                currentY += potionHeight + spacing;
            }
        } else {
            for (Potion potion : this.renderSnapshot) {
                float animValue = potion.anim.getOutput().floatValue();
                if (!(animValue > 0.0f)) continue;
                StatusEffectInstance effect = potion.effect;
                String name = EffectNames.of(effect.getEffectType());
                String duration = this.getDuration(effect);
                int amplifier = effect.getAmplifier();
                boolean isBad = this.isBadEffect((RegistryEntry<StatusEffect>)effect.getEffectType());
                this.drawPotionEntry(matrix, currentY, posX, potionHeight, moduleFont, levelFont, durationFont, name, duration, amplifier, animValue, isBad, (RegistryEntry<StatusEffect>)effect.getEffectType());
                currentY += (potionHeight + spacing) * animValue;
            }
        }
    }

    private void drawPotionEntry(MatrixStack matrix, float currentY, float posX, float potionHeight, FontRenderer moduleFont, FontRenderer levelFont, FontRenderer durationFont, String name, String duration, int amplifier, float animValue, boolean isBad, RegistryEntry<StatusEffect> effectType) {
        float barWidth = 1.3f;
        float barHeight = 7.0f;
        float barX = posX + 4.5f + 2.8f;
        float barY = currentY + 5.5f;
        int alpha = (int)(255.0f * animValue);
        Color barColor1 = new Color(HudTheme.hudGray(255) >> 16 & 0xFF, HudTheme.hudGray(255) >> 8 & 0xFF, HudTheme.hudGray(255) & 0xFF, alpha);
        Color barColor2 = new Color(HudTheme.hudGraySecondary(255) >> 16 & 0xFF, HudTheme.hudGraySecondary(255) >> 8 & 0xFF, HudTheme.hudGraySecondary(255) & 0xFF, alpha);
        rectangle.render(ShapeProperties.create(matrix, barX, barY, barWidth, barHeight * animValue).round(0.9f).color(barColor1.getRGB(), barColor2.getRGB(), barColor1.getRGB(), barColor2.getRGB()).build());
        float iconX = barX + barWidth + 3.5f;
        float iconY = currentY + potionHeight - 11.0f;
        /* TODO(port 1.21.11): API спрайтов эффектов (method_18505/method_18663) удалён — иконка эффекта не рисуется. */
        float nameX = iconX + 11.0f;
        float nameY = currentY + potionHeight - 9.0f;
        Color textColor = isBad ? new Color(255, 85, 75, alpha) : new Color(ThemeManager.textColor.getColor() >> 16 & 0xFF, ThemeManager.textColor.getColor() >> 8 & 0xFF, ThemeManager.textColor.getColor() & 0xFF, alpha);
        moduleFont.drawString(matrix, name, nameX, nameY, textColor.getRGB());
        if (amplifier > 0) {
            String level = " " + (amplifier + 1);
            float levelX = nameX + moduleFont.getStringWidth(name) + 3.0f;
            Color levelColor = new Color(157, 157, 160, alpha);
            float levelY = HudListPanel.alignedRowTextY(moduleFont, levelFont, currentY);
            levelFont.drawString(matrix, level, levelX, levelY, levelColor.getRGB());
        }
        float durationWidth = durationFont.getStringWidth(duration);
        float durationX = posX + 95.0f - 6.5f - durationWidth;
        Color durationColor = new Color(150, 150, 150, alpha);
        durationFont.drawString(matrix, duration, durationX, nameY, durationColor.getRGB());
    }

    static {
        RANDOM = new Random();
    }

    private record Potion(StatusEffectInstance effect, Animation anim) {
    }
}
