/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1792
 *  net.minecraft.class_1802
 *  net.minecraft.class_2596
 *  net.minecraft.class_2656
 *  net.minecraft.class_2724
 *  net.minecraft.class_332
 *  net.minecraft.class_4587
 *  net.minecraft.class_7923
 */
package code.wiosvisual.display.hud;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.common.animation.Animation;
import code.wiosvisual.common.animation.Direction;
import code.wiosvisual.common.animation.implement.Decelerate;
import code.wiosvisual.common.animation.implement.EaseOut;
import code.wiosvisual.display.hud.HudListPanel;
import code.wiosvisual.display.hud.TimerTextAnimator;
import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.chat.StringHelper;
import code.wiosvisual.utils.client.managers.api.draggable.AbstractDraggable;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.BackgroundIcons;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import code.wiosvisual.utils.interactions.interact.PlayerInteractionHelper;
import code.wiosvisual.utils.math.time.StopWatch;
import code.wiosvisual.utils.theme.ThemeManager;
import java.awt.Color;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.registry.Registries;

public class CoolDowns
extends AbstractDraggable {
    /** Панель тянется шире содержимого, чтобы строки не липли к краям. */
    private static final float ROW_EXTRA_WIDTH = 26.0f;

    public final List<CoolDown> list = new ArrayList<CoolDown>();
    private final List<CoolDown> renderSnapshot = new ArrayList<CoolDown>();
    private long lastItemChange = 0L;
    private int currentItemIndex = 0;
    private static final Item[] EXAMPLE_ITEMS = new Item[]{Items.ENDER_EYE, Items.ENDER_PEARL, Items.SUGAR, Items.MACE, Items.ENCHANTED_GOLDEN_APPLE, Items.TRIDENT, Items.CROSSBOW, Items.DRIED_KELP, Items.NETHERITE_SCRAP};
    private final FontRenderer fontMedium = Fonts.getSize(13, Fonts.Type.SuisseIntlMedium);
    private final FontRenderer fontHeader = Fonts.getSize(16, Fonts.Type.DEFAULT);
    private final FontRenderer fontDuration = Fonts.getSize(13, Fonts.Type.DEFAULT);
    private final FontRenderer fontItemName = Fonts.getSize(14, Fonts.Type.SuisseIntlMedium);

    public static CoolDowns getInstance() {
        return Instance.getDraggable(CoolDowns.class);
    }

    public CoolDowns() {
        super("Cooldowns", 10, 40, 80, 23, true);
        this.scaleAnimation = new EaseOut().setValue(1.0).setMs(100);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public boolean visible() {
        if (this.list.isEmpty()) {
            if (!PlayerInteractionHelper.isChat(CoolDowns.mc.currentScreen)) return false;
        }
        Hud.getInstance();
        if (!Hud.interfaceSettings.isSelected("Cooldowns")) return false;
        if (!Hud.getInstance().state) return false;
        return true;
    }

    @Override
    public void tick() {
        long currentTime;
        this.list.removeIf(c -> c.anim.isFinished(Direction.BACKWARDS));
        this.list.stream().filter(c -> !Objects.requireNonNull(CoolDowns.mc.player).getItemCooldownManager().isCoolingDown(c.item.getDefaultStack())).forEach(coolDown -> coolDown.anim.setDirection(Direction.BACKWARDS));
        if (this.list.isEmpty() && PlayerInteractionHelper.isChat(CoolDowns.mc.currentScreen) && (currentTime = System.currentTimeMillis()) - this.lastItemChange >= 1000L) {
            this.currentItemIndex = (this.currentItemIndex + 1) % EXAMPLE_ITEMS.length;
            this.lastItemChange = currentTime;
        }
    }

    @Override
    public void packet(PacketEvent e) {
        if (PlayerInteractionHelper.nullCheck()) {
            return;
        }
        Packet<?> packet2 = e.getPacket();
        Objects.requireNonNull(packet2);
        Packet<?> packet3 = packet2;
        int n = 0;
        switch (code.wiosvisual.utils.TypeSwitch.typeSwitch(new Object[]{CooldownUpdateS2CPacket.class, PlayerRespawnS2CPacket.class}, packet3, n)) {
            case 0: {
                CooldownUpdateS2CPacket c = (CooldownUpdateS2CPacket)packet3;
                Item item = (Item)Registries.ITEM.get(c.cooldownGroup());
                this.list.stream().filter(coolDown -> coolDown.item.equals(item)).forEach(coolDown -> coolDown.anim.setDirection(Direction.BACKWARDS));
                if (c.cooldown() == 0) break;
                this.list.add(new CoolDown(item, new StopWatch().setMs((long)(-c.cooldown()) * 50L), new Decelerate().setMs(150).setValue(1.0)));
                break;
            }
            case 1: {
                PlayerRespawnS2CPacket p = (PlayerRespawnS2CPacket)packet3;
                this.list.clear();
                break;
            }
        }
    }

    /** "4m 44s" once a minute or more remains, otherwise one decimal of seconds ("49,5s"). */
    private static String formatCooldown(long millis) {
        if (millis < 0L) {
            millis = 0L;
        }
        long totalSeconds = millis / 1000L;
        if (totalSeconds >= 60L) {
            return totalSeconds / 60L + "m " + totalSeconds % 60L + "s";
        }
        return String.format(java.util.Locale.ROOT, "%.1f", (double)millis / 1000.0).replace('.', ',') + "s";
    }

    private final DeltaHud.ListPanel deltaPanel = new DeltaHud.ListPanel();
    private final FontRenderer deltaHeaderFont = Fonts.getSize(14, Fonts.Type.DEFAULT);

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = GuiCompat.stack(context);
        this.renderSnapshot.clear();
        this.renderSnapshot.addAll(this.list);
        List<DeltaHud.Row> rows = new ArrayList<DeltaHud.Row>();
        if (this.list.isEmpty() && PlayerInteractionHelper.isChat(CoolDowns.mc.currentScreen)) {
            rows.add(new DeltaHud.Row(1.0f, "Example", "**:**", 0xFFFFFFFF, null));
        } else {
            for (CoolDown coolDown : this.renderSnapshot) {
                float animValue = coolDown.anim.getOutput().floatValue();
                if (!(animValue > 0.0f)) continue;
                String name = coolDown.item.getDefaultStack().getName().getString();
                if (name.equals("\u0417\u0430\u0447\u0430\u0440\u043e\u0432\u0430\u043d\u043d\u043e\u0435 \u0437\u043e\u043b\u043e\u0442\u043e\u0435 \u044f\u0431\u043b\u043e\u043a\u043e")) {
                    name = "\u0427\u0430\u0440\u043a\u0430";
                }
                rows.add(new DeltaHud.Row(animValue, name, CoolDowns.formatCooldown(-coolDown.time.elapsedTime()), 0xFFFFFFFF, null));
            }
        }
        float[] size = this.deltaPanel.draw(context, matrix, rectangle, this.getX(), this.getY(), BackgroundIcons.COOLDOWNS,
                HudTheme.cooldownsIconBright(255), "Cooldowns", this.deltaHeaderFont, this.fontMedium, rows);
        this.setWidth((int)Math.ceil(size[0]));
        this.setHeight((int)Math.ceil(size[1]));
    }

    /**
     * Остаток перезарядки предмета в секундах (0 — предмет не на кд).
     * Используется отрисовкой кд прямо в слотах инвентаря.
     */
    public static int getRemainingSeconds(ItemStack stack) {
        CoolDowns instance = CoolDowns.getInstance();
        if (instance == null || stack == null || stack.isEmpty() || CoolDowns.mc.player == null) {
            return 0;
        }
        if (!CoolDowns.mc.player.getItemCooldownManager().isCoolingDown(stack)) {
            return 0;
        }
        Item item = stack.getItem();
        for (CoolDown coolDown : instance.list) {
            if (!coolDown.item.equals(item)) continue;
            return Math.max((int)(-coolDown.time.elapsedTime() / 1000L), 0);
        }
        return 0;
    }

    public record CoolDown(Item item, StopWatch time, Animation anim) {
    }
}
