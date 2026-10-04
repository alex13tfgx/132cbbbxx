package code.wiosvisual.features.impl.player;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.events.block.BlockUpdateEvent;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.events.render.DrawEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import code.wiosvisual.utils.math.projection.Projection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Таймеры структур HolyWorld: ловит постройку трапки или пласта по расстановке
 * блоков, определяет тип по габаритам и рисует над центром оставшееся время.
 * Порт Structures из DELTA.
 */
public class Structures
extends Module {
    /** Блоки, из которых строятся отслеживаемые структуры. */
    private static final List<Block> ALLOWED_BLOCKS = Arrays.asList(Blocks.QUARTZ_BLOCK, Blocks.OAK_SIGN, Blocks.MOSSY_STONE_BRICKS, Blocks.PURPUR_PILLAR, Blocks.END_STONE_BRICKS, Blocks.NETHER_BRICKS, Blocks.BLACKSTONE, Blocks.PRISMARINE_BRICKS, Blocks.ICE, Blocks.NETHER_WART_BLOCK, Blocks.RESPAWN_ANCHOR, Blocks.NETHERITE_BLOCK, Blocks.WHITE_STAINED_GLASS, Blocks.BLACK_CONCRETE, Blocks.POLISHED_BASALT);

    private final BooleanSetting showIcon = new BooleanSetting("Иконка предмета", "Рисовать иконку предмета рядом со временем").setValue(true);

    private final List<Tracked> tracked = new ArrayList<Tracked>();
    /** Блоки прилетают из сетевого потока, а разбираются в тике — поэтому потокобезопасный набор. */
    private final Set<BlockPos> pending = ConcurrentHashMap.newKeySet();
    private final Rectangle rectangle = new Rectangle();
    private int settleTicks;

    public static Structures getInstance() {
        return Instance.get(Structures.class);
    }

    public Structures() {
        super("Structures", "Structures", ModuleCategory.PLAYER);
        this.setup(this.showIcon);
    }

    @Override
    public void deactivate() {
        this.tracked.clear();
        this.pending.clear();
        this.settleTicks = 0;
        super.deactivate();
    }

    @EventHandler
    public void onBlockUpdate(BlockUpdateEvent e) {
        if (e.state() == null || !ALLOWED_BLOCKS.contains(e.state().getBlock())) {
            return;
        }
        // Блоки структуры прилетают пачкой — ждём пару тиков и разбираем разом.
        this.pending.add(e.pos().toImmutable());
        this.settleTicks = 2;
    }

    @EventHandler
    public void onTick(TickEvent e) {
        long now = System.currentTimeMillis();
        this.tracked.removeIf(structure -> now - structure.placedAt >= structure.type.durationMs);
        if (this.settleTicks <= 0) {
            return;
        }
        if (--this.settleTicks != 0) {
            return;
        }
        // Снимок: пока идёт разбор, новые блоки могут добавляться в pending.
        Set<BlockPos> snapshot = new HashSet<BlockPos>(this.pending);
        this.pending.removeAll(snapshot);
        BlockBox box = Structures.encompass(snapshot);
        if (box != null) {
            for (Type type : Type.values()) {
                if (!type.matches(box, snapshot)) continue;
                this.tracked.add(new Tracked(type, box, now));
                break;
            }
        }
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        if (this.tracked.isEmpty() || Structures.mc.player == null) {
            return;
        }
        DrawContext context = e.getDrawContext();
        MatrixStack matrix = GuiCompat.stack(context);
        FontRenderer font = Fonts.getSize(12, Fonts.Type.SuisseIntlMedium);
        long now = System.currentTimeMillis();
        for (Tracked structure : this.tracked) {
            float remaining = Math.max(0.0f, (float)(structure.type.durationMs - (now - structure.placedAt)) / 1000.0f);
            String text = String.format(Locale.US, "%.1f", Float.valueOf(remaining));
            Vec3d center = structure.center();
            if (!Projection.isInFrontOfCamera(center)) continue;
            Vec3d screen = Projection.worldSpaceToScreenSpace(center);
            if (screen == null) continue;
            boolean icon = this.showIcon.isValue();
            float textWidth = font.getStringWidth(text);
            float iconWidth = icon ? 14.0f : 0.0f;
            float totalWidth = iconWidth + textWidth + 8.0f;
            float boxX = (float)screen.x - totalWidth / 2.0f;
            float boxY = (float)screen.y - 6.0f;
            this.rectangle.render(ShapeProperties.create(matrix, boxX, boxY, totalWidth, 13.0).round(3.5f).color(ColorAssist.replAlpha(0, 150)).build());
            Rectangle.flushPending();
            float cursor = boxX + 4.0f;
            if (icon) {
                Render2D.defaultDrawStack(context, structure.type.item.getDefaultStack(), cursor, boxY + 2.0f, false, false, 0.55f);
                cursor += iconWidth;
            }
            font.drawString(matrix, text, cursor, font.getCenteredY(boxY, 13.0f, text), ColorAssist.getText());
        }
    }

    /** Общий бокс по всем поставленным позициям. */
    private static BlockBox encompass(Set<BlockPos> positions) {
        if (positions.isEmpty()) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private record Tracked(Type type, BlockBox box, long placedAt) {
        Vec3d center() {
            return new Vec3d((double)(this.box.getMinX() + this.box.getMaxX()) / 2.0 + 0.5, (double)(this.box.getMinY() + this.box.getMaxY()) / 2.0 + 0.5, (double)(this.box.getMinZ() + this.box.getMaxZ()) / 2.0 + 0.5);
        }
    }

    /** Тип структуры: габариты, полая ли она, время жизни и предмет-иконка. */
    private enum Type {
        DRAGON_TRAPKA("Драконья трапка", new int[][]{{7, 7, 7}, {7, 7, 6}}, true, 30000L, Items.NETHERITE_SCRAP),
        TRAPKA("Трапка", new int[][]{{5, 5, 5}, {5, 5, 4}, {5, 6, 5}, {5, 6, 4}}, true, 15000L, Items.NETHERITE_SCRAP),
        DRAGON_PLAST("Драконий пласт", new int[][]{{7, 7, 2}, {7, 7, 1}}, true, 20000L, Items.SWEET_BERRIES),
        PLAST("Пласт", new int[][]{{5, 5, 2}, {5, 5, 1}}, true, 20000L, Items.SWEET_BERRIES),
        GARMOSHKA("Пласт", new int[][]{{5, 5, 5}, {5, 6, 5}}, false, 20000L, Items.SWEET_BERRIES);

        final String displayName;
        final int[][] dimensions;
        final boolean hollow;
        final long durationMs;
        final Item item;

        private Type(String displayName, int[][] dimensions, boolean hollow, long durationMs, Item item) {
            this.displayName = displayName;
            this.dimensions = dimensions;
            this.hollow = hollow;
            this.durationMs = durationMs;
            this.item = item;
        }

        /** Совпадают ли габариты бокса с этим типом и сходится ли «полость». */
        boolean matches(BlockBox box, Set<BlockPos> positions) {
            int[] size = new int[]{box.getBlockCountX(), box.getBlockCountY(), box.getBlockCountZ()};
            Arrays.sort(size);
            for (int[] dimension : this.dimensions) {
                int[] sorted = (int[])dimension.clone();
                Arrays.sort(sorted);
                if (!Arrays.equals(size, sorted)) continue;
                boolean empty = positions.stream().noneMatch(pos -> pos.getX() > box.getMinX() && pos.getX() < box.getMaxX() && pos.getY() > box.getMinY() && pos.getY() < box.getMaxY() && pos.getZ() > box.getMinZ() && pos.getZ() < box.getMaxZ());
                return this.hollow == empty;
            }
            return false;
        }
    }
}
