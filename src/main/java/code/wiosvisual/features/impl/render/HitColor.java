package code.wiosvisual.features.impl.render;

import code.wiosvisual.events.player.AttackEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.color.ColorAssist;
import java.awt.Color;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Подсвечивает игрока выбранным цветом (по умолчанию фиолетовым), когда мы по нему
 * ударили и урон реально прошёл (сервер прислал анимацию урона).
 * Сама покраска модели живёт в LivingEntityRendererMixin#renderModelHook.
 */
public class HitColor
extends Module {
    private final ColorSetting color = new ColorSetting("Цвет", "Цвет подсветки при ударе").value(new Color(170, 0, 255).getRGB());
    private final SliderSettings duration = new SliderSettings("Длительность", "Сколько мс после удара считать урон нашим").setValue(800.0f).range(100, 3000).step(50.0f);
    private final SliderSettings strength = new SliderSettings("Сила", "Насколько сильно перекрашивать модель").setValue(1.0f).range(0.2f, 1.0f).step(0.05f);
    private final BooleanSetting onlyMyHits = new BooleanSetting("Только мои удары", "Красить только тех, кого бил я").setValue(true);
    private final BooleanSetting players = new BooleanSetting("Игроки", "Красить игроков").setValue(true);
    private final BooleanSetting mobs = new BooleanSetting("Мобы", "Красить остальных живых существ").setValue(true);
    private final BooleanSetting tintArmor = new BooleanSetting("Красить броню", "Подсвечивать и надетую броню").setValue(true);
    private final BooleanSetting removeVanillaFlash = new BooleanSetting("Без красной вспышки", "Убрать ванильную красную заливку").setValue(true);
    private final Map<Integer, Long> hits = new ConcurrentHashMap<Integer, Long>();
    /** Существо, которое EntityRenderDispatcher рисует прямо сейчас (нужно для не-игроков). */
    private static volatile Entity renderedEntity;
    /** Ставится на время рендера брони конкретного игрока: 0 — красить не нужно. */
    private static volatile int armorTint;
    private static volatile int armorOverlay;

    public HitColor() {
        super("HitColor", "Hit Color", ModuleCategory.RENDER);
        this.setup(this.color, this.duration, this.strength, this.onlyMyHits, this.players, this.mobs, this.tintArmor, this.removeVanillaFlash);
    }

    public static HitColor getInstance() {
        return Instance.get(HitColor.class);
    }

    @Override
    public void deactivate() {
        this.hits.clear();
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        // Удар отменён (например, NoFriendDamage) — реагировать не на что.
        if (e.isCancelled()) {
            return;
        }
        LivingEntity living;
        Entity entity = e.getEntity();
        if (!(entity instanceof LivingEntity) || (living = (LivingEntity)entity) == HitColor.mc.player || !living.isAlive()) {
            return;
        }
        this.hits.put(living.getId(), System.currentTimeMillis());
        this.cleanup();
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        long window = (long)this.duration.getValue();
        Iterator<Map.Entry<Integer, Long>> iterator = this.hits.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue() <= window) continue;
            iterator.remove();
        }
    }

    /** Существо, которое рисуется прямо сейчас — выставляется из EntityRenderDispatcherMixin. */
    public static void setRenderedEntity(Entity entity) {
        renderedEntity = entity;
    }

    public static Entity getRenderedEntity() {
        return renderedEntity;
    }

    /**
     * @param hurt значение LivingEntityRenderState#hurt — урон по существу реально прошёл.
     */
    public boolean shouldTint(LivingEntity living, boolean hurt) {
        if (!this.isState() || living == null || living == HitColor.mc.player) {
            return false;
        }
        boolean isPlayer = living instanceof PlayerEntity;
        if (isPlayer ? !this.players.isValue() : !this.mobs.isValue()) {
            return false;
        }
        if (!this.onlyMyHits.isValue()) {
            // Без фильтра «только мои удары» опираемся на ванильный флаг урона.
            return hurt;
        }
        Long time = this.hits.get(living.getId());
        if (time == null) {
            return false;
        }
        if (System.currentTimeMillis() - time > (long)this.duration.getValue()) {
            this.hits.remove(living.getId());
            return false;
        }
        return true;
    }

    /**
     * Смешивает ванильный цвет модели с выбранным, чтобы «Сила» работала как плавность перекраски.
     */
    public int getTintColor(int original) {
        return this.mix(original, this.color.getColor(), this.strength.getValue());
    }

    private int mix(int original, int target, float amount) {
        int r = (int)((float)ColorAssist.red(original) + ((float)ColorAssist.red(target) - (float)ColorAssist.red(original)) * amount);
        int g = (int)((float)ColorAssist.green(original) + ((float)ColorAssist.green(target) - (float)ColorAssist.green(original)) * amount);
        int b = (int)((float)ColorAssist.blue(original) + ((float)ColorAssist.blue(target) - (float)ColorAssist.blue(original)) * amount);
        int a = ColorAssist.alpha(original);
        if (a == 0) {
            a = 255;
        }
        return ColorAssist.getColor(r, g, b, a);
    }

    /**
     * Вызывается из ArmorFeatureRendererMixin перед отрисовкой брони игрока:
     * запоминает цвет, которым EquipmentRendererMixin покрасит слои брони.
     */
    public static void beginArmorTint(LivingEntity living, boolean hurt) {
        armorTint = 0;
        HitColor module = HitColor.getInstance();
        if (module == null || !module.tintArmor.isValue() || !module.shouldTint(living, hurt)) {
            return;
        }
        armorTint = module.color.getColor();
        armorOverlay = module.removeVanillaFlash.isValue() ? OverlayTexture.DEFAULT_UV : 0;
    }

    public static void endArmorTint() {
        armorTint = 0;
    }

    public static boolean isArmorTintActive() {
        return armorTint != 0;
    }

    /** Цвет слоя брони: та же смесь «сила», что и у модели игрока. */
    public static int tintArmorColor(int original) {
        HitColor module = HitColor.getInstance();
        if (module == null || armorTint == 0) {
            return original;
        }
        return module.mix(original, armorTint, module.strength.getValue());
    }

    public static int tintArmorOverlay(int original) {
        return armorTint != 0 && armorOverlay != 0 ? armorOverlay : original;
    }

    public int getOverlay(int original) {
        return this.removeVanillaFlash.isValue() ? OverlayTexture.DEFAULT_UV : original;
    }
}
