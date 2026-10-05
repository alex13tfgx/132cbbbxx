package ru.mentions.module.impl.display;

import code.wiosvisual.events.keyboard.KeyEvent;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.events.render.DrawEvent;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.InputUtil;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import ru.mentions.Client;
import ru.mentions.module.api.Category;
import ru.mentions.module.api.Module;
import ru.mentions.module.api.ModuleInfo;
import ru.mentions.module.api.settings.impl.BooleanSetting;
import ru.mentions.module.api.settings.impl.DragSetting;
import ru.mentions.module.api.settings.impl.ModeSetting;
import ru.mentions.module.api.settings.impl.MultiBooleanSetting;
import ru.mentions.module.api.settings.impl.SliderSetting;
import ru.mentions.module.impl.display.interfaceimpl.KeyBinds;
import ru.mentions.module.impl.display.interfaceimpl.MusicHud;
import ru.mentions.module.impl.display.interfaceimpl.Notify;
import ru.mentions.module.impl.display.interfaceimpl.ArmorHud;
import ru.mentions.module.impl.display.interfaceimpl.InventoryHud;
import ru.mentions.module.impl.display.interfaceimpl.Potions;
import ru.mentions.module.impl.display.interfaceimpl.TargetHud;
import ru.mentions.module.impl.display.interfaceimpl.WaterMark;
import ru.mentions.utils.other.Instance;
import ru.mentions.utils.render.Render2D;
import ru.mentions.utils.render.ScreenBlur;
import ru.mentions.utils.render.font.FontInitializer;

@ModuleInfo(
        name = "Inter Face",
        desc = "Настройка интерфейса (HUD) клиента",
        category = Category.RENDER
)
public class InterFace extends Module {

    public static InterFace getInstance() {
        return Instance.get(InterFace.class);
    }

    public MultiBooleanSetting element = new MultiBooleanSetting(this, "Элементы",
            new BooleanSetting("Water mark", true),
            new BooleanSetting("Key Binds", true),
            new BooleanSetting("Potions", true),
            new BooleanSetting("Music Player", true),
            new BooleanSetting("Notifications", true),
            new BooleanSetting("Target Hud", true),
            new BooleanSetting("Броня", true),
            new BooleanSetting("Inventory", true));

    public SliderSetting volume = new SliderSetting(this, "Громкость уведомления", 0.5F, 0.1F, 1.0F, 0.1F);
    public ModeSetting typeNotify = new ModeSetting(this, "Тип уведомления",
            "Первый", "Второй", "Третий");

    public SliderSetting sizeHud = new SliderSetting(this, "Размер интерфейса", 1.0F, 0.5F, 1.5F, 0.05F);
    public SliderSetting alphaHUD = new SliderSetting(this, "Прозрачность худа", 1.0F, 0.0F, 1.0F, 0.05F);

    public BooleanSetting notifyEffects = new BooleanSetting(this, "эффектах", true);
    public BooleanSetting notifyModules = new BooleanSetting(this, "модулях", true);
    public BooleanSetting notifyArmor = new BooleanSetting(this, "броне", true);

    public DragSetting waterMark = new DragSetting(this, "WaterMark", new Vector2f(10, 10));
    public DragSetting keyBind = new DragSetting(this, "Key Binds", new Vector2f(10, 50));
    public DragSetting potion = new DragSetting(this, "Potions", new Vector2f(90, 50));
    public DragSetting music = new DragSetting(this, "Music Player", new Vector2f(10, 400));
    public DragSetting notifications = new DragSetting(this, "Notifications", new Vector2f(0, 200));
    public DragSetting targetHudDrag = new DragSetting(this, "Target Hud", new Vector2f(90, 40));
    public DragSetting inventoryDrag = new DragSetting(this, "Inventory", new Vector2f(10, 260));

    private final WaterMark waterMarkElemnt = new WaterMark();
    private final KeyBinds keyBinds = new KeyBinds();
    private final Potions potions = new Potions();
    private final MusicHud musicHud = new MusicHud();
    private final Notify notifyHud = new Notify();
    private final TargetHud targetHud = new TargetHud();
    private final ArmorHud armorHud = new ArmorHud();
    private final InventoryHud inventoryHud = new InventoryHud();

    /** Цель из Target Hud (или null) — для Hud «Броня». */
    public net.minecraft.entity.LivingEntity armorTarget() {
        return this.targetHud.currentTarget();
    }

    private final DragSetting[] drags;
    private DragSetting dragged;
    private final Vector2f dragOffset = new Vector2f();

    public InterFace() {
        this.notifications.lockX = true;
        this.drags = new DragSetting[]{this.waterMark, this.keyBind, this.potion, this.music, this.notifications, this.targetHudDrag, this.inventoryDrag};
        FontInitializer.register();
        this.loadPositions();
    }

    private static final java.nio.file.Path POSITIONS_FILE = java.nio.file.Path.of("config", "wios_interface_positions.properties");

