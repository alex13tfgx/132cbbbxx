/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_1291
 *  net.minecraft.class_1293
 *  net.minecraft.class_1304
 *  net.minecraft.class_1799
 *  net.minecraft.class_332
 *  net.minecraft.class_345
 *  net.minecraft.class_3532
 *  net.minecraft.class_4587
 *  net.minecraft.class_640
 */
package code.wiosvisual.display.hud;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.utils.client.discord.DiscordAvatar;
import code.wiosvisual.utils.client.managers.api.draggable.AbstractDraggable;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.BackgroundIcons;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.theme.ThemeManager;
import java.awt.Color;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import code.wiosvisual.utils.client.packet.network.Network;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import com.mojang.blaze3d.systems.RenderSystem;

public class DynamicIsland
extends AbstractDraggable {
    private static final String SEPARATOR = " / ";
    private static final long SHOW_MS = 2200L;
    private static final float PILL_H = 15.0f;
    private static final float ORB_R = 3.2f;
    private static final float ICON_TEXT_GAP = 5.0f;
    private static final float TIMER_PREFIX_GAP = 3.0f;
    private static volatile Notif active = null;
    private static volatile long activeEnd = 0L;
    private static volatile long activeRevision = 0L;
    private static final Queue<Notif> queue = new ArrayDeque<Notif>();
    private final FontRenderer font = Fonts.getSize(16, Fonts.Type.DEFAULT);
    private float pillW = 80.0f;
    private float notifAnim = 0.0f;
    private long lastAnimTime = System.currentTimeMillis();
    private Notif lastNotif = null;
    private long lastSeenRevision = -1L;
    private final Set<String> warnedExpiringEffects = new HashSet<String>();
    private long nextArmorCheckAt = 0L;
    private String cachedEventStatus = null;
    private static Method cachedEventStatusMethod;

    public DynamicIsland() {
        // canDrag = true: водяной знак можно двигать в редакторе HUD.
        super("Watermark", 10, 10, 80, 18, true);
    }

    public static void addNotification(String text, NotificationType type) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String clean = Formatting.strip((String)text) == null ? text : Formatting.strip((String)text);
        boolean isDynamic = text.contains("\u0418\u0432\u0435\u043d\u0442 \u0447\u0435\u0440\u0435\u0437") || text.contains("\u0418\u0432\u0435\u043d\u0442 \u0443\u0436\u0435 \u0438\u0434\u0435\u0442");
        Notif notif = new Notif(clean, type.color, isDynamic);
        if (active == null || System.currentTimeMillis() >= activeEnd) {
            active = notif;
            activeEnd = System.currentTimeMillis() + 2200L;
            ++activeRevision;
        } else {
            queue.offer(notif);
        }
    }

    public static void notifyModuleToggle(String name, boolean enabled) {
        String cleanName = Formatting.strip((String)name) == null ? name : Formatting.strip((String)name);
        active = new Notif("Module " + cleanName + (enabled ? " was enabled" : " was disabled"), (enabled ? NotificationType.SUCCESS : NotificationType.ERROR).color, false);
        activeEnd = System.currentTimeMillis() + 2200L;
        ++activeRevision;
    }

    public static void strengthNotification() {
        if (DynamicIsland.mc.player == null) {
            return;
        }
        EquipmentSlot[] slots = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] names = new String[]{"\u0428\u043b\u0435\u043c", "\u041d\u0430\u0433\u0440\u0443\u0434\u043d\u0438\u043a", "\u041f\u043e\u043d\u043e\u0436\u0438", "\u0411\u043e\u0442\u0438\u043d\u043a\u0438"};
        for (int i = 0; i < slots.length; ++i) {
            float pct;
            ItemStack stack = DynamicIsland.mc.player.getEquippedStack(slots[i]);
            if (stack.isEmpty() || !stack.isDamageable() || !((pct = (float)(stack.getMaxDamage() - stack.getDamage()) / (float)stack.getMaxDamage()) <= 0.1f) || !(pct > 0.0f)) continue;
            DynamicIsland.addNotification(names[i] + " \u0441\u043a\u043e\u0440\u043e \u0441\u043b\u043e\u043c\u0430\u0435\u0442\u0441\u044f!", NotificationType.WARNING);
        }
    }

    public static String getEventStatus() {
        if (cachedEventStatusMethod == null) {
            return null;
        }
        try {
            return (String)cachedEventStatusMethod.invoke(null, new Object[0]);
        }
        catch (Exception e) {
            return null;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (active != null && System.currentTimeMillis() >= activeEnd) {
            Notif next;
            active = next = queue.poll();
            activeEnd = next != null ? System.currentTimeMillis() + 2200L : 0L;
            ++activeRevision;
        }
        if (this.lastSeenRevision != activeRevision) {
            this.notifAnim = 0.0f;
            this.lastNotif = active;
            this.lastSeenRevision = activeRevision;
        }
        this.tickArmorDurabilityWarnings();
        this.tickExpiringPotionWarnings();
        this.updateRenderCache();
    }

    private void updateRenderCache() {
        Notif currentActive;
        long now = System.currentTimeMillis();
        this.cachedEventStatus = (currentActive = active) != null && currentActive.isDynamic() && now < activeEnd ? DynamicIsland.getEventStatus() : null;
    }

    private void tickArmorDurabilityWarnings() {
        Hud hud = Hud.getInstance();
        if (hud == null || hud.notificationSettings == null) {
            return;
        }
        if (!hud.notificationSettings.isSelected("\u0421\u043a\u043e\u0440\u043e\u0439 \u043f\u043e\u043b\u043e\u043c\u043a\u0435 \u0431\u0440\u043e\u043d\u0438")) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < this.nextArmorCheckAt) {
            return;
        }
        this.nextArmorCheckAt = now + 1200L;
        DynamicIsland.strengthNotification();
    }

    private void tickExpiringPotionWarnings() {
        Hud hud = Hud.getInstance();
        if (hud == null || hud.notificationSettings == null || DynamicIsland.mc.player == null) {
            return;
        }
        if (!hud.notificationSettings.isSelected("\u0417\u0430\u043a\u0430\u043d\u0447\u0438\u0432\u0430\u044e\u0449\u0435\u043c\u0441\u044f \u0437\u0435\u043b\u044c\u0435")) {
            return;
        }
        HashSet<String> activeKeys = new HashSet<String>();
        for (StatusEffectInstance effect : DynamicIsland.mc.player.getStatusEffects()) {
            String effectName = ((StatusEffect)effect.getEffectType().value()).getName().getString();
            int amp = effect.getAmplifier() + 1;
            String key = effectName + ":" + amp;
            activeKeys.add(key);
            int seconds = effect.getDuration() / 20;
            if (seconds <= 0 || seconds > 30 || this.warnedExpiringEffects.contains(key)) continue;
            DynamicIsland.addNotification("\u0417\u0435\u043b\u044c\u0435 " + effectName + " " + amp + " \u0437\u0430\u043a\u0430\u043d\u0447\u0438\u0432\u0430\u0435\u0442\u0441\u044f (" + seconds + "s)", NotificationType.WARNING);
            this.warnedExpiringEffects.add(key);
        }
        Iterator<String> it = this.warnedExpiringEffects.iterator();
        while (it.hasNext()) {
            String key = it.next();
            if (activeKeys.contains(key)) continue;
            it.remove();
        }
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public boolean canDraw(Hud hud, AbstractDraggable draggable) {
        if (hud == null) return false;
        if (!hud.isState()) return false;
        if (!Hud.interfaceSettings.isSelected("Watermark")) return false;
        return true;
    }

    private IslandCapsule resolveCapsule(boolean showNotif, Notif renderNotif) {
        if (!Hud.watermarkStatus.isValue()) {
            return new IslandCapsule(IslandState.DEFAULT, this.getDisplayName(), "", null, ThemeManager.primaryColor.getColor());
        }
        if (showNotif && renderNotif != null) {
            String updatedStatus;
            String text = renderNotif.text();
            if (renderNotif.isDynamic() && (updatedStatus = this.cachedEventStatus) != null && !updatedStatus.isEmpty()) {
                text = updatedStatus;
            }
            return new IslandCapsule(IslandState.NOTIFICATION, text, "", null, renderNotif.color().getRGB());
        }
        return new IslandCapsule(IslandState.DEFAULT, this.getDisplayName(), "", null, ThemeManager.primaryColor.getColor());
    }

    @Override
    public void drawDraggable(DrawContext context) {
        // Рисуется только водяной знак. Старая капсула по центру (уведомления о модулях и
        // строка «WiosVisuals / ms / FPS») убрана — уведомления показывает элемент HUD Notifications.
        this.drawMeowWatermark(context, GuiCompat.stack(context));
    }

    /* ------------------------------------------------------------------ *
     *  Водяной знак — компактная скруглённая панель:
     *  [лого со слабым свечением]  WiosAlpha  |  N ms  |  N FPS
     *  «WiosAlpha» переливается между цветом темы и белым.
     *  Перетаскивается мышкой в редакторе HUD (открытый чат); позицию и сохранение
     *  в конфиг ведут AbstractDraggable и ElementsFile, края экрана — код ниже.
     *  Размеры — в единицах GUI (при GUI scale 2 панель ≈ 32 px высотой).
     * ------------------------------------------------------------------ */

    /* ------------------------------------------------------------------ *
     *  Водяной знак — две отдельные капсулы, одна под другой:
     *    [лого]  WiosAlpha  (скин) Ник
     *    (v) Singleplayer   (~) 54fps   (o) 0ms
     *  Никаких свечений, теней, рамок и градиентов: только тёмный
     *  полупрозрачный фон капсул, мятные иконки и белый текст.
     *  Перетаскивается мышкой в редакторе HUD (открытый чат); позицию и
     *  сохранение в конфиг ведут AbstractDraggable и ElementsFile.
     *  Размеры — в единицах GUI (при GUI scale 2 капсула ≈ 30 px высотой).
     * ------------------------------------------------------------------ */

    /** Имя клиента в первой капсуле: белым цветом. */
    private static final String MW_CLIENT = "WiosAlpha";

    /** Высота обеих капсул одинаковая, скругление — ровно половина (полный pill). */
    private static final float MW_H = 12.5f;
    private static final float MW_ROUND = 5.0f;
    /** Просвет между верхней и нижней капсулой. */
    private static final float MW_GAP = 3.5f;
    private static final float MW_PAD = 6.0f;

    /** Лого клиента и круглая аватарка скина в верхней капсуле. */
    private static final float MW_LOGO = 9.0f;
    private static final float MW_LOGO_GAP = 5.0f;
    /** Голова игрока на месте прежнего кружка-разделителя. */
    private static final float MW_RING = 8.0f;
    private static final float MW_RING_GAP_L = 8.0f;
    private static final float MW_RING_GAP_R = 4.0f;

    /** Период пробега блика по названию клиента, мс. */
    private static final long MW_SWEEP_MS = 2200L;
    /** Ширина блика: чем меньше сигма, тем острее вспышка. */
    private static final float MW_SWEEP_SIGMA = 0.0578f;
    private static final float MW_SWEEP_STRENGTH = 0.9f;

    /** Иконки нижней капсулы и отступы вокруг них. */
    private static final float MW_ICON = 7.5f;
    private static final float MW_ICON_GAP = 3.5f;
    private static final float MW_SEG_GAP = 8.0f;
    /** Точка-разделитель сегментов единой капсулы, как «•» у ReallyVisuals. */
    private static final float MW_DOT = 3.0f;
    private static final float MW_DOT_GAP = 5.0f;

    private static final int MW_BG = new Color(0, 0, 0, 255).getRGB();
    private static final int MW_TEXT = new Color(0xF5, 0xF5, 0xF5).getRGB();
    private static final int MW_MUTED = new Color(0xA0, 0xA0, 0xA0).getRGB();

    private boolean mwPlaced;
    /** Значения обновляются по таймеру, а не каждый кадр: строки не пересоздаются зря. */
    private static final long MW_REFRESH_MS = 250L;
    private long mwRefreshAt;
    private String mwMode = "Singleplayer";
    private String mwFps = "0fps";
    private String mwPing = "0ms";
    /** Ник игрока рядом с аватаркой: на сервере — имя профиля, иначе имя сессии. */
    private String mwNick = "Player";

    /** Пинг игрока из таб-листа сервера; вне сервера — 0. */
    private static int mwPingValue() {
        if (DynamicIsland.mc.player == null || DynamicIsland.mc.getNetworkHandler() == null) {
            return 0;
        }
        PlayerListEntry entry = DynamicIsland.mc.getNetworkHandler().getPlayerListEntry(DynamicIsland.mc.player.getUuid());
        return entry == null ? 0 : Math.max(0, entry.getLatency());
    }

    /** Режим игры: «Singleplayer» в одиночной игре, иначе адрес сервера без порта. */
    private static String mwModeValue() {
        if (DynamicIsland.mc.isInSingleplayer()) {
            return "Singleplayer";
        }
        ServerInfo entry = DynamicIsland.mc.getCurrentServerEntry();
        if (entry == null || entry.address == null || entry.address.isBlank()) {
            return "Multiplayer";
        }
        String address = entry.address.trim();
        int colon = address.lastIndexOf(':');
        if (colon > 0 && address.indexOf(':') == colon) {
            address = address.substring(0, colon);
        }
        return address;
    }

    private void mwRefresh() {
        long now = System.currentTimeMillis();
        if (now < this.mwRefreshAt) {
            return;
        }
        this.mwRefreshAt = now + MW_REFRESH_MS;
        this.mwMode = mwModeValue();
        this.mwFps = DynamicIsland.mc.getCurrentFps() + " FPS";
        this.mwPing = mwPingValue() + " ms";
        this.mwNick = mwNickValue();
    }

    /** Ник на сервере: имя игрового профиля, а до входа в мир — имя сессии. */
    private static String mwNickValue() {
        String name = null;
        if (DynamicIsland.mc.player != null && DynamicIsland.mc.player.getGameProfile() != null) {
            name = DynamicIsland.mc.player.getGameProfile().name();
        }
        if ((name == null || name.isBlank()) && DynamicIsland.mc.getSession() != null) {
            name = DynamicIsland.mc.getSession().getUsername();
        }
        return name == null || name.isBlank() ? "Player" : name;
    }

    /** Текстура скина локального игрока; до входа в мир её ещё нет. */
    private static Identifier mwSkinTexture() {
        try {
            if (DynamicIsland.mc.player != null && DynamicIsland.mc.player.getSkin() != null) {
                return net.minecraft.util.Identifier.of("textures/entity/player/wide/steve.png") /* TODO port 1.21.11: skin API class_12081 */;
            }
        } catch (Throwable ignored) {
            // Скин ещё не подгружен — рисуем запасной кружок.
        }
        return null;
    }

    /**
     * Название клиента, переливающееся цветами темы: под строкой лежит градиент от
     * основного цвета темы к дополнительному, а поверх по буквам бежит светлый блик.
     * Возвращает правый край строки.
     */
    private float mwShimmerText(MatrixStack matrices, FontRenderer font, String text, float x, float pillY) {
        float total = font.getStringWidth(text);
        float y = mwSnap(font.getCenteredY(pillY, MW_H));
        int from = HudTheme.accent(255);
        int to = HudTheme.accentSecondary(255);
        float phase = (float)(System.currentTimeMillis() % MW_SWEEP_MS) / (float)MW_SWEEP_MS;
        float cursor = x;
        for (int i = 0; i < text.length(); ++i) {
            String glyph = String.valueOf(text.charAt(i));
            float glyphWidth = font.getStringWidth(glyph);
            float centre = total <= 0.0f ? 0.0f : (cursor - x + glyphWidth / 2.0f) / total;
            int base = ColorAssist.lerp(centre, from, to);
            // Гауссова вспышка, зациклённая по времени: светлый блик бежит по названию.
            float delta = centre - phase;
            delta -= (float)Math.floor(delta + 0.5f);
            float highlight = (float)Math.exp(-(delta * delta) / MW_SWEEP_SIGMA);
            font.drawString(matrices, glyph, cursor, y, ColorAssist.lerp(highlight * MW_SWEEP_STRENGTH, base, -1));
            cursor += glyphWidth;
        }
        return x + total;
    }

    /**
     * Капсула в тоне Potions. У высокой панели Potions на глаз заметен синеватый верх (panelTop),
     * а низкая капсула с тем же градиентом выглядела просто чёрной — поэтому заливка ровная,
     * цветом верха панели.
     */
    /** Точь-в-точь капсула ReallyVisuals: сплошной почти-чёрный фон, без оттенка темы. */
    private static final int MW_PILL_FILL = 170 << 24 | 0x111221;

    private void mwPill(MatrixStack matrices, float x, float y, float width) {
        rectangle.render(ShapeProperties.create(matrices, x, y, width, MW_H).round(MW_ROUND).softness(2.0f)
                .color(MW_PILL_FILL, MW_PILL_FILL, MW_PILL_FILL, MW_PILL_FILL).build());
    }

    /** Текст по центру капсулы по вертикали; возвращает правый край строки. */
    private float mwText(MatrixStack matrices, FontRenderer font, String text, float x, float pillY, int color) {
        font.drawString(matrices, text, mwSnap(x), mwSnap(font.getCenteredY(pillY, MW_H)), color);
        return x + font.getStringWidth(text);
    }

    /** Иконка цветом темы по центру капсулы по вертикали; возвращает правый край иконки. */
    private float mwIcon(DrawContext context, Identifier texture, float x, float pillY, float size) {
        return this.mwIcon(context, texture, x, pillY, size, mwThemeColor());
    }

    /** Вершины логотипа «W» в долях квадрата (x, y) — рисуется векторно, без текстуры и мыла. */
    private static final float[] WM_W_POINTS = new float[]{
            0.0000f, 0.4402f, 0.4154f, 0.3294f, 0.1859f, 0.5118f, 0.2775f, 0.6601f, 0.4564f, 0.4507f,
            0.5567f, 0.5903f, 1.0000f, 0.1305f, 0.5593f, 0.8172f, 0.4459f, 0.6444f, 0.2539f, 0.8695f};
    /** Разбиение вогнутого контура на треугольники (индексы вершин). */
    private static final int[] WM_W_TRIANGLES = new int[]{0, 1, 2, 9, 0, 2, 9, 2, 3, 9, 3, 4, 5, 6, 7, 4, 5, 7, 4, 7, 8, 4, 8, 9};

    /**
     * Логотип «W» геометрией: треугольники в экранных координатах, поэтому края
     * остаются резкими при любом масштабе GUI. Возвращает правый край значка.
     */
    private float mwVectorLogo(MatrixStack matrices, float x, float pillY, float size, int color) {
        Rectangle.flushPending();
        float left = mwSnap(x);
        float top = mwSnap(pillY + (MW_H - size) / 2.0f);
        org.joml.Matrix4f m = matrices.peek().getPositionMatrix();
        int rgb = ColorAssist.lerp(0.22f, color, 0xFFFFFFFF) & 0xFFFFFF;

        // Свечение: копии буквы, разнесённые кольцами вокруг неё, складываются аддитивно.
        // Ближние кольца плотнее, дальние бледнее — получается мягкий неоновый ореол.
        net.minecraft.client.render.BufferBuilder glow = net.minecraft.client.render.Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.TRIANGLES, net.minecraft.client.render.VertexFormats.POSITION_COLOR);
        float pulse = 0.85f + 0.15f * (float)Math.sin((double)System.currentTimeMillis() / 520.0);
        for (int ring = 1; ring <= MW_GLOW_RINGS; ++ring) {
            float radius = ring * MW_GLOW_STEP;
            int alpha = Math.round(MW_GLOW_ALPHA * pulse * (1.0f - (float)(ring - 1) / MW_GLOW_RINGS));
            int argb = Math.max(0, Math.min(255, alpha)) << 24 | rgb;
            for (int dir = 0; dir < 8; ++dir) {
                double angle = Math.PI / 4.0 * dir;
                float dx = (float)Math.cos(angle) * radius;
                float dy = (float)Math.sin(angle) * radius;
                for (int index : WM_W_TRIANGLES) {
                    glow.vertex(m, left + dx + WM_W_POINTS[index * 2] * size, top + dy + WM_W_POINTS[index * 2 + 1] * size, 0.0f).color(argb);
                }
            }
        }
        code.wiosvisual.utils.display.render.pipeline.WiosRender.draw(code.wiosvisual.utils.display.render.pipeline.WiosRender.posColor(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.TRIANGLES, false, false, true), glow);

        // Сама буква поверх ореола: чёткая, вершины на целых пикселях.
        net.minecraft.client.render.BufferBuilder buffer = net.minecraft.client.render.Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.TRIANGLES, net.minecraft.client.render.VertexFormats.POSITION_COLOR);
        int solid = 0xFF000000 | rgb;
        for (int index : WM_W_TRIANGLES) {
            float vx = mwSnap(left + WM_W_POINTS[index * 2] * size);
            float vy = mwSnap(top + WM_W_POINTS[index * 2 + 1] * size);
            buffer.vertex(m, vx, vy, 0.0f).color(solid);
        }
        code.wiosvisual.utils.display.render.pipeline.WiosRender.draw(code.wiosvisual.utils.display.render.pipeline.WiosRender.posColor(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.TRIANGLES, false, false), buffer);
        return x + size;
    }

    /** Ореол логотипа: число колец, шаг между ними (в пикселях GUI) и плотность ближнего кольца. */
    private static final int MW_GLOW_RINGS = 4;
    private static final float MW_GLOW_STEP = 0.7f;
    private static final float MW_GLOW_ALPHA = 45.0f;

    /** То же, но заданным цветом: лого красится цветами темы. */
    private float mwIcon(DrawContext context, Identifier texture, float x, float pillY, float size, int color) {
        // Мягкий ореол вокруг иконки — как у логотипа (mwVectorLogo), а не плоская заливка.
        BackgroundIcons.drawGlowing(context, texture, mwSnap(x), mwSnap(pillY + (MW_H - size) / 2.0f), size, color);
        return x + size;
    }

    /**
     * Текущий цвет темы с тем же бегущим бликом, что и у названия: лого дышит
     * в такт строке «WiosAlpha», а не стоит одним мятным пятном.
     */
    private static int mwThemeColor() {
        float phase = (float)(System.currentTimeMillis() % MW_SWEEP_MS) / (float)MW_SWEEP_MS;
        // Треугольная волна 0→1→0: цвет ходит между двумя цветами темы туда-обратно.
        float wave = phase < 0.5f ? phase * 2.0f : (1.0f - phase) * 2.0f;
        return ColorAssist.lerp(wave, HudTheme.accent(255), HudTheme.accentSecondary(255));
    }

    /**
     * Круглая голова игрока по центру капсулы; пока скин не подгружен — прежний
     * мятный кружок того же размера, чтобы ширина капсулы не прыгала.
     */
    private float mwSkin(DrawContext context, float x, float pillY) {
        float y = mwSnap(pillY + (MW_H - MW_RING) / 2.0f);
        Identifier skin = mwSkinTexture();
        if (skin == null) {
            return this.mwIcon(context, BackgroundIcons.WM_RING, x, pillY, MW_RING);
        }
        BackgroundIcons.drawSkinHead(context, skin, mwSnap(x), y, MW_RING, 255);
        return x + MW_RING;
    }

    private void drawMeowWatermark(DrawContext context, MatrixStack matrices) {
        if (!this.mwPlaced) {
            this.mwPlaced = true;
            // Место со скриншота — левый верхний угол. Переносим туда только позиции
            // прошлых версий, чтобы не сбрасывать точку, выставленную в редакторе HUD.
            boolean legacyCentered = this.getY() == 6 && (this.getX() == Math.round(window.getScaledWidth() * 0.5f - 80.0f)
                    || this.getX() == Math.round(window.getScaledWidth() * 0.3f));
            if (legacyCentered || this.getY() == 7 || this.getX() == 10 && (this.getY() == 10 || this.getY() == 4)) {
                this.setX(6);
                this.setY(6);
            }
        }
        this.mwRefresh();

        FontRenderer authorFont = Fonts.getSize(12, Fonts.Type.REGULAR);
        FontRenderer statFont = Fonts.getSize(12, Fonts.Type.DEFAULT);

        // Какие части включены в настройке «Элементы ватермарки».
        boolean showLogo = Hud.watermarkElements.isSelected("Лого");
        boolean showTitle = Hud.watermarkElements.isSelected("Название");
        boolean showNick = Hud.watermarkElements.isSelected("Ник");
        java.util.List<String> statValues = new java.util.ArrayList<String>();
        java.util.List<Identifier> statIcons = new java.util.ArrayList<Identifier>();
        if (Hud.watermarkElements.isSelected("Сервер")) {
            statValues.add(this.mwMode);
            statIcons.add(BackgroundIcons.WM_MODE);
        }
        if (Hud.watermarkElements.isSelected("FPS")) {
            statValues.add(this.mwFps);
            statIcons.add(BackgroundIcons.WM_FPS);
        }
        if (Hud.watermarkElements.isSelected("Пинг")) {
            statValues.add(this.mwPing);
            statIcons.add(BackgroundIcons.WM_PING);
        }
        boolean hasLead = showLogo || showTitle || showNick;
        if (!hasLead && statValues.isEmpty()) {
            return;
        }

        // Единая капсула в одну строку (как у ReallyVisuals): лого+название(+ник) слева,
        // дальше через точку-разделитель — сервер/FPS/пинг. Раньше это были две капсулы
        // друг под другом; свели в одну по образцу референса.
        boolean hasLogo = showLogo;
        boolean hasTitle = showTitle;
        boolean hasNick = showNick;
        boolean hasStats = !statValues.isEmpty();
        float leadW = 0.0f;
        if (hasLogo) {
            leadW += MW_LOGO;
            if (hasTitle || hasNick || hasStats) {
                leadW += MW_LOGO_GAP + MW_DOT + MW_DOT_GAP;
            }
        }
        if (hasTitle) {
            leadW += this.font.getStringWidth(this.watermarkTitle());
            if (hasNick || hasStats) {
                leadW += MW_LOGO_GAP + MW_DOT + MW_DOT_GAP;
            }
        }
        if (hasNick) {
            leadW += MW_RING + MW_RING_GAP_R + authorFont.getStringWidth(this.mwNick);
            if (hasStats) {
                leadW += MW_LOGO_GAP + MW_DOT + MW_DOT_GAP;
            }
        }
        float width = MW_PAD + leadW;
        for (int i = 0; i < statValues.size(); ++i) {
            String value = statValues.get(i);
            if (i > 0) {
                width += MW_DOT_GAP + MW_DOT + MW_DOT_GAP;
            }
            width += MW_ICON + MW_ICON_GAP + statFont.getStringWidth(value);
        }
        width = mwSnap(width + MW_PAD);
        float height = MW_H;

        // Водяной знак всегда целиком на экране — и в игре, и во время перетаскивания
        // (общий clampToScreen у элементов HUD пустой).
        int maxX = Math.max(0, window.getScaledWidth() - (int)Math.ceil(width));
        int maxY = Math.max(0, window.getScaledHeight() - (int)Math.ceil(height));
        this.setX(Math.max(0, Math.min(this.getX(), maxX)));
        this.setY(Math.max(0, Math.min(this.getY(), maxY)));
        float x = mwSnap(this.getX());
        float y = mwSnap(this.getY());

        this.mwPill(matrices, x, y, width);
        Rectangle.flushPending();

        float cx = x + MW_PAD;
        if (showLogo) {
            // Логотип и иконки ватермарки — цвет текущей темы.
            cx = this.mwVectorLogo(matrices, cx, y, MW_LOGO, HudTheme.fixedAccent(255));
        }
        if (showLogo && (showTitle || showNick || !statValues.isEmpty())) {
            cx += MW_LOGO_GAP;
            this.mwDot(matrices, cx, y);
            cx += MW_DOT_GAP;
        }
        if (showTitle) {
            // Референс: «WiosVisuals» ровным белым без бегущего блика — переливание было только у Meow-стиля.
            String title = this.watermarkTitle();
            this.font.drawString(matrices, title, mwSnap(cx), mwSnap(this.font.getCenteredY(y, MW_H)), 0xFFFFFFFF);
            cx += this.font.getStringWidth(title);
            if (showNick || !statValues.isEmpty()) {
                cx += MW_LOGO_GAP;
                this.mwDot(matrices, cx, y);
                cx += MW_DOT_GAP;
            }
        }
        if (showNick) {
            cx = this.mwSkin(context, cx, y) + MW_RING_GAP_R;
            cx = this.mwText(matrices, authorFont, this.mwNick, cx, y, MW_TEXT);
            if (!statValues.isEmpty()) {
                cx += MW_LOGO_GAP;
                this.mwDot(matrices, cx, y);
                cx += MW_DOT_GAP;
            }
        }
        for (int i = 0; i < statValues.size(); ++i) {
            if (i > 0) {
                this.mwDot(matrices, cx, y);
                cx += MW_DOT + MW_DOT_GAP;
            }
            cx = this.mwIcon(context, statIcons.get(i), cx, y, MW_ICON, HudTheme.fixedAccent(255)) + MW_ICON_GAP;
            cx = this.mwText(matrices, statFont, statValues.get(i), cx, y, MW_TEXT);
        }
        Rectangle.flushPending();

        this.setWidth((int)Math.ceil(width));
        this.setHeight((int)Math.ceil(height));
        this.clampToScreen();
    }

    /** Тонкая точка-разделитель сегментов капсулы («•» в ReallyVisuals), по центру капсулы. */
    private void mwDot(MatrixStack matrices, float x, float pillY) {
        float h = MW_H / 2.0f;
        rectangle.render(ShapeProperties.create(matrices, mwSnap(x + MW_DOT / 2.0f - 0.375f), mwSnap(pillY + (MW_H - h) / 2.0f), 0.75f, h)
                .round(0.0f).color(0x64C8C8C8).build());
    }

    /** Привязка к целому физическому пикселю: иначе сглаженный текст размазывается на два пикселя. */
    private static float mwSnap(float value) {
        double scale = DynamicIsland.mc.getWindow().getScaleFactor();
        return (float)(Math.round(value * scale) / scale);
    }



    /* ------------------------------------------------------------------ *
     *  Ватермарка в стиле WexSide: логотип с бегущим бликом, затем секции
     *  «ник / fps / пинг», разделённые тонкими вертикальными полосками.
     * ------------------------------------------------------------------ */

    /* Светлая тема: капсула ватермарки становится светлой, а текст — тёмным. */
    private static final int WM_LIGHT_TOP = new Color(255, 255, 255, 255).getRGB();
    private static final int WM_LIGHT_BOTTOM = new Color(233, 233, 239, 255).getRGB();
    private static final int WM_LIGHT_TEXT = new Color(24, 24, 27, 255).getRGB();

    private static int islandTop() {
        return HudTheme.isLight() ? WM_LIGHT_TOP : HudTheme.panelTop(255);
    }

    private static int islandBottom() {
        return HudTheme.isLight() ? WM_LIGHT_BOTTOM : HudTheme.panelBottom(255);
    }

    /** Основной цвет текста ватермарки: на светлой теме белый текст нечитаем. */
    private static int islandText() {
        return HudTheme.isLight() ? WM_LIGHT_TEXT : ThemeManager.textColor.getColor();
    }

    private static final float WM_PAD = 6.0f;
    /** Сторона неонового значка — единственного логотипа ватермарки. */
    private static final float WM_BADGE = 11.0f;
    /** Заливка значка: тёмная на любой теме, иначе неон не читается. */
    private static final int WM_BADGE_FILL = new Color(0, 0, 0, 255).getRGB();
    private static final float WM_LOGO_GAP = 4.0f;

    /** Ширина ведущего логотипа ватермарки с учётом выбранного режима (перо/аватарка). */
    private static float leadLogoWidth() {
        return WM_BADGE;
    }
    private static final float WM_SEG_GAP = 5.0f;
    private static final float WM_BAR_W = 0.5f;
    private static final float WM_BAR_H = 5.0f;
    /** Период пробега блика по логотипу, мс. */
    private static final long WM_SWEEP_MS = 2200L;
    /** Ширина блика: чем меньше, тем острее вспышка (взято из WexSide). */
    private static final float WM_SWEEP_SIGMA = 0.0578f;
    private static final float WM_SWEEP_STRENGTH = 0.9f;

    /** Первая половина названия; обе части рисуются общим градиентом темы. */
    private static final String WM_TITLE_HEAD = "Wios";
    /** Вторая половина названия. */
    private static final String WM_TITLE_ACCENT = "Visuals";

    private String watermarkTitle() {
        return WM_TITLE_HEAD + WM_TITLE_ACCENT;
    }

    /**
     * Рисует «WiosVisuals» одной переливающейся строкой: градиент от основного цвета
     * клиента к его светлому оттенку тянется через всё название, а поверх пробегает
     * блик, так что «Wios» живёт так же, как «Visuals».
     * Возвращает ширину отрисованного текста.
     */
    private float drawTitle(MatrixStack matrices, float x, float y) {
        String title = this.watermarkTitle();
        float total = this.font.getStringWidth(title);
        int from = HudTheme.accent(255);
        int to = HudTheme.accentSecondary(255);
        float phase = (float)(System.currentTimeMillis() % WM_SWEEP_MS) / (float)WM_SWEEP_MS;
        float cursor = x;
        for (int i = 0; i < title.length(); ++i) {
            String glyph = String.valueOf(title.charAt(i));
            float glyphWidth = this.font.getStringWidth(glyph);
            float centre = total <= 0.0f ? 0.0f : (cursor - x + glyphWidth / 2.0f) / total;
            int base = ColorAssist.lerp(centre, from, to);
            // Гауссова вспышка, зациклённая по времени: светлый блик бежит по названию.
            float delta = centre - phase;
            delta -= (float)Math.floor(delta + 0.5f);
            float highlight = (float)Math.exp(-(delta * delta) / WM_SWEEP_SIGMA);
            this.font.drawString(matrices, glyph, cursor, y, ColorAssist.lerp(highlight * WM_SWEEP_STRENGTH, base, -1));
            cursor += glyphWidth;
        }
        return cursor - x;
    }

    private String watermarkNick() {
        String name = mc.getSession() == null ? null : mc.getSession().getUsername();
        return name == null || name.isBlank() ? "Player" : name;
    }

    private String watermarkFps() {
        return DynamicIsland.getFpsString() + " FPS";
    }

    private String watermarkPing() {
        return this.getPlayerPing() + " ms";
    }

    /** Server address segment, or "singleplayer" when there is no server entry. */
    private String watermarkServer() {
        ServerInfo server = mc.getCurrentServerEntry();
        if (server == null || server.address == null || server.address.isBlank()) {
            return "singleplayer";
        }
        return server.address;
    }

    /** Head texture of the local player, used for the round skin badge before the nick. */
    private Identifier watermarkSkin() {
        try {
            if (mc.player != null) {
                return net.minecraft.util.Identifier.of("textures/entity/player/wide/steve.png") /* TODO port 1.21.11: skin API class_12081 */;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static final float WM_ICON = 7.5f;
    private static final float WM_ICON_GAP = 3.5f;
    /** Кружок аватарки Discord в ватермарке. */
    private static final float WM_AVATAR = 13.5f;

    /** Ник владельца Discord или пустая строка, если Discord не запущен. */
    private static String watermarkDiscord() {
        String name = DiscordAvatar.userName();
        return name == null ? "" : name;
    }

    private float watermarkWidth() {
        FontRenderer segFont = Fonts.getSize(13, Fonts.Type.DEFAULT);
        float width = WM_PAD + leadLogoWidth() + WM_LOGO_GAP + this.font.getStringWidth(this.watermarkTitle());
        String discord = watermarkDiscord();
        if (!discord.isEmpty()) {
            width += WM_SEG_GAP + WM_BAR_W + WM_SEG_GAP + WM_AVATAR + WM_ICON_GAP + segFont.getStringWidth(discord);
        }
        for (String segment : new String[]{this.watermarkFps(), this.watermarkPing()}) {
            width += WM_SEG_GAP + WM_BAR_W + WM_SEG_GAP + WM_ICON + WM_ICON_GAP + segFont.getStringWidth(segment);
        }
        return width + WM_PAD;
    }

    private void renderWatermark(DrawContext context, MatrixStack matrices, float x, float midY) {
        FontRenderer segFont = Fonts.getSize(13, Fonts.Type.DEFAULT);
        int textColor = DynamicIsland.islandText();
        int segmentColor = ColorAssist.multAlpha(textColor, 0.85f);
        int iconColor = HudTheme.accentBright(255);
        float cursor = x;
        String title = this.watermarkTitle();
        cursor += this.drawTitle(matrices, cursor, this.font.getCenteredY(midY - 7.5f, 15.0f, title));

        // Секция Discord: круглая аватарка владельца и его ник. Без запущенного Discord
        // ника нет — тогда секцию целиком пропускаем, чтобы не оставлять пустой разделитель.
        String discord = watermarkDiscord();
        if (!discord.isEmpty()) {
            cursor += WM_SEG_GAP;
            int barTop = ColorAssist.multAlpha(iconColor, 0.1f);
            int barMid = ColorAssist.multAlpha(iconColor, 0.55f);
            rectangle.render(ShapeProperties.create(matrices, cursor, midY - WM_BAR_H / 2.0f, WM_BAR_W, WM_BAR_H)
                    .round(WM_BAR_W / 2.0f).softness(1.0f).color(barTop, barTop, barMid, barMid).build());
            cursor += WM_BAR_W + WM_SEG_GAP;
            Identifier avatar = DiscordAvatar.texture();
            float avatarY = midY - WM_AVATAR / 2.0f;
            if (avatar != null) {
                BackgroundIcons.drawAvatar(context, avatar, cursor, avatarY, WM_AVATAR, WM_AVATAR / 2.0f, 255);
            } else {
                BackgroundIcons.drawGlowing(context, BackgroundIcons.DISCORD, cursor, avatarY, WM_AVATAR, iconColor);
            }
            cursor += WM_AVATAR + WM_ICON_GAP;
            segFont.drawString(matrices, discord, cursor, segFont.getCenteredY(midY - 7.5f, 15.0f, discord), segmentColor);
            cursor += segFont.getStringWidth(discord);
        }

        // Each segment is introduced by its own icon: performance first, then latency.
        String[] segments = new String[]{this.watermarkFps(), this.watermarkPing()};
        float iconY = midY - WM_ICON / 2.0f;
        for (int i = 0; i < segments.length; ++i) {
            String segment = segments[i];
            cursor += WM_SEG_GAP;
            // Секции разделены тонкой вертикальной полоской: сверху цвет темы, снизу — прозрачнее.
            int barTop = ColorAssist.multAlpha(iconColor, 0.1f);
            int barMid = ColorAssist.multAlpha(iconColor, 0.55f);
            rectangle.render(ShapeProperties.create(matrices, cursor, midY - WM_BAR_H / 2.0f, WM_BAR_W, WM_BAR_H).round(WM_BAR_W / 2.0f).softness(1.0f).color(barTop, barTop, barMid, barMid).build());
            cursor += WM_BAR_W + WM_SEG_GAP;
            BackgroundIcons.drawGlowing(context, i == 0 ? BackgroundIcons.ESP : BackgroundIcons.COOLDOWNS, cursor, iconY, WM_ICON, iconColor);
            cursor += WM_ICON + WM_ICON_GAP;
            segFont.drawString(matrices, segment, cursor, segFont.getCenteredY(midY - 7.5f, 15.0f, segment), segmentColor);
            cursor += segFont.getStringWidth(segment);
        }
    }

    /**
     * Рисует строку посимвольно: под текстом лежит градиент из цветов темы, а поверх
     * бежит светлый блик — гауссова вспышка, положение которой зациклено по времени.
     */
    private void drawSweepString(MatrixStack matrices, String text, float x, float y) {
        if (text.isEmpty()) {
            return;
        }
        int from = ThemeManager.primaryColor.getColor();
        int to = ThemeManager.secondaryColor.getColor();
        float total = this.font.getStringWidth(text);
        float phase = (float)(System.currentTimeMillis() % WM_SWEEP_MS) / (float)WM_SWEEP_MS;
        float cursor = x;
        for (int i = 0; i < text.length(); ++i) {
            String glyph = String.valueOf(text.charAt(i));
            float centre = total <= 0.0f ? 0.0f : (cursor - x + this.font.getStringWidth(glyph) / 2.0f) / total;
            int base = ColorAssist.lerp(centre, from, to);
            float delta = centre - phase;
            delta -= (float)Math.floor(delta + 0.5f);
            float highlight = (float)Math.exp(-(delta * delta) / WM_SWEEP_SIGMA);
            this.font.drawString(matrices, glyph, cursor, y, ColorAssist.lerp(highlight * WM_SWEEP_STRENGTH, base, -1));
            cursor += this.font.getStringWidth(glyph);
        }
    }

    private float centeredTextY(FontRenderer font, float boxY, float boxHeight) {
        return font.getCenteredY(boxY, boxHeight);
    }

    private String getDisplayName() {
        StringBuilder builder = new StringBuilder("WiosVisuals");
        builder.append(SEPARATOR).append(this.getPlayerPing()).append(" ms");
        builder.append(SEPARATOR).append(DynamicIsland.getFpsString()).append(" FPS");
        return builder.toString();
    }

    private static String getFpsString() {
        String debug = String.valueOf(DynamicIsland.mc.getCurrentFps());
        if (debug == null || debug.isEmpty()) {
            return "0";
        }
        String first = debug.split(" ")[0];
        return first.isEmpty() ? "0" : first;
    }

    private int getPlayerPing() {
        if (DynamicIsland.mc.player == null || mc.getNetworkHandler() == null) {
            return 0;
        }
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(DynamicIsland.mc.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }

    private static int blend(int from, int to, float t) {
        float p = MathHelper.clamp((float)t, (float)0.0f, (float)1.0f);
        int fr = from >> 16 & 0xFF;
        int fg = from >> 8 & 0xFF;
        int fb = from & 0xFF;
        int fa = from >>> 24 & 0xFF;
        int tr = to >> 16 & 0xFF;
        int tg = to >> 8 & 0xFF;
        int tb = to & 0xFF;
        int ta = to >>> 24 & 0xFF;
        int r = (int)((float)fr + (float)(tr - fr) * p);
        int g = (int)((float)fg + (float)(tg - fg) * p);
        int b = (int)((float)fb + (float)(tb - fb) * p);
        int a = (int)((float)fa + (float)(ta - fa) * p);
        return a << 24 | r << 16 | g << 8 | b;
    }

    static {
        try {
            Class<?> trackerClass = Class.forName("code.wiosvisual.features.impl.movement.autoMystGlider.EventStatusTracker");
            cachedEventStatusMethod = trackerClass.getMethod("getDisplayStatus", new Class[0]);
        }
        catch (Exception ignored) {
            cachedEventStatusMethod = null;
        }
    }

    private record Notif(String text, Color color, boolean isDynamic) {
    }

    public static enum NotificationType {
        SUCCESS(new Color(4908362)),
        ERROR(new Color(0xFF4545)),
        WARNING(new Color(16758832)),
        INFO(new Color(4894207)),
        DEFAULT(new Color(11032055));

        public final Color color;

        private NotificationType(Color color) {
            this.color = color;
        }
    }

    private record IslandCapsule(IslandState state, String text, String prefix, String timer, int accentColor) {
    }

    private static enum IslandState {
        DEFAULT,
        NOTIFICATION;

    }
}
