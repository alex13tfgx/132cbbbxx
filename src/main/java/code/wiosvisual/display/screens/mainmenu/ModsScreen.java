package code.wiosvisual.display.screens.mainmenu;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.utils.client.mods.ModManager;
import code.wiosvisual.utils.client.mods.ModManager.ModEntry;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

/**
 * Экран «Моды» из главного меню: список модов из папки {@code mods}, установка новых .jar
 * (кнопкой или перетаскиванием файлов в окно игры), включение/выключение и удаление.
 */
public class ModsScreen
extends Screen
implements QuickImports {
    private static final float ROW_H = 30.0f;
    private static final float ROW_GAP = 4.0f;
    private static final float HEADER_BTN_H = 18.0f;
    private static final long CONFIRM_MS = 3000L;
    private static final long MESSAGE_MS = 6000L;

    private final NeverloseMenu parent;
    private final FontRenderer titleFont = Fonts.getSize(18, Fonts.Type.SEMI);
    private final FontRenderer nameFont = Fonts.getSize(14, Fonts.Type.DEFAULT);
    private final FontRenderer smallFont = Fonts.getSize(12, Fonts.Type.DEFAULT);

    private List<ModEntry> entries = new ArrayList<>();
    private int scroll;
    private final Map<String, Float> hover = new HashMap<>();
    private final Map<String, Float> switchAnim = new HashMap<>();
    private Path confirmDelete;
    private long confirmDeleteTime;
    private String message;
    private boolean messageError;
    private long messageTime;
    private volatile boolean pickerOpen;
    private long lastFrame = System.currentTimeMillis();

    public ModsScreen(NeverloseMenu parent) {
        super(Text.literal((String)"Mods"));
        this.parent = parent;
        this.refresh();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    private void refresh() {
        this.entries = ModManager.scan();
        this.scroll = MathHelper.clamp(this.scroll, 0, this.maxScroll());
    }

    private void showMessage(String text, boolean error) {
        this.message = text;
        this.messageError = error;
        this.messageTime = System.currentTimeMillis();
    }

    private void showResults(List<ModManager.Result> results) {
        if (results.isEmpty()) {
            return;
        }
        boolean anyError = results.stream().anyMatch(ModManager.Result::error);
        if (results.size() == 1) {
            this.showMessage(results.get(0).message(), anyError);
            return;
        }
        long ok = results.stream().filter(r -> !r.error()).count();
        ModManager.Result firstError = results.stream().filter(ModManager.Result::error).findFirst().orElse(null);
        String text = NeverloseMenu.tr("Установлено ", "Installed ") + ok + "/" + results.size();
        this.showMessage(firstError == null ? text : text + " — " + firstError.message(), anyError);
    }

    /* ---------------------------- разметка ---------------------------- */

    private float panelW() {
        return Math.min(460.0f, (float)this.width - 32.0f);
    }

    private float panelH() {
        return Math.min(290.0f, (float)this.height - 32.0f);
    }

    private float panelX() {
        return ((float)this.width - this.panelW()) / 2.0f;
    }

    private float panelY() {
        return ((float)this.height - this.panelH()) / 2.0f;
    }

    private float listX() {
        return this.panelX() + 10.0f;
    }

    private float listY() {
        return this.panelY() + 52.0f;
    }

    private float listW() {
        return this.panelW() - 20.0f;
    }

    private float listH() {
        return this.panelH() - 52.0f - 36.0f;
    }

    private int visibleRows() {
        return Math.max(1, (int)((this.listH() + ROW_GAP) / (ROW_H + ROW_GAP)));
    }

    private int maxScroll() {
        return Math.max(0, this.entries.size() - this.visibleRows());
    }

    /** x, y, w, h кнопок в шапке: 0 — «Добавить», 1 — «Папка». */
    private float[] headerButton(int idx) {
        float addW = 78.0f;
        float folderW = 62.0f;
        float right = this.panelX() + this.panelW() - 12.0f;
        float y = this.panelY() + 12.0f;
        return idx == 0 ? new float[]{right - addW, y, addW, HEADER_BTN_H} : new float[]{right - addW - 6.0f - folderW, y, folderW, HEADER_BTN_H};
    }

    private float[] doneButton() {
        float w = 70.0f;
        return new float[]{this.panelX() + this.panelW() - 12.0f - w, this.panelY() + this.panelH() - 28.0f, w, HEADER_BTN_H};
    }

    private float rowY(int visibleIndex) {
        return this.listY() + (float)visibleIndex * (ROW_H + ROW_GAP);
    }

    private float[] deleteButton(float rowY) {
        return new float[]{this.listX() + this.listW() - 26.0f, rowY + 6.0f, 18.0f, 18.0f};
    }

    private float[] restoreButton(float rowY) {
        return new float[]{this.listX() + this.listW() - 70.0f, rowY + 6.0f, 62.0f, 18.0f};
    }

    private float[] toggleSwitch(float rowY) {
        return new float[]{this.listX() + this.listW() - 26.0f - 8.0f - 28.0f, rowY + 8.0f, 28.0f, 14.0f};
    }

    /* ---------------------------- отрисовка ---------------------------- */

    /** 1.21.11: ванильный фон (панорама, blur, затемнение) рисуется отложенно поверх нашего немедленного рендера — отключаем. */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = GuiCompat.stack(context);
        int w = this.width;
        int h = this.height;
        long now = System.currentTimeMillis();
        float dt = Math.min(0.05f, (float)(now - this.lastFrame) / 1000.0f);
        this.lastFrame = now;
        float lerp = 1.0f - (float)Math.pow(0.001f, dt);

        this.parent.drawBackground(matrix, w, h);

        float px = this.panelX();
        float py = this.panelY();
        float pw = this.panelW();
        float ph = this.panelH();
        rectangle.render(ShapeProperties.create(matrix, px - 1.0f, py - 1.0f, pw + 2.0f, ph + 2.0f).round(9.0f).softness(0.5f).color(new Color(255, 255, 255, 20).getRGB()).build());
        rectangle.render(ShapeProperties.create(matrix, px, py, pw, ph).round(8.0f).softness(0.5f).color(new Color(14, 14, 15, 238).getRGB()).build());
        rectangle.render(ShapeProperties.create(matrix, px + 12.0f, py + 14.0f, 2.0, 12.0).round(1.0f).color(new Color(235, 235, 235, 235).getRGB()).build());
        Rectangle.flushPending();
        this.titleFont.drawString(matrix, NeverloseMenu.tr("Моды", "Mods"), px + 19.0f, py + 13.0f, -1);
        long enabledCount = this.entries.stream().filter(e -> e.enabled && !e.pendingDelete).count();
        String subtitle = NeverloseMenu.tr("Включено " + enabledCount + " из " + this.entries.size() + " · перетащите .jar в окно игры",
                "Enabled " + enabledCount + " of " + this.entries.size() + " · drop .jar files into the game window");
        this.smallFont.drawString(matrix, this.trim(this.smallFont, subtitle, pw - 175.0f), px + 12.0f, py + 33.0f, new Color(130, 130, 130).getRGB());

        String[] headerLabels = {this.pickerOpen ? "..." : NeverloseMenu.tr("+ Добавить", "+ Add"), NeverloseMenu.tr("Папка", "Folder")};
        for (int i = 0; i < 2; ++i) {
            float[] b = this.headerButton(i);
            this.drawButton(matrix, "header" + i, b, headerLabels[i], mouseX, mouseY, lerp, i == 0);
        }

        this.renderList(matrix, mouseX, mouseY, lerp, now);

        // Нижняя строка: сообщение о последнем действии или напоминание о перезапуске.
        float bottomY = py + ph - 23.0f;
        float textW = pw - 110.0f;
        if (this.message != null && now - this.messageTime < MESSAGE_MS) {
            int color = this.messageError ? new Color(230, 120, 120).getRGB() : new Color(140, 210, 140).getRGB();
            this.smallFont.drawString(matrix, this.trim(this.smallFont, this.message, textW), px + 12.0f, bottomY, color);
        } else if (ModManager.restartRequired()) {
            this.smallFont.drawString(matrix, this.trim(this.smallFont, NeverloseMenu.tr("Изменения применятся после перезапуска игры", "Changes apply after restarting the game"), textW),
                    px + 12.0f, bottomY, new Color(232, 198, 106).getRGB());
        }
        this.drawButton(matrix, "done", this.doneButton(), NeverloseMenu.tr("Готово", "Done"), mouseX, mouseY, lerp, false);
    }

    private void renderList(MatrixStack matrix, int mouseX, int mouseY, float lerp, long now) {
        float lx = this.listX();
        float lw = this.listW();
        if (this.entries.isEmpty()) {
            float cy = this.listY() + this.listH() / 2.0f;
            rectangle.render(ShapeProperties.create(matrix, lx, this.listY(), lw, this.listH()).round(6.0f).thickness(1.0f)
                    .outlineColor(new Color(255, 255, 255, 30).getRGB()).color(new Color(255, 255, 255, 6).getRGB()).build());
            Rectangle.flushPending();
            this.nameFont.drawCenteredString(matrix, NeverloseMenu.tr("Модов пока нет", "No mods yet"), lx + lw / 2.0f, cy - 10.0f, new Color(200, 200, 200).getRGB());
            this.smallFont.drawCenteredString(matrix, NeverloseMenu.tr("Нажмите «Добавить» или перетащите .jar файлы сюда", "Press \"Add\" or drop .jar files here"),
                    lx + lw / 2.0f, cy + 4.0f, new Color(120, 120, 120).getRGB());
            return;
        }
        if (this.confirmDelete != null && now - this.confirmDeleteTime > CONFIRM_MS) {
            this.confirmDelete = null;
        }
        int rows = Math.min(this.visibleRows(), this.entries.size() - this.scroll);
        for (int v = 0; v < rows; ++v) {
            ModEntry entry = this.entries.get(this.scroll + v);
            float y = this.rowY(v);
            boolean rowHovered = NeverloseMenu.isMouseOver(mouseX, mouseY, lx, y, lw, ROW_H);
            float rowHover = this.animate("row" + entry.path, rowHovered, lerp);
            boolean active = entry.enabled && !entry.pendingDelete;
            int base = active ? 24 : 18;
            int bg = NeverloseMenu.interpolateColor(new Color(base, base, base + 1, 230).getRGB(), new Color(34, 34, 36, 240).getRGB(), rowHover);
            rectangle.render(ShapeProperties.create(matrix, lx, y, lw, ROW_H).round(5.0f).softness(0.5f).color(bg).build());
            Rectangle.flushPending();

            float rightControlsW = entry.self ? 0.0f : (entry.pendingDelete ? 74.0f : 68.0f);
            String status = this.statusText(entry);
            int statusColor = this.statusColor(entry);
            float statusW = this.smallFont.getStringWidth(status);
            float statusX = lx + lw - 10.0f - rightControlsW - statusW;
            float textMax = statusX - (lx + 10.0f) - 10.0f;

            int nameColor = active ? -1 : new Color(140, 140, 140).getRGB();
            this.nameFont.drawString(matrix, this.trim(this.nameFont, entry.name, textMax), lx + 10.0f, y + 6.0f, nameColor);
            StringBuilder meta = new StringBuilder();
            if (entry.version != null) {
                meta.append(entry.version).append(" · ");
            }
            meta.append(entry.fileName).append(" · ").append(ModsScreen.formatSize(entry.size));
            this.smallFont.drawString(matrix, this.trim(this.smallFont, meta.toString(), textMax), lx + 10.0f, y + 18.0f, new Color(110, 110, 110).getRGB());
            this.smallFont.drawString(matrix, status, statusX, y + 12.0f, statusColor);

            if (entry.self) {
                continue;
            }
            if (entry.pendingDelete) {
                this.drawButton(matrix, "restore" + entry.path, this.restoreButton(y), NeverloseMenu.tr("Вернуть", "Restore"), mouseX, mouseY, lerp, false);
                continue;
            }
            this.drawSwitch(matrix, entry, this.toggleSwitch(y), mouseX, mouseY, lerp);
            float[] del = this.deleteButton(y);
            boolean confirming = entry.path.equals(this.confirmDelete);
            boolean delHovered = NeverloseMenu.isMouseOver(mouseX, mouseY, del[0], del[1], del[2], del[3]);
            float delHover = this.animate("del" + entry.path, delHovered || confirming, lerp);
            int delBg = NeverloseMenu.interpolateColor(new Color(36, 36, 38, 235).getRGB(), new Color(170, 60, 60, 240).getRGB(), delHover);
            rectangle.render(ShapeProperties.create(matrix, del[0], del[1], del[2], del[3]).round(4.0f).softness(0.5f).color(delBg).build());
            Rectangle.flushPending();
            int delText = NeverloseMenu.interpolateColor(new Color(200, 110, 110).getRGB(), -1, delHover);
            this.smallFont.drawCenteredString(matrix, confirming ? "?" : "x", del[0] + del[2] / 2.0f, del[1] + 5.0f, delText);
        }
        // Полоса прокрутки справа от списка.
        if (this.maxScroll() > 0) {
            float trackH = this.listH();
            float thumbH = Math.max(16.0f, trackH * (float)this.visibleRows() / (float)this.entries.size());
            float thumbY = this.listY() + (trackH - thumbH) * (float)this.scroll / (float)this.maxScroll();
            rectangle.render(ShapeProperties.create(matrix, lx + lw + 4.0f, thumbY, 2.0, thumbH).round(1.0f).color(new Color(255, 255, 255, 60).getRGB()).build());
            Rectangle.flushPending();
        }
    }

    private String statusText(ModEntry entry) {
        if (entry.self) {
            return NeverloseMenu.tr("Клиент", "Client");
        }
        if (entry.pendingDelete) {
            return NeverloseMenu.tr("Удалится при выходе", "Removed on exit");
        }
        if (entry.enabled) {
            return entry.loaded ? NeverloseMenu.tr("Работает", "Active") : NeverloseMenu.tr("После перезапуска", "After restart");
        }
        return entry.loaded ? NeverloseMenu.tr("Выкл. после перезапуска", "Off after restart") : NeverloseMenu.tr("Выключен", "Disabled");
    }

    private int statusColor(ModEntry entry) {
        if (entry.self) {
            return new Color(150, 150, 150).getRGB();
        }
        if (entry.pendingDelete) {
            return new Color(224, 122, 122).getRGB();
        }
        if (entry.enabled == entry.loaded) {
            return entry.enabled ? new Color(143, 209, 143).getRGB() : new Color(120, 120, 120).getRGB();
        }
        return new Color(232, 198, 106).getRGB();
    }

    private void drawSwitch(MatrixStack matrix, ModEntry entry, float[] b, int mouseX, int mouseY, float lerp) {
        float target = entry.enabled ? 1.0f : 0.0f;
        // Ключ без ".disabled": после переименования файла переключатель плавно доезжает, а не прыгает.
        String key = entry.fileName.endsWith(".disabled") ? entry.fileName.substring(0, entry.fileName.length() - 9) : entry.fileName;
        float anim = this.switchAnim.getOrDefault(key, target);
        anim = MathHelper.lerp(Math.min(1.0f, lerp * 1.5f), anim, target);
        this.switchAnim.put(key, anim);
        boolean hovered = NeverloseMenu.isMouseOver(mouseX, mouseY, b[0], b[1], b[2], b[3]);
        int track = NeverloseMenu.interpolateColor(new Color(50, 50, 53, 240).getRGB(), new Color(225, 225, 225, 245).getRGB(), anim);
        if (hovered) {
            track = NeverloseMenu.interpolateColor(track, -1, 0.15f);
        }
        rectangle.render(ShapeProperties.create(matrix, b[0], b[1], b[2], b[3]).round(b[3] / 2.0f).softness(0.5f).color(track).build());
        float knob = b[3] - 4.0f;
        float knobX = b[0] + 2.0f + (b[2] - knob - 4.0f) * anim;
        int knobColor = NeverloseMenu.interpolateColor(new Color(150, 150, 150).getRGB(), new Color(20, 20, 22).getRGB(), anim);
        rectangle.render(ShapeProperties.create(matrix, knobX, b[1] + 2.0f, knob, knob).round(knob / 2.0f).softness(0.5f).color(knobColor).build());
        Rectangle.flushPending();
    }

    private void drawButton(MatrixStack matrix, String key, float[] b, String label, int mouseX, int mouseY, float lerp, boolean accent) {
        boolean hovered = NeverloseMenu.isMouseOver(mouseX, mouseY, b[0], b[1], b[2], b[3]);
        float t = this.animate(key, hovered, lerp);
        int top;
        int bottom;
        int text;
        if (accent) {
            top = NeverloseMenu.interpolateColor(new Color(225, 225, 225, 240).getRGB(), new Color(255, 255, 255, 255).getRGB(), t);
            bottom = NeverloseMenu.interpolateColor(new Color(195, 195, 195, 240).getRGB(), new Color(230, 230, 230, 255).getRGB(), t);
            text = new Color(16, 16, 18).getRGB();
        } else {
            top = NeverloseMenu.interpolateColor(new Color(40, 40, 42, 235).getRGB(), new Color(60, 60, 63, 245).getRGB(), t);
            bottom = NeverloseMenu.interpolateColor(new Color(27, 27, 29, 235).getRGB(), new Color(42, 42, 45, 245).getRGB(), t);
            text = NeverloseMenu.interpolateColor(new Color(215, 215, 215).getRGB(), -1, t);
        }
        float x = NeverloseMenu.snap(b[0]);
        float y = NeverloseMenu.snap(b[1]);
        rectangle.render(ShapeProperties.create(matrix, x - 1.0f, y - 1.0f, b[2] + 2.0f, b[3] + 2.0f).round(5.5f).softness(0.5f).color(new Color(255, 255, 255, 22).getRGB()).build());
        rectangle.render(ShapeProperties.create(matrix, x, y, b[2], b[3]).round(4.5f).softness(0.5f).color(top, top, bottom, bottom).build());
        Rectangle.flushPending();
        this.smallFont.drawCenteredString(matrix, label, x + b[2] / 2.0f, NeverloseMenu.snap(y + b[3] / 2.0f - 3.0f), text);
    }

    private float animate(String key, boolean on, float lerp) {
        float value = MathHelper.lerp(lerp, this.hover.getOrDefault(key, 0.0f), on ? 1.0f : 0.0f);
        this.hover.put(key, value);
        return value;
    }

    private String trim(FontRenderer font, String text, float maxWidth) {
        if (font.getStringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        String cut = font.trimToWidth(text, (int)Math.max(0.0f, maxWidth - font.getStringWidth(ellipsis)));
        return cut + ellipsis;
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.0f KB", (double)bytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", (double)bytes / 1024.0 / 1024.0);
    }

    /* ---------------------------- ввод ---------------------------- */

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click wiosClick, boolean wiosDbl) {
        double mouseX = wiosClick.x();
        double mouseY = wiosClick.y();
        int button = wiosClick.button();
        if (button != 0) {
            return super.mouseClicked(wiosClick, wiosDbl);
        }
        float mX = (float)mouseX;
        float mY = (float)mouseY;
        float[] add = this.headerButton(0);
        if (NeverloseMenu.isMouseOver(mX, mY, add[0], add[1], add[2], add[3])) {
            this.openFilePicker();
            return true;
        }
        float[] folder = this.headerButton(1);
        if (NeverloseMenu.isMouseOver(mX, mY, folder[0], folder[1], folder[2], folder[3])) {
            this.openModsFolder();
            return true;
        }
        float[] done = this.doneButton();
        if (NeverloseMenu.isMouseOver(mX, mY, done[0], done[1], done[2], done[3])) {
            this.close();
            return true;
        }
        int rows = Math.min(this.visibleRows(), this.entries.size() - this.scroll);
        for (int v = 0; v < rows; ++v) {
            ModEntry entry = this.entries.get(this.scroll + v);
            float y = this.rowY(v);
            if (entry.self) continue;
            if (entry.pendingDelete) {
                float[] restore = this.restoreButton(y);
                if (NeverloseMenu.isMouseOver(mX, mY, restore[0], restore[1], restore[2], restore[3])) {
                    ModManager.cancelDelete(entry);
                    this.refresh();
                    return true;
                }
                continue;
            }
            float[] del = this.deleteButton(y);
            if (NeverloseMenu.isMouseOver(mX, mY, del[0], del[1], del[2], del[3])) {
                if (entry.path.equals(this.confirmDelete)) {
                    this.confirmDelete = null;
                    ModManager.Result result = ModManager.delete(entry);
                    if (result != null) {
                        this.showMessage(result.message(), result.error());
                    }
                    this.refresh();
                } else {
                    this.confirmDelete = entry.path;
                    this.confirmDeleteTime = System.currentTimeMillis();
                    this.showMessage(NeverloseMenu.tr("Нажмите ещё раз, чтобы удалить " + entry.name, "Click again to delete " + entry.name), false);
                }
                return true;
            }
            float[] sw = this.toggleSwitch(y);
            // Переключатель срабатывает и по клику на саму строку.
            if (NeverloseMenu.isMouseOver(mX, mY, sw[0], sw[1], sw[2], sw[3]) || NeverloseMenu.isMouseOver(mX, mY, this.listX(), y, this.listW(), ROW_H)) {
                ModManager.Result result = ModManager.setEnabled(entry, !entry.enabled);
                if (result != null) {
                    this.showMessage(result.message(), result.error());
                }
                this.refresh();
                return true;
            }
        }
        return super.mouseClicked(wiosClick, wiosDbl);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.scroll = MathHelper.clamp(this.scroll - (int)Math.signum(verticalAmount), 0, this.maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput wiosKey) {
        int keyCode = wiosKey.key();
        int scanCode = wiosKey.scancode();
        int modifiers = wiosKey.modifiers();
        if (keyCode == GLFW.GLFW_KEY_F5) {
            this.refresh();
            return true;
        }
        return super.keyPressed(wiosKey);
    }

    /** Файлы, перетащенные в окно игры. */
    @Override
    public void onFilesDropped(List<Path> paths) {
        this.installFiles(paths);
    }

    void installFiles(List<Path> paths) {
        List<Path> files = new ArrayList<>();
        for (Path path : paths) {
            if (Files.isRegularFile(path)) {
                files.add(path);
            }
        }
        if (files.isEmpty()) {
            this.showMessage(NeverloseMenu.tr("Перетащите файлы .jar, а не папку", "Drop .jar files, not a folder"), true);
            return;
        }
        this.showResults(ModManager.install(files));
        this.refresh();
    }

    private void openModsFolder() {
        try {
            Path dir = ModManager.modsDir();
            Files.createDirectories(dir);
            Util.getOperatingSystem().open(dir);
        } catch (Exception e) {
            this.showMessage(NeverloseMenu.tr("Не удалось открыть папку", "Could not open the folder"), true);
        }
    }

    /**
     * Системный диалог выбора файлов. Он модальный и блокирует свой поток,
     * поэтому открывается отдельно, а результат возвращается в поток игры.
     */
    private void openFilePicker() {
        if (this.pickerOpen) {
            return;
        }
        this.pickerOpen = true;
        String title = NeverloseMenu.tr("Выберите моды (.jar)", "Select mods (.jar)");
        String defaultPath = System.getProperty("user.home") + java.io.File.separator + "Downloads" + java.io.File.separator;
        Thread thread = new Thread(() -> {
            String selected = null;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(1);
                filters.put(stack.UTF8("*.jar"));
                filters.flip();
                selected = TinyFileDialogs.tinyfd_openFileDialog(title, defaultPath, filters, "Fabric mods (*.jar)", true);
            } catch (Throwable t) {
                String error = t.getMessage();
                mc.execute(() -> this.showMessage(NeverloseMenu.tr("Диалог выбора не открылся: ", "File dialog failed: ") + error, true));
            }
            String result = selected;
            mc.execute(() -> {
                this.pickerOpen = false;
                if (result == null || result.isBlank()) {
                    return;
                }
                List<Path> paths = new ArrayList<>();
                for (String part : result.split("\\|")) {
                    if (!part.isBlank()) {
                        paths.add(Path.of(part));
                    }
                }
                this.installFiles(paths);
            });
        }, "wios-mod-picker");
        thread.setDaemon(true);
        thread.start();
    }
}
