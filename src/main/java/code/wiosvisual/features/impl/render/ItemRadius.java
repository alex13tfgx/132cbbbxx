package code.wiosvisual.features.impl.render;

import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.MultiSelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.geometry.Render3D;
import code.wiosvisual.utils.math.calc.CalcVector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Радиус спец-предметов HolyWorld: пока предмет в руке, под ногами рисуется зона
 * его действия. Порт ItemRadius из Pouch на современный рендер-пайплайн клиента.
 */
public class ItemRadius
extends Module {
    private static final String DEZKA = "Дезка";
    private static final String YAVKA = "Явка";
    private static final String FIRE_CHARGE = "Огненный заряд";
    private static final String AURA = "Божья аура";
    private static final String TRAPKA = "Трапка";
    private static final String PLAST = "Пласт";

    private static final int SEGMENTS = 72;
    private static final float FADE_SPEED = 0.5f;

    private final MultiSelectSetting items = new MultiSelectSetting("Предметы", "Для каких предметов рисовать радиус").value(DEZKA, YAVKA, FIRE_CHARGE, AURA, TRAPKA, PLAST).selected(DEZKA, YAVKA, FIRE_CHARGE, AURA);
    private final SliderSettings lineWidth = new SliderSettings("Толщина линий", "Толщина обводки").setValue(2.0f).range(0.5f, 5.0f).step(0.1f);
    private final SliderSettings opacity = new SliderSettings("Прозрачность", "Прозрачность заливки").setValue(1.0f).range(0.1f, 1.0f).step(0.05f);

    private int fillColor = 0x55005500;
    private int outlineColor = -16755456;
    private float fade;
    private boolean lastPlayersInRadius;

    public ItemRadius() {
        super("ItemRadius", "Item Radius", ModuleCategory.RENDER);
        this.setup(this.items, this.lineWidth, this.opacity);
    }

    public static ItemRadius getInstance() {
        return Instance.get(ItemRadius.class);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (ItemRadius.mc.player == null || ItemRadius.mc.world == null) {
            return;
        }
        ItemStack mainHand = ItemRadius.mc.player.getMainHandStack();
        ItemStack offHand = ItemRadius.mc.player.getOffHandStack();
        Vec3d position = CalcVector.lerpPosition((Entity)ItemRadius.mc.player);
        Vec3d center = position.add(0.0, -1.4, 0.0);
        float partialTicks = event.getPartialTicks();
        if (this.items.isSelected(DEZKA) && this.holding(mainHand, offHand, "ender_eye")) {
            this.animate(this.playersInRadius(center, 10.0), 0x55005500, -16755456, 0x5500AA00, -16733696, partialTicks);
            this.drawCircle(position, 10.0);
            return;
        }
        if (this.items.isSelected(YAVKA) && this.holding(mainHand, offHand, "sugar")) {
            this.animate(this.playersInRadius(center, 10.0), 0x55999999, -6710887, 0x55FFFFFF, -1, partialTicks);
            this.drawCircle(position, 10.0);
            return;
        }
        if (this.items.isSelected(FIRE_CHARGE) && this.holding(mainHand, offHand, "fire_charge")) {
            this.animate(this.playersInRadius(center, 10.0), 0x55550000, -11206656, 0x55AA0000, -5636096, partialTicks);
            this.drawCircle(position, 10.0);
            return;
        }
        if (this.items.isSelected(AURA) && this.holding(mainHand, offHand, "phantom_membrane")) {
            this.animate(this.playersInRadius(center, 2.0), 0x55009999, -16737895, 0x5500FFFF, -16711681, partialTicks);
            this.drawCircle(position, 2.0);
            return;
        }
        if (this.items.isSelected(TRAPKA) && this.holding(mainHand, offHand, "netherite_scrap")) {
            this.drawCube(position, 1435190547, -7650029);
            return;
        }
        if (this.items.isSelected(PLAST) && this.holding(mainHand, offHand, "dried_kelp")) {
            this.drawPlane(position, partialTicks, 0x55333333, -13421773);
        }
    }

    @Override
    public void deactivate() {
        this.fade = 0.0f;
        this.lastPlayersInRadius = false;
    }

    private boolean holding(ItemStack mainHand, ItemStack offHand, String path) {
        return ItemRadius.isItem(mainHand, path) || ItemRadius.isItem(offHand, path);
    }

    private static boolean isItem(ItemStack stack, String path) {
        if (stack.isEmpty()) {
            return false;
        }
        Identifier id = Registries.ITEM.getId(stack.getItem());
        return id != null && id.getPath().equals(path);
    }

    private boolean playersInRadius(Vec3d center, double radius) {
        Box box = new Box(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius);
        for (PlayerEntity entity : ItemRadius.mc.world.getEntitiesByClass(PlayerEntity.class, box, player -> true)) {
            if (entity == ItemRadius.mc.player || !(new net.minecraft.util.math.Vec3d(entity.getX(), entity.getY(), entity.getRandomBodyY()).distanceTo(center) <= radius)) continue;
            return true;
        }
        return false;
    }

    private void animate(boolean playersInRadius, int baseFill, int baseOutline, int activeFill, int activeOutline, float partialTicks) {
        if (playersInRadius != this.lastPlayersInRadius) {
            this.fade = 0.0f;
            this.lastPlayersInRadius = playersInRadius;
        }
        int targetFill = playersInRadius ? activeFill : baseFill;
        int targetOutline = playersInRadius ? activeOutline : baseOutline;
        this.fade = Math.min(this.fade + partialTicks / FADE_SPEED, 1.0f);
        this.fillColor = ItemRadius.interpolate(this.fillColor, targetFill, this.fade);
        this.outlineColor = ItemRadius.interpolate(this.outlineColor, targetOutline, this.fade);
    }

    private static int interpolate(int start, int end, float delta) {
        int startA = start >> 24 & 0xFF;
        int startR = start >> 16 & 0xFF;
        int startG = start >> 8 & 0xFF;
        int startB = start & 0xFF;
        int a = (int)((float)startA + (float)((end >> 24 & 0xFF) - startA) * delta);
        int r = (int)((float)startR + (float)((end >> 16 & 0xFF) - startR) * delta);
        int g = (int)((float)startG + (float)((end >> 8 & 0xFF) - startG) * delta);
        int b = (int)((float)startB + (float)((end & 0xFF) - startB) * delta);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private void drawCircle(Vec3d position, double radius) {
        int fill = ColorAssist.multAlpha(this.fillColor, this.opacity.getValue());
        float width = this.lineWidth.getValue();
        Vec3d center = new Vec3d(position.x, position.y + 0.02, position.z);
        for (int i = 0; i < SEGMENTS; ++i) {
            Vec3d first = ItemRadius.onCircle(center, radius, i);
            Vec3d second = ItemRadius.onCircle(center, radius, i + 1);
            Render3D.drawQuad(center, first, second, center, fill, true);
            Render3D.drawLine(first, second, this.outlineColor, width, true);
        }
    }

    private static Vec3d onCircle(Vec3d center, double radius, int step) {
        double angle = Math.PI * 2 * (double)step / (double)SEGMENTS;
        return new Vec3d(center.x + Math.cos(angle) * radius, center.y, center.z - Math.sin(angle) * radius);
    }

    private void drawCube(Vec3d position, int fill, int outline) {
        double half = 2.0;
        double x = Math.floor(position.x) + 0.5;
        double y = Math.floor(position.y) + 0.5 + 1.625;
        double z = Math.floor(position.z) + 0.5;
        Box box = new Box(x - half, y - half, z - half, x + half, y + half, z + half);
        Render3D.drawBox(box, ColorAssist.multAlpha(fill, this.opacity.getValue()), this.lineWidth.getValue(), false, true, true);
        Render3D.drawBox(box, outline, this.lineWidth.getValue(), true, false, true);
    }

    private void drawPlane(Vec3d position, float partialTicks, int fill, int outline) {
        PlayerEntity player = ItemRadius.mc.player;
        float pitch = player.getPitch(partialTicks);
        float yaw = player.getYaw(partialTicks);
        boolean lookingDown = pitch > 45.0f;
        boolean lookingUp = pitch < -45.0f;
        boolean horizontal = !lookingDown && !lookingUp;
        double halfThickness = 0.75;
        Vec3d start = position.add(0.0, player.getStandingEyeHeight(), 0.0);
        Vec3d look = player.getRotationVec(partialTicks);
        Vec3d end = start.add(look.x * 4.0, look.y * 4.0, look.z * 4.0);
        HitResult hit = ItemRadius.mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)player));
        Vec3d planePos = hit.getType() == HitResult.Type.BLOCK && hit.getPos().distanceTo(start) <= 4.0 ? ItemRadius.snapToHit(hit.getPos(), lookingDown, lookingUp, halfThickness) : ItemRadius.snapToLook(start, look, lookingDown, lookingUp, halfThickness);
        double sizeX = 2.0;
        double sizeY = horizontal ? 2.0 : halfThickness;
        double sizeZ = horizontal ? halfThickness : 2.0;
        double rotation = horizontal ? Math.toRadians(-yaw) : 0.0;
        Vec3d[] corners = new Vec3d[8];
        int index = 0;
        for (int cx = -1; cx <= 1; cx += 2) {
            for (int cy = -1; cy <= 1; cy += 2) {
                for (int cz = -1; cz <= 1; cz += 2) {
                    double localX = (double)cx * sizeX;
                    double localY = (double)cy * sizeY;
                    double localZ = (double)cz * sizeZ;
                    double rotatedX = localX * Math.cos(rotation) + localZ * Math.sin(rotation);
                    double rotatedZ = -localX * Math.sin(rotation) + localZ * Math.cos(rotation);
                    corners[index++] = planePos.add(rotatedX, localY, rotatedZ);
                }
            }
        }
        int fillColor = ColorAssist.multAlpha(fill, this.opacity.getValue());
        float width = this.lineWidth.getValue();
        int[][] faces = new int[][]{{0, 1, 3, 2}, {4, 5, 7, 6}, {0, 1, 5, 4}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 3, 7, 5}};
        for (int[] face : faces) {
            Render3D.drawQuad(corners[face[0]], corners[face[1]], corners[face[2]], corners[face[3]], fillColor, true);
        }
        int[][] edges = new int[][]{{0, 1}, {1, 3}, {3, 2}, {2, 0}, {4, 5}, {5, 7}, {7, 6}, {6, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] edge : edges) {
            Render3D.drawLine(corners[edge[0]], corners[edge[1]], outline, width, true);
        }
    }

    private static Vec3d snapToHit(Vec3d hitPos, boolean lookingDown, boolean lookingUp, double halfThickness) {
        double y = lookingDown ? Math.floor(hitPos.y + 1.0) - 1.8 + halfThickness : (lookingUp ? Math.floor(hitPos.y) - halfThickness + 1.6 : Math.floor(hitPos.y) + 0.5 + 1.6);
        return new Vec3d(Math.floor(hitPos.x) + 0.5, y, Math.floor(hitPos.z) + 0.5);
    }

    private static Vec3d snapToLook(Vec3d start, Vec3d look, boolean lookingDown, boolean lookingUp, double halfThickness) {
        Vec3d target = start.add(look.x * 4.0, look.y * 4.0, look.z * 4.0);
        double y = Math.floor(target.y) + (lookingDown ? -1.8 + halfThickness : (lookingUp ? -halfThickness + 1.6 : 2.1));
        return new Vec3d(Math.floor(target.x) + 0.5, y, Math.floor(target.z) + 0.5);
    }
}
