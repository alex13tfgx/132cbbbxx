/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_1707
 *  net.minecraft.class_1735
 *  net.minecraft.class_1738
 *  net.minecraft.class_1799
 *  net.minecraft.class_1810
 *  net.minecraft.class_1829
 *  net.minecraft.class_2371
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 *  net.minecraft.class_437
 *  net.minecraft.class_4587
 *  net.minecraft.class_476
 *  net.minecraft.class_5321
 *  net.minecraft.class_6880
 *  net.minecraft.class_9290
 *  net.minecraft.class_9304
 *  net.minecraft.class_9334
 */
package code.wiosvisual.features.impl.render;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.utils.display.render.pipeline.WiosRender;
import code.wiosvisual.events.container.HandledScreenEvent;
import code.wiosvisual.events.keyboard.KeyEvent;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BindSetting;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.RadioSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.chat.ChatCommandSender;
import code.wiosvisual.utils.client.chat.ChatMessage;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;

public class AuctionHelper
extends Module {
    static final Pattern PRICE_WITH_LABEL = Pattern.compile("(?iu)(?:\u0446\u0435\u043d\u0430|\u0441\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c|price|cost)\\s*[:]?\\s*([\\d\\s,._]+)");
    static final Pattern PRICE_WITH_CURRENCY = Pattern.compile("(?iu)(?:\\$|\u20bd|\u0440|\u0440\u0443\u0431|\u0440\\.)\\s*([\\d\\s,._]+)|([\\d\\s,._]+)\\s*(?:\\$|\u20bd|\u0440|\u0440\u0443\u0431|\u0440\\.)");
    static final Pattern DIGITS_PATTERN = Pattern.compile("([\\d][\\d\\s,._]{1,18})");
    private final BooleanSetting onlyMending = new BooleanSetting("\u0422\u043e\u043b\u044c\u043a\u043e \u043f\u043e\u0447\u0438\u043d\u043a\u0430", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u0442\u043e\u043b\u044c\u043a\u043e \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u044b \u0441 \u043f\u043e\u0447\u0438\u043d\u043a\u043e\u0439").setValue(false);
    private final BooleanSetting showPricePerItem = new BooleanSetting("\u0426\u0435\u043d\u0430 \u0437\u0430 1", "\u0414\u043e\u0431\u0430\u0432\u043b\u044f\u0442\u044c \u0432 \u043b\u043e\u0440 \u0446\u0435\u043d\u0443 \u0437\u0430 \u043e\u0434\u043d\u0443 \u0448\u0442\u0443\u043a\u0443").setValue(true);
    private final BooleanSetting enchantSort = new BooleanSetting("\u0424\u0438\u043b\u044c\u0442\u0440 \u0437\u0430\u0447\u0430\u0440\u043e\u0432", "\u041f\u043e\u0434\u0441\u0432\u0435\u0442\u043a\u0430 \u0438 \u0441\u043e\u0440\u0442\u0438\u0440\u043e\u0432\u043a\u0430 \u043f\u043e \u0437\u0430\u0447\u0430\u0440\u0430\u043c \u0438 \u043f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u0438").setValue(true);
    private final RadioSetting sortPreset = new RadioSetting("\u041f\u0440\u0435\u0441\u0435\u0442 \u0444\u0438\u043b\u044c\u0442\u0440\u0430", "\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0442\u0438\u043f \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432", new String[]{"\u041b\u044e\u0431\u044b\u0435", "\u0411\u0440\u043e\u043d\u044f Prot 5+", "\u041a\u0438\u0440\u043a\u0430 Eff 7-10", "\u041c\u0435\u0447 Sharp 7+"}, "\u041b\u044e\u0431\u044b\u0435").visible(this.enchantSort::isValue);
    private final BooleanSetting onlyMatching = new BooleanSetting("\u0422\u043e\u043b\u044c\u043a\u043e \u043f\u043e\u0434\u0445\u043e\u0434\u044f\u0449\u0438\u0435", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u0442\u043e\u043b\u044c\u043a\u043e \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u044b, \u043f\u043e\u0434\u0445\u043e\u0434\u044f\u0449\u0438\u0435 \u043f\u043e\u0434 \u0444\u0438\u043b\u044c\u0442\u0440").setValue(true).visible(this.enchantSort::isValue);
    private final SliderSettings minDurability = new SliderSettings("\u041c\u0438\u043d. \u043f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u044c %", "\u041c\u0438\u043d\u0438\u043c\u0430\u043b\u044c\u043d\u0430\u044f \u043f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u044c \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430 \u0432 \u043f\u0440\u043e\u0446\u0435\u043d\u0442\u0430\u0445").setValue(0.0f).range(0, 100).step(1.0f).visible(this.enchantSort::isValue);
    private final SliderSettings minProtection = new SliderSettings("\u041c\u0438\u043d. Protection", "\u041c\u0438\u043d\u0438\u043c\u0430\u043b\u044c\u043d\u044b\u0439 \u0443\u0440\u043e\u0432\u0435\u043d\u044c Protection").setValue(5.0f).range(1, 10).step(1.0f).visible(this.enchantSort::isValue);
    private final SliderSettings minSharpness = new SliderSettings("\u041c\u0438\u043d. Sharpness", "\u041c\u0438\u043d\u0438\u043c\u0430\u043b\u044c\u043d\u044b\u0439 \u0443\u0440\u043e\u0432\u0435\u043d\u044c Sharpness").setValue(7.0f).range(1, 10).step(1.0f).visible(this.enchantSort::isValue);
    private final SliderSettings minEfficiency = new SliderSettings("\u041c\u0438\u043d. Efficiency", "\u041c\u0438\u043d\u0438\u043c\u0430\u043b\u044c\u043d\u044b\u0439 \u0443\u0440\u043e\u0432\u0435\u043d\u044c Efficiency").setValue(7.0f).range(1, 10).step(1.0f).visible(this.enchantSort::isValue);
    private final SliderSettings maxEfficiency = new SliderSettings("\u041c\u0430\u043a\u0441. Efficiency", "\u041c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u044b\u0439 \u0443\u0440\u043e\u0432\u0435\u043d\u044c Efficiency").setValue(10.0f).range(1, 10).step(1.0f).visible(this.enchantSort::isValue);
    private final ColorSetting cheapestColor = new ColorSetting("\u0426\u0432\u0435\u0442 \u0434\u0435\u0448\u0435\u0432\u043e\u0433\u043e", "\u0426\u0432\u0435\u0442 \u043f\u043e\u0434\u0441\u0432\u0435\u0442\u043a\u0438 \u0441\u0430\u043c\u043e\u0433\u043e \u0434\u0435\u0448\u0435\u0432\u043e\u0433\u043e \u0441\u043b\u043e\u0442\u0430").setColor(new Color(75, 255, 75, 255).getRGB());
    private final ColorSetting bestValueColor = new ColorSetting("\u0426\u0432\u0435\u0442 \u0432\u044b\u0433\u043e\u0434\u043d\u043e\u0433\u043e", "\u0426\u0432\u0435\u0442 \u043f\u043e\u0434\u0441\u0432\u0435\u0442\u043a\u0438 \u043b\u0443\u0447\u0448\u0435\u0433\u043e \u0441\u043b\u043e\u0442\u0430 \u043f\u043e \u0446\u0435\u043d\u0435 \u0437\u0430 \u0448\u0442\u0443\u043a\u0443").setColor(new Color(51, 170, 255, 255).getRGB());
    private final BindSetting searchHeldItemBind = new BindSetting("\u041f\u043e\u0438\u0441\u043a \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430", "\u0418\u0449\u0435\u0442 \u043f\u0440\u0435\u0434\u043c\u0435\u0442 \u0438\u0437 \u0440\u0443\u043a\u0438 \u0447\u0435\u0440\u0435\u0437 /ah search");
    private final BindSetting autoSellBind = new BindSetting("\u041f\u0440\u043e\u0434\u0430\u0442\u044c \u043f\u043e \u043d\u0430\u0446\u0435\u043d\u043a\u0435", "\u0418\u0449\u0435\u0442 \u043c\u0438\u043d\u0438\u043c\u0430\u043b\u044c\u043d\u0443\u044e \u0446\u0435\u043d\u0443 \u0438 \u043f\u0440\u043e\u0434\u0430\u0435\u0442 \u043f\u0440\u0435\u0434\u043c\u0435\u0442 \u0438\u0437 \u0440\u0443\u043a\u0438");
    private final SliderSettings sellMarkupPercent = new SliderSettings("\u041d\u0430\u0446\u0435\u043d\u043a\u0430 %", "\u041d\u0430\u0446\u0435\u043d\u043a\u0430 \u043a \u0441\u0430\u043c\u043e\u0439 \u0434\u0435\u0448\u0435\u0432\u043e\u0439 \u0446\u0435\u043d\u0435").setValue(25.0f).range(0, 500).step(1.0f);
    private Slot cheapestSlot;
    private Slot bestValueSlot;
    private int cheapestTotalPrice = 0;
    private int cheapestCount = 1;
    private int lastUpdateTick = 0;
    private int lastSlotCount = 0;
    private GenericContainerScreen lastScreen = null;
    private Map<Integer, String> pricePerItemCache = new HashMap<Integer, String>();
    private boolean waitingForAutoSellResult = false;
    private int autoSellStartTick = 0;
    private long lastActionMs = 0L;

    public AuctionHelper() {
        super("AuctionHelper", "Auction Helper", ModuleCategory.RENDER);
        this.setup(this.onlyMending, this.showPricePerItem, this.enchantSort, this.sortPreset, this.onlyMatching, this.minDurability, this.minProtection, this.minSharpness, this.minEfficiency, this.maxEfficiency, this.cheapestColor, this.bestValueColor, this.searchHeldItemBind, this.autoSellBind, this.sellMarkupPercent);
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (AuctionHelper.mc.player == null || AuctionHelper.mc.player.networkHandler == null) {
            return;
        }
        if (e.isKeyDown(this.searchHeldItemBind.getKey())) {
            this.searchHeldItemByBind();
        }
        if (e.isKeyDown(this.autoSellBind.getKey())) {
            this.startAutoSellByBind();
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (AuctionHelper.mc.player == null) {
            return;
        }
        Screen screen2 = AuctionHelper.mc.currentScreen;
        if (!(screen2 instanceof GenericContainerScreen)) {
            this.cheapestSlot = null;
            this.bestValueSlot = null;
            this.cheapestTotalPrice = 0;
            this.lastScreen = null;
            this.pricePerItemCache.clear();
            return;
        }
        GenericContainerScreen screen = (GenericContainerScreen)screen2;
        int currentSlotCount = ((GenericContainerScreenHandler)screen.getScreenHandler()).slots.size();
        if (currentSlotCount != this.lastSlotCount || AuctionHelper.mc.player.age - this.lastUpdateTick > 5) {
            this.lastUpdateTick = AuctionHelper.mc.player.age;
            this.lastSlotCount = currentSlotCount;
            this.updateBestSlots(screen);
        }
        if (this.waitingForAutoSellResult) {
            this.handleAutoSellAfterSearch(screen);
        }
    }

    @EventHandler
    public void onHandledScreen(HandledScreenEvent e) {
        int color;
        Screen screen2 = AuctionHelper.mc.currentScreen;
        if (!(screen2 instanceof GenericContainerScreen)) {
            return;
        }
        GenericContainerScreen screen = (GenericContainerScreen)screen2;
        if (this.lastScreen != screen) {
            this.pricePerItemCache.clear();
            this.lastScreen = screen;
        }
        DrawContext context = e.getDrawContext();
        MatrixStack matrix = GuiCompat.stack(context);
        int offsetX = (screen.width - e.getBackgroundWidth()) / 2;
        int offsetY = (screen.height - e.getBackgroundHeight()) / 2;
        matrix.push();
        matrix.translate((float)offsetX, (float)offsetY, 0.0f);
        long time = System.currentTimeMillis();
        if (this.cheapestSlot != null && this.isValidSlot(this.cheapestSlot, screen)) {
            color = this.getBlinkingColor(this.cheapestColor.getColor(), time, 500);
            this.highlightSlot(context, this.cheapestSlot, color);
        }
        if (this.bestValueSlot != null && this.isValidSlot(this.bestValueSlot, screen) && this.bestValueSlot != this.cheapestSlot) {
            color = this.getBlinkingColor(this.bestValueColor.getColor(), time, 600);
            this.highlightSlot(context, this.bestValueSlot, color);
        }
        matrix.pop();
        if (this.showPricePerItem.isValue()) {
            this.addPricePerItemToSlots(screen);
        }
    }

    private void searchHeldItemByBind() {
        ItemStack held = AuctionHelper.mc.player.getMainHandStack();
        if (held.isEmpty()) {
            ChatMessage.brandmessage("\u00a7c[AuctionHelper] \u0412 \u0440\u0443\u043a\u0435 \u043d\u0435\u0442 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430 \u0434\u043b\u044f \u043f\u043e\u0438\u0441\u043a\u0430.");
            return;
        }
        String query = this.getCleanItemName(held);
        if (query.isBlank()) {
            ChatMessage.brandmessage("\u00a7c[AuctionHelper] \u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430.");
            return;
        }
        if (!this.canSendActionNow()) {
            return;
        }
        ChatCommandSender.sendCommand(AuctionHelper.mc.player, "/ah search " + query);
        this.lastActionMs = System.currentTimeMillis();
        ChatMessage.brandmessage("\u00a7a[AuctionHelper] \u041f\u043e\u0438\u0441\u043a: \u00a7f" + query);
    }

    private void startAutoSellByBind() {
        ItemStack held = AuctionHelper.mc.player.getMainHandStack();
        if (held.isEmpty()) {
            ChatMessage.brandmessage("\u00a7c[AuctionHelper] \u0414\u043b\u044f \u043f\u0440\u043e\u0434\u0430\u0436\u0438 \u0434\u0435\u0440\u0436\u0438\u0442\u0435 \u043f\u0440\u0435\u0434\u043c\u0435\u0442 \u0432 \u0440\u0443\u043a\u0435.");
            return;
        }
        String query = this.getCleanItemName(held);
        if (query.isBlank()) {
            ChatMessage.brandmessage("\u00a7c[AuctionHelper] \u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430.");
            return;
        }
        if (!this.canSendActionNow()) {
            return;
        }
        this.waitingForAutoSellResult = true;
        this.autoSellStartTick = AuctionHelper.mc.player.age;
        ChatCommandSender.sendCommand(AuctionHelper.mc.player, "/ah search " + query);
        this.lastActionMs = System.currentTimeMillis();
        ChatMessage.brandmessage("\u00a7e[AuctionHelper] \u0418\u0449\u0443 \u043c\u0438\u043d\u0438\u043c\u0430\u043b\u044c\u043d\u0443\u044e \u0446\u0435\u043d\u0443 \u0434\u043b\u044f: \u00a7f" + query);
    }

    private void handleAutoSellAfterSearch(GenericContainerScreen screen) {
        String lowerTitle;
        String title = Formatting.strip((String)screen.getTitle().getString());
        String string = lowerTitle = title == null ? "" : title.toLowerCase(Locale.ROOT);
        if (!lowerTitle.contains("\u0430\u0443\u043a\u0446\u0438\u043e\u043d") && !lowerTitle.contains("auction")) {
            if (AuctionHelper.mc.player.age - this.autoSellStartTick > 120) {
                this.waitingForAutoSellResult = false;
                ChatMessage.brandmessage("\u00a7c[AuctionHelper] \u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0442\u043a\u0440\u044b\u0442\u044c \u0430\u0443\u043a\u0446\u0438\u043e\u043d \u0441 \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u0430\u043c\u0438.");
            }
            return;
        }
        if (AuctionHelper.mc.player.age - this.autoSellStartTick < 6) {
            return;
        }
        if (this.cheapestTotalPrice <= 0) {
            if (AuctionHelper.mc.player.age - this.autoSellStartTick > 120) {
                this.waitingForAutoSellResult = false;
                ChatMessage.brandmessage("\u00a7c[AuctionHelper] \u041d\u0435 \u043d\u0430\u0448\u0435\u043b \u0446\u0435\u043d\u0443 \u0434\u043b\u044f \u0430\u0432\u0442\u043e\u043f\u0440\u043e\u0434\u0430\u0436\u0438.");
            }
            return;
        }
        double markup = (double)this.sellMarkupPercent.getValue() / 100.0;
        int sellPrice = (int)Math.max(1L, Math.round((double)this.cheapestTotalPrice * (1.0 + markup)));
        this.waitingForAutoSellResult = false;
        if (!this.canSendActionNow()) {
            return;
        }
        ChatCommandSender.sendCommand(AuctionHelper.mc.player, "/ah sell " + sellPrice);
        this.lastActionMs = System.currentTimeMillis();
        ChatMessage.brandmessage("\u00a7a[AuctionHelper] \u041f\u0440\u043e\u0434\u0430\u0436\u0430 \u0432\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0430 \u0437\u0430: \u00a7f" + sellPrice + "\u00a7a (\u0440\u044b\u043d\u043e\u043a: " + this.cheapestTotalPrice + ", \u043d\u0430\u0446\u0435\u043d\u043a\u0430: " + (int)this.sellMarkupPercent.getValue() + "%)");
    }

    private boolean canSendActionNow() {
        return System.currentTimeMillis() - this.lastActionMs >= 350L;
    }

    private String getCleanItemName(ItemStack stack) {
        String raw = Formatting.strip((String)stack.getName().getString());
        if (raw == null) {
            return "";
        }
        String clean = raw.replace("[\u2605]", "").replace("\u2605", "").trim();
        return clean.replaceAll("\\s{2,}", " ");
    }

    private boolean isValidSlot(Slot slot, GenericContainerScreen screen) {
        if (slot == null) {
            return false;
        }
        if (slot.id < 0 || slot.id >= ((GenericContainerScreenHandler)screen.getScreenHandler()).slots.size()) {
            return false;
        }
        Slot currentSlot = ((GenericContainerScreenHandler)screen.getScreenHandler()).getSlot(slot.id);
        return currentSlot.hasStack() && !currentSlot.getStack().isEmpty();
    }

    private void updateBestSlots(GenericContainerScreen screen) {
        DefaultedList slots = ((GenericContainerScreenHandler)screen.getScreenHandler()).slots;
        ArrayList<ItemPriceData> validItems = new ArrayList<ItemPriceData>();
        for (Slot slot : (Iterable<Slot>)slots) {
            int totalPrice;
            ItemStack stack;
            if (slot.inventory == AuctionHelper.mc.player.getInventory() || (stack = slot.getStack()).isEmpty() || this.isServiceItem(stack) || this.onlyMending.isValue() && !this.hasMending(stack) || !this.matchesAdvancedFilters(stack) || (totalPrice = this.parsePriceFromLore(stack)) <= 0) continue;
            int count = Math.max(1, stack.getCount());
            int pricePerItem = totalPrice / count;
            int durabilityPercent = this.getDurabilityPercent(stack);
            int enchantScore = this.calculateEnchantScore(stack);
            validItems.add(new ItemPriceData(slot, totalPrice, pricePerItem, count, durabilityPercent, enchantScore));
        }
        if (validItems.isEmpty()) {
            this.cheapestSlot = null;
            this.bestValueSlot = null;
            this.cheapestTotalPrice = 0;
            return;
        }
        ItemPriceData cheapest = validItems.stream().min(Comparator.comparingInt(d -> d.totalPrice)).orElse(null);
        this.cheapestSlot = cheapest == null ? null : cheapest.slot;
        this.cheapestTotalPrice = cheapest == null ? 0 : cheapest.totalPrice;
        this.bestValueSlot = validItems.stream().min(Comparator.comparingInt((ItemPriceData d) -> d.pricePerItem).thenComparing(d -> d.enchantmentScore, Comparator.reverseOrder()).thenComparing(d -> d.durabilityPercent, Comparator.reverseOrder())).map(d -> d.slot).orElse(null);
    }

    private boolean matchesAdvancedFilters(ItemStack stack) {
        if (!this.enchantSort.isValue()) {
            return true;
        }
        int durability = this.getDurabilityPercent(stack);
        if (durability < (int)this.minDurability.getValue()) {
            return false;
        }
        String preset = this.sortPreset.get();
        boolean armorMatch = AuctionHelper.isArmor(stack) && this.getEnchantmentLevel(stack, "protection") >= (int)this.minProtection.getValue();
        int efficiencyLevel = this.getEnchantmentLevel(stack, "efficiency");
        boolean pickaxeMatch = stack.isIn(ItemTags.PICKAXES) && efficiencyLevel >= (int)this.minEfficiency.getValue() && efficiencyLevel <= (int)Math.max(this.minEfficiency.getValue(), this.maxEfficiency.getValue());
        boolean swordMatch = stack.isIn(ItemTags.SWORDS) && this.getEnchantmentLevel(stack, "sharpness") >= (int)this.minSharpness.getValue();
        return switch (preset) {
            case "\u0411\u0440\u043e\u043d\u044f Prot 5+" -> armorMatch;
            case "\u041a\u0438\u0440\u043a\u0430 Eff 7-10" -> pickaxeMatch;
            case "\u041c\u0435\u0447 Sharp 7+" -> swordMatch;
            default -> !this.onlyMatching.isValue() || armorMatch || pickaxeMatch || swordMatch;
        };
    }

    /** Порт 1.21.11: класс ArmorItem удалён — броня определяется по тегам предметов. */
    private static boolean isArmor(ItemStack stack) {
        return stack.isIn(ItemTags.HEAD_ARMOR)   // HEAD_ARMOR
            || stack.isIn(ItemTags.CHEST_ARMOR)   // CHEST_ARMOR
            || stack.isIn(ItemTags.LEG_ARMOR)   // LEG_ARMOR
            || stack.isIn(ItemTags.FOOT_ARMOR);  // FOOT_ARMOR
    }

    private int getDurabilityPercent(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isDamageable()) {
            return 100;
        }
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return 100;
        }
        int left = max - stack.getDamage();
        return Math.max(0, Math.min(100, Math.round((float)left * 100.0f / (float)max)));
    }

    private int getEnchantmentLevel(ItemStack stack, String shortId) {
        ItemEnchantmentsComponent enchantments = (ItemEnchantmentsComponent)stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) {
            return 0;
        }
        for (RegistryEntry enchantment : enchantments.getEnchantments()) {
            String fullId = enchantment.getIdAsString();
            if (fullId == null || !fullId.endsWith(":" + shortId) && !fullId.equals(shortId)) continue;
            return enchantments.getLevel(enchantment);
        }
        return 0;
    }

    private int calculateEnchantScore(ItemStack stack) {
        int prot = this.getEnchantmentLevel(stack, "protection");
        int eff = this.getEnchantmentLevel(stack, "efficiency");
        int sharp = this.getEnchantmentLevel(stack, "sharpness");
        int score = 0;
        if (AuctionHelper.isArmor(stack)) {
            score += prot * 10;
        }
        if (stack.isIn(ItemTags.PICKAXES)) {
            score += eff * 10;
        }
        if (stack.isIn(ItemTags.SWORDS)) {
            score += sharp * 10;
        }
        if (this.hasMending(stack)) {
            score += 7;
        }
        return score;
    }

    private boolean hasMending(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ItemEnchantmentsComponent enchantments = stack.getEnchantments();
        for (RegistryEntry entry : enchantments.getEnchantments()) {
            String enchantmentId;
            if (!entry.getKey().isPresent() || !"minecraft:mending".equals(enchantmentId = ((RegistryKey)entry.getKey().get()).getValue().toString())) continue;
            return true;
        }
        return false;
    }

    private boolean isServiceItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        String itemName = stack.getItem().toString().toLowerCase(Locale.ROOT);
        return itemName.contains("glass_pane") || itemName.contains("stained_glass") || itemName.contains("barrier") || itemName.contains("air");
    }

    private int parsePriceFromLore(ItemStack stack) {
        int parsed;
        String displayName;
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        LoreComponent loreComp = (LoreComponent)stack.get(DataComponentTypes.LORE);
        if (loreComp != null) {
            for (Text text : loreComp.lines()) {
                int parsed2;
                String line = Formatting.strip((String)text.getString());
                if (line == null || (parsed2 = this.parsePriceFromLine(line)) <= 0) continue;
                return parsed2;
            }
        }
        if ((displayName = Formatting.strip((String)stack.getName().getString())) != null && !displayName.isBlank() && (parsed = this.parsePriceFromLine(displayName)) > 0) {
            return parsed;
        }
        return 0;
    }

    private int parsePriceFromLine(String line) {
        String candidate;
        int parsed;
        int parsed2;
        if (line == null || line.isBlank()) {
            return 0;
        }
        Matcher m = PRICE_WITH_LABEL.matcher(line);
        if (m.find() && (parsed2 = this.parseNumberSafely(m.group(1))) > 0) {
            return parsed2;
        }
        Matcher m2 = PRICE_WITH_CURRENCY.matcher(line);
        if (m2.find() && (parsed = this.parseNumberSafely(candidate = m2.group(1) != null ? m2.group(1) : m2.group(2))) > 0) {
            return parsed;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.contains("\u0446\u0435\u043d\u0430") || lower.contains("\u0441\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c") || lower.contains("price") || lower.contains("cost") || lower.contains("$") || lower.contains("\u20bd") || lower.contains("\u0440\u0443\u0431") || lower.contains(" \u0440")) {
            Matcher digits = DIGITS_PATTERN.matcher(line);
            while (digits.find()) {
                int parsed3 = this.parseNumberSafely(digits.group(1));
                if (parsed3 <= 0) continue;
                return parsed3;
            }
        }
        return 0;
    }

    private int parseNumberSafely(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        }
        catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void addPricePerItemToSlots(GenericContainerScreen screen) {
        for (Slot slot : ((GenericContainerScreenHandler)screen.getScreenHandler()).slots) {
            int totalPrice;
            ItemStack stack;
            if (slot.inventory == AuctionHelper.mc.player.getInventory() || (stack = slot.getStack()).isEmpty() || stack.getCount() <= 1 || this.pricePerItemCache.containsKey(slot.id) || (totalPrice = this.parsePriceFromLore(stack)) <= 0) continue;
            int count = stack.getCount();
            int pricePerItem = totalPrice / count;
            this.addPricePerItemToLore(stack, pricePerItem);
            this.pricePerItemCache.put(slot.id, this.formatPrice(pricePerItem));
        }
    }

    private void addPricePerItemToLore(ItemStack stack, int pricePerItem) {
        String pricePerItemText = "\u0426\u0435\u043d\u0430 \u0437\u0430 1 \u0448\u0442: " + this.formatPrice(pricePerItem);
        LoreComponent existingLore = (LoreComponent)stack.get(DataComponentTypes.LORE);
        ArrayList<Text> loreLines = new ArrayList<Text>();
        if (existingLore != null) {
            for (Text line : existingLore.lines()) {
                String lineStr = line.getString().toLowerCase(Locale.ROOT);
                if (lineStr.contains("\u0446\u0435\u043d\u0430 \u0437\u0430 1 \u0448\u0442")) {
                    return;
                }
                loreLines.add(line);
            }
            boolean inserted = false;
            ArrayList<Text> newLoreLines = new ArrayList<Text>();
            for (Text text2 : loreLines) {
                newLoreLines.add(text2);
                String lineStr = text2.getString().toLowerCase(Locale.ROOT);
                if (inserted || !lineStr.contains("\u0446\u0435\u043d\u0430") && !lineStr.contains("\u0441\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c") && !lineStr.contains("price") && !lineStr.contains("cost")) continue;
                newLoreLines.add(Text.literal((String)pricePerItemText));
                inserted = true;
            }
            if (!inserted) {
                newLoreLines.add(Text.literal((String)pricePerItemText));
            }
            stack.set(DataComponentTypes.LORE, new LoreComponent(newLoreLines));
        } else {
            loreLines.add(Text.literal((String)pricePerItemText));
            stack.set(DataComponentTypes.LORE, new LoreComponent(loreLines));
        }
    }

    private int getBlinkingColor(int color, long time, int periodMs) {
        float alpha = (float)(Math.sin((double)time / (double)periodMs * Math.PI) * (double)0.3f + (double)0.7f);
        return ColorAssist.multAlpha(color, Math.min(1.0f, Math.max(0.4f, alpha)));
    }

    private void highlightSlot(DrawContext context, Slot slot, int color) {
        if (slot == null) {
            return;
        }
        Render2D.rectangle.render(ShapeProperties.create(GuiCompat.stack(context), slot.x, slot.y, 16.0, 16.0).color(color).build());
    }

    private String formatPrice(int price) {
        if (price >= 1000000) {
            return String.format("%.2fM", (double)price / 1000000.0).replace(",", ".");
        }
        if (price >= 1000) {
            return String.format("%.1fK", (double)price / 1000.0).replace(",", ".");
        }
        return String.valueOf(price);
    }

    private static final class ItemPriceData {
        private final Slot slot;
        private final int totalPrice;
        private final int pricePerItem;
        private final int count;
        private final int durabilityPercent;
        private final int enchantmentScore;

        public ItemPriceData(Slot slot, int totalPrice, int pricePerItem, int count, int durabilityPercent, int enchantmentScore) {
            this.slot = slot;
            this.totalPrice = totalPrice;
            this.pricePerItem = pricePerItem;
            this.count = count;
            this.durabilityPercent = durabilityPercent;
            this.enchantmentScore = enchantmentScore;
        }

        public Slot getSlot() {
            return this.slot;
        }

        public int getTotalPrice() {
            return this.totalPrice;
        }

        public int getPricePerItem() {
            return this.pricePerItem;
        }

        public int getCount() {
            return this.count;
        }

        public int getDurabilityPercent() {
            return this.durabilityPercent;
        }

        public int getEnchantmentScore() {
            return this.enchantmentScore;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof ItemPriceData)) {
                return false;
            }
            ItemPriceData other = (ItemPriceData)o;
            if (this.getTotalPrice() != other.getTotalPrice()) {
                return false;
            }
            if (this.getPricePerItem() != other.getPricePerItem()) {
                return false;
            }
            if (this.getCount() != other.getCount()) {
                return false;
            }
            if (this.getDurabilityPercent() != other.getDurabilityPercent()) {
                return false;
            }
            if (this.getEnchantmentScore() != other.getEnchantmentScore()) {
                return false;
            }
            Slot this$slot = this.getSlot();
            Slot other$slot = other.getSlot();
            return !(this$slot == null ? other$slot != null : !this$slot.equals(other$slot));
        }

        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            result = result * 59 + this.getTotalPrice();
            result = result * 59 + this.getPricePerItem();
            result = result * 59 + this.getCount();
            result = result * 59 + this.getDurabilityPercent();
            result = result * 59 + this.getEnchantmentScore();
            Slot $slot = this.getSlot();
            result = result * 59 + ($slot == null ? 43 : $slot.hashCode());
            return result;
        }

        public String toString() {
            return "AuctionHelper.ItemPriceData(slot=" + String.valueOf(this.getSlot()) + ", totalPrice=" + this.getTotalPrice() + ", pricePerItem=" + this.getPricePerItem() + ", count=" + this.getCount() + ", durabilityPercent=" + this.getDurabilityPercent() + ", enchantmentScore=" + this.getEnchantmentScore() + ")";
        }
    }
}
