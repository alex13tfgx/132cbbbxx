package code.wiosvisual.features.impl.misc;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.events.keyboard.KeyEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BindSetting;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.bind.BindCodes;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.interactions.inv.InventorySearch;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * Быстрый выбор предмета в хотбаре по кнопке: каждой способности своя клавиша,
 * по нажатию выбирается первый подходящий слот хотбара. Над такими предметами
 * в хотбаре можно подписать назначенную кнопку.
 */
public class FastSwap
extends Module {
    private final BindSetting chorus = new BindSetting("Кнопка хоруса", "Плод хоруса");
    private final BindSetting pearl = new BindSetting("Кнопка эндерпёрла", "Жемчуг Края");
    private final BindSetting healing = new BindSetting("Кнопка зелья исцеления", "Зелье мгновенного лечения");
    private final BindSetting trap = new BindSetting("Кнопка трапки", "Незеритовый лом");
    private final BindSetting plast = new BindSetting("Кнопка пласта", "Сушёные водоросли");
    private final BindSetting disorient = new BindSetting("Кнопка дезориентации", "Око Края");
    private final BindSetting dust = new BindSetting("Кнопка явной пыли", "Сахар");
    private final BindSetting lightning = new BindSetting("Кнопка шаровой молнии", "Звезда Незера");
    private final BindSetting slime = new BindSetting("Кнопка кома слизи", "Слизь");
    private final BindSetting turtle = new BindSetting("Кнопка черепашьего захвата", "Щиток черепахи");
    private final BindSetting spider = new BindSetting("Кнопка паучьей судьбы", "Паутина");
    private final BindSetting stun = new BindSetting("Кнопка стана", "Око Края");
    private final BindSetting magnet = new BindSetting("Кнопка магнитного шара", "Звезда фейерверка");
    private final BindSetting boomTrap = new BindSetting("Кнопка взрывной трапки", "Осколок призмарина");
    private final BindSetting bomb = new BindSetting("Кнопка взрывной штучки", "Огненный шар");
    private final BindSetting starStun = new BindSetting("Кнопка стана звезды", "Звезда Незера");
    private final BindSetting snow = new BindSetting("Кнопка кома снега", "Снежок");
    private final BooleanSetting showBinds = new BooleanSetting("Отображать бинды", "Подписывать кнопку над предметом в хотбаре").setValue(true);
    private final SliderSettings bindScale = new SliderSettings("Размер шрифта биндов", "Размер подписи кнопки в хотбаре").setValue(1.0f).range(0.5f, 2.0f).step(0.1f).visible(this.showBinds::isValue);

    /** Порядок важен: при нескольких биндах на одну клавишу срабатывает первое правило. */
    private final List<Rule> rules = List.of(
            new Rule(this.trap, FastSwap.item(Items.NETHERITE_SCRAP)),
            new Rule(this.plast, FastSwap.item(Items.DRIED_KELP)),
            new Rule(this.disorient, FastSwap.item(Items.ENDER_EYE)),
            new Rule(this.dust, FastSwap.item(Items.SUGAR)),
            new Rule(this.lightning, FastSwap.item(Items.NETHER_STAR)),
            new Rule(this.slime, FastSwap.item(Items.SLIME_BALL)),
            new Rule(this.turtle, FastSwap.item(Items.TURTLE_SCUTE)),
            new Rule(this.spider, FastSwap.item(Items.COBWEB)),
            new Rule(this.stun, FastSwap.item(Items.ENDER_EYE)),
            new Rule(this.magnet, FastSwap.item(Items.FIREWORK_STAR)),
            new Rule(this.chorus, FastSwap.item(Items.CHORUS_FRUIT)),
            new Rule(this.pearl, FastSwap.item(Items.ENDER_PEARL)),
            new Rule(this.healing, FastSwap::isHealingPotion),
            new Rule(this.boomTrap, FastSwap.item(Items.PRISMARINE_SHARD)),
            new Rule(this.bomb, FastSwap.item(Items.FIRE_CHARGE)),
            new Rule(this.starStun, FastSwap.item(Items.NETHER_STAR)),
            new Rule(this.snow, FastSwap.item(Items.SNOWBALL)));

    private record Rule(BindSetting bind, Predicate<ItemStack> matches) {
        boolean bound() {
            int key = this.bind.getKey();
            return key != -1 && key != 0;
        }
    }

    public static FastSwap getInstance() {
        return Instance.get(FastSwap.class);
    }

    public FastSwap() {
        super("FastSwap", "Fast Swap", ModuleCategory.MISC);
        this.setup(this.chorus, this.pearl, this.healing, this.trap, this.plast, this.disorient, this.dust, this.lightning, this.slime, this.turtle, this.spider, this.stun, this.magnet, this.boomTrap, this.bomb, this.starStun, this.snow, this.showBinds, this.bindScale);
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (mc.player == null) {
            return;
        }
        for (Rule rule : this.rules) {
            if (!rule.bound() || !e.isKeyDown(rule.bind().getKey())) {
                continue;
            }
            Optional<Integer> slot = InventorySearch.find(rule.matches(), false, true);
            if (slot.isPresent()) {
                mc.player.getInventory().selectedSlot = slot.get();
                return;
            }
        }
    }

    /** Те же подписи в инвентаре и любых контейнерах: над каждым предметом с назначенной кнопкой. */
    @EventHandler
    public void onHandledScreen(code.wiosvisual.events.container.HandledScreenEvent e) {
        if (!this.showBinds.isValue() || mc.player == null || !(mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen)) {
            return;
        }
        net.minecraft.client.gui.screen.ingame.HandledScreen screen = (net.minecraft.client.gui.screen.ingame.HandledScreen)mc.currentScreen;
        net.minecraft.screen.ScreenHandler handler = mc.player.currentScreenHandler;
        if (handler == null) {
            return;
        }
        DrawContext context = e.getDrawContext();
        MatrixStack matrices = GuiCompat.stack(context);
        int offsetX = (screen.width - e.getBackgroundWidth()) / 2;
        int offsetY = (screen.height - e.getBackgroundHeight()) / 2;
        matrices.push();
        matrices.translate((float)offsetX, (float)offsetY, 0.0f);
        for (net.minecraft.screen.slot.Slot slot : handler.slots) {
            FastSwap.drawBindLabel(context, slot.getStack(), (float)slot.x, (float)slot.y);
        }
        matrices.pop();
    }

    /** Кнопка, назначенная на предмет, или -1 — для подписи над хотбаром. */
    public int bindFor(ItemStack stack) {
        for (Rule rule : this.rules) {
            if (rule.bound() && rule.matches().test(stack)) {
                return rule.bind().getKey();
            }
        }
        return -1;
    }

    /**
     * Подпись назначенной кнопки в углу предмета хотбара. Координаты — левый верхний
     * угол иконки предмета. Вызывается и из ванильного хотбара, и из кастомного.
     */
    public static void drawBindLabel(DrawContext context, ItemStack stack, float itemX, float itemY) {
        FastSwap fastSwap = FastSwap.getInstance();
        if (fastSwap == null || !fastSwap.isState() || !fastSwap.showBinds.isValue() || stack.isEmpty()) {
            return;
        }
        int key = fastSwap.bindFor(stack);
        if (key == -1) {
            return;
        }
        String label = BindCodes.getDisplayName(key);
        int width = Math.max(1, mc.textRenderer.getWidth(label));
        // Длинные названия кнопок ужимаем до ширины слота, затем применяем размер из настроек.
        float scale = Math.min(1.0f, 14.0f / (float)width) * fastSwap.bindScale.getValue();
        MatrixStack matrices = GuiCompat.stack(context);
        matrices.push();
        // Предметы в GUI рисуются с глубиной ~150, без сдвига подпись уйдёт под иконку.
        matrices.translate(0.0f, 0.0f, 200.0f);
        matrices.scale(scale, scale, 1.0f);
        int tx = (int)((itemX + 1.0f) / scale);
        int ty = (int)((itemY + 1.0f) / scale);
        context.drawText(mc.textRenderer, label, tx, ty, 0xFFFFFFFF, true);
        matrices.pop();
    }

    private static Predicate<ItemStack> item(Item item) {
        return stack -> stack.getItem() == item;
    }

    private static boolean isHealingPotion(ItemStack stack) {
        if (stack.getItem() != Items.POTION) {
            return false;
        }
        PotionContentsComponent contents = (PotionContentsComponent)stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
        Optional<RegistryEntry<Potion>> potion = contents.potion();
        if (potion.isEmpty()) {
            return false;
        }
        for (StatusEffectInstance effect : potion.get().value().getEffects()) {
            if (effect.getEffectType() == StatusEffects.INSTANT_HEALTH) {
                return true;
            }
        }
        return false;
    }
}