    private void loadPositions() {
        try {
            if (!java.nio.file.Files.exists(POSITIONS_FILE)) {
                return;
            }
            java.util.Properties props = new java.util.Properties();
            try (java.io.InputStream in = java.nio.file.Files.newInputStream(POSITIONS_FILE)) {
                props.load(in);
            }
            for (DragSetting drag : this.drags) {
                String x = props.getProperty(drag.getName() + ".x");
                String y = props.getProperty(drag.getName() + ".y");
                if (x != null && y != null) {
                    float px = Float.parseFloat(x);
                    float py = Float.parseFloat(y);
                    drag.targetPosition.set(px, py);
                    drag.position.set(px, py);
                    if (drag == this.waterMark) {
                        WaterMark.markPlaced();
                    }
                }
            }
        }
        catch (Exception ignored) {
            // Повреждённый файл позиций просто игнорируется: элементы встанут на места по умолчанию.
        }
    }

    private void savePositions() {
        try {
            java.util.Properties props = new java.util.Properties();
            for (DragSetting drag : this.drags) {
                props.setProperty(drag.getName() + ".x", String.valueOf(drag.targetPosition.x));
                props.setProperty(drag.getName() + ".y", String.valueOf(drag.targetPosition.y));
            }
            java.nio.file.Files.createDirectories(POSITIONS_FILE.getParent());
            try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(POSITIONS_FILE)) {
                props.store(out, "Wios interface element positions");
            }
        }
        catch (Exception ignored) {
            // Позиции не сохранятся до следующей попытки.
        }
    }

    @Override
    protected void onDisable() {
        this.musicHud.shutdown();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (this.element.getValue("Music Player")) {
            this.musicHud.onTick();
        }
        if (this.element.getValue("Notifications")) {
            this.notifyHud.onTick(this.notifyModules, this.notifyArmor, this.notifyEffects);
        }
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (mc.player == null || event.type() != InputUtil.Type.MOUSE || event.action() != 1 || event.key() != 0) {
            return;
        }
        if (this.element.getValue("Music Player")) {
            this.musicHud.onMouseClick(mc.mouse.getX() / 2.0, mc.mouse.getY() / 2.0);
        }
    }

    @EventHandler
    public void onDraw(DrawEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        DrawContext context = event.getDrawContext();
        ScreenBlur.frame();
        context.createNewRootLayer();
        Render2D.beginOverlay();
        context.getMatrices().pushMatrix();
        try {
            this.updateDrag();
            this.renderElements(event);
            Client.get().render2D().flushAll();
        }
        finally {
            context.getMatrices().popMatrix();
            Render2D.endOverlay();
        }
    }

    private void renderElements(DrawEvent event) {
        if (this.element.getValue("Potions")) {
            this.potions.onRender(this.potion, this, event);
        }
        if (this.element.getValue("Key Binds")) {
            this.keyBinds.onRender(this.keyBind, this);
        }
        if (this.element.getValue("Water mark")) {
            this.waterMarkElemnt.onRender(this.waterMark, this);
        }
        if (this.element.getValue("Music Player")) {
            this.musicHud.onRender(this.music, this);
        }
        if (this.element.getValue("Notifications")) {
            this.notifyHud.onRender(this.notifications, this, event);
        }
        if (this.element.getValue("Target Hud")) {
            this.targetHud.onRender(this.targetHudDrag, this, event);
        }
        if (this.element.getValue("Броня")) {
            this.armorHud.onRender(event);
        }
        if (this.element.getValue("Inventory")) {
            this.inventoryHud.onRender(this.inventoryDrag, this, event);
        }
    }

    /** Перетаскивание элементов мышью, пока открыт чат. Координаты в виртуальном масштабе x2, как у mentions. */
    private void updateDrag() {
        for (DragSetting drag : this.drags) {
            drag.position.x += (drag.targetPosition.x - drag.position.x) * 0.35F;
            drag.position.y += (drag.targetPosition.y - drag.position.y) * 0.35F;
        }
        if (!(mc.currentScreen instanceof ChatScreen)) {
            if (this.dragged != null) {
                this.dragged = null;
                this.savePositions();
            }
            return;
        }
        long window = mc.getWindow().getHandle();
        boolean down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        float mx = (float)(mc.mouse.getX() / 2.0);
        float my = (float)(mc.mouse.getY() / 2.0);
        if (!down) {
            if (this.dragged != null) {
                this.dragged = null;
                this.savePositions();
            }
            return;
        }
        if (this.dragged == null) {
            for (DragSetting drag : this.drags) {
                if (mx >= drag.position.x && mx <= drag.position.x + drag.size.x && my >= drag.position.y && my <= drag.position.y + drag.size.y) {
                    this.dragged = drag;
                    this.dragOffset.set(drag.targetPosition.x - mx, drag.targetPosition.y - my);
                    break;
                }
            }
        }
        if (this.dragged != null) {
            if (!this.dragged.lockX) {
                this.dragged.targetPosition.x = mx + this.dragOffset.x;
            }
            this.dragged.targetPosition.y = my + this.dragOffset.y;
        }
    }
}
