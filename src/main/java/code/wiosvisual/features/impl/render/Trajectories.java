/*
 * Trajectories — projectile trajectory prediction with a constant-screen-width
 * gradient ribbon, landing marker and a 2D icon/time label. Ported from Nightix,
 * rewritten for the 1.21.4 render system (intermediary mappings) reusing BigBrr's
 * Render3D / Projection / Fonts / Render2D helpers.
 */
package code.wiosvisual.features.impl.render;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.utils.display.render.pipeline.WiosRender;
import code.wiosvisual.events.render.DrawEvent;
import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.MultiSelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.geometry.Render3D;
import code.wiosvisual.utils.features.aura.utils.RaycastAngle;
import code.wiosvisual.utils.math.projection.Projection;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import com.mojang.blaze3d.vertex.VertexFormat;

public class Trajectories
extends Module {
    private static final int MAX_STEPS = 200;
    private static final int HELD_KEY = 0;
    private static final String PEARL = "Жемчуг эндера";
    private static final String ARROW = "Стрелы";
    private static final String TRIDENT = "Трезубец";

    public final MultiSelectSetting items = new MultiSelectSetting("Предметы", "Что показывать").value(PEARL, ARROW, TRIDENT).selected(PEARL, ARROW, TRIDENT);
    public final BooleanSetting fromHand = new BooleanSetting("Из рук", "Предсказывать из рук").setValue(true);
    public final BooleanSetting flying = new BooleanSetting("Летящие снаряды", "Показывать у летящих").setValue(true);
    public final BooleanSetting showLine = new BooleanSetting("Линия траектории", "Рисовать ленту").setValue(true);
    public final BooleanSetting glow = new BooleanSetting("Свечение линии", "Аддитивный ореол").setValue(false).visible(() -> this.showLine.isValue());
    public final BooleanSetting showIcon = new BooleanSetting("Иконка предмета", "Иконка в точке падения").setValue(true);
    public final BooleanSetting showTime = new BooleanSetting("Время до падения", "Время в секундах").setValue(true);
    public final SliderSettings lineWidth = new SliderSettings("Толщина линии", "Экранная толщина").setValue(2.0f).range(1.0f, 5.0f).step(0.5f).visible(() -> this.showLine.isValue());
    public final SliderSettings indicatorSize = new SliderSettings("Размер метки", "Радиус метки").setValue(0.5f).range(0.1f, 2.0f).step(0.1f);

    private final Map<Integer, SmoothedState> smoothed = new LinkedHashMap<Integer, SmoothedState>();
    private final List<SmoothedState> renderStates = new ArrayList<SmoothedState>();
    private long lastOverlayNs;
    private final code.wiosvisual.utils.display.render.shape.implement.Rectangle pill = new code.wiosvisual.utils.display.render.shape.implement.Rectangle();
    private long lastFrameNs = 0L;

    public Trajectories() {
        super("Trajectories", "Trajectories", ModuleCategory.RENDER);
        this.setup(this.items, this.fromHand, this.flying, this.showLine, this.glow, this.showIcon, this.showTime, this.lineWidth, this.indicatorSize);
    }

    @Override
    public void deactivate() {
        this.smoothed.clear();
        this.renderStates.clear();
        this.lastFrameNs = 0L;
        super.deactivate();
    }

    // ───────────────────────────── world render ─────────────────────────────

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (Trajectories.mc.player == null || Trajectories.mc.world == null) {
            return;
        }
        long now = System.nanoTime();
        float dt = this.lastFrameNs == 0L ? 0.05f : Math.min(0.1f, (float)(now - this.lastFrameNs) / 1.0E9f);
        this.lastFrameNs = now;

        this.updateSmoothedPredictions(dt);
        if (this.renderStates.isEmpty()) {
            return;
        }

        Vec3d cam = Trajectories.mc.getEntityRenderDispatcher().camera.pos;
        MatrixStack.Entry entry = e.getStack().peek();

        List<RibbonData> ribbons = new ArrayList<RibbonData>();
        if (this.showLine.isValue()) {
            for (SmoothedState state : this.renderStates) {
                RibbonData data = this.buildRibbon(state, cam);
                if (data != null) {
                    ribbons.add(data);
                }
            }
        }

        // additive glow pass
        if (this.showLine.isValue() && this.glow.isValue() && !ribbons.isEmpty()) {
            BufferBuilder buf = Trajectories.beginQuads();
            for (RibbonData data : ribbons) {
                Trajectories.emitGlowRibbon(buf, entry, data);
            }
            Trajectories.endQuads(buf, true);
        }

        // translucent pass: ribbon + marker fill
        BufferBuilder buf = Trajectories.beginQuads();
        if (this.showLine.isValue()) {
            for (RibbonData data : ribbons) {
                Trajectories.emitRibbon(buf, entry, data);
            }
        }
        for (SmoothedState state : this.renderStates) {
            this.drawLandingMarkerFill(buf, entry, state);
        }
        Trajectories.endQuads(buf, false);

        // marker cross via proven line helper (absolute coords)
        for (SmoothedState state : this.renderStates) {
            this.drawLandingMarkerCross(state);
        }
    }

    // ───────────────────────────── 2D overlay ─────────────────────────────

    @EventHandler
    public void onDraw(DrawEvent e) {
        if (Trajectories.mc.player == null || this.renderStates.isEmpty() || e.getDrawContext() == null) {
            return;
        }
        if (!this.showIcon.isValue() && !this.showTime.isValue()) {
            return;
        }
        // Метка рисуется строго один раз за кадр: если событие отрисовки приходит повторно, лишние вызовы пропускаем.
        long nowNs = System.nanoTime();
        if (nowNs - this.lastOverlayNs < 1_500_000L) {
            return;
        }
        this.lastOverlayNs = nowNs;
        SmoothedState only = this.renderStates.isEmpty() ? null : this.renderStates.get(0);
        for (SmoothedState state : java.util.Collections.singletonList(only)) {
            if (state == null) {
                continue;
            }
            Vec3d anchor = state.impact.add(0.0, 0.25, 0.0);
            if (!Projection.canSee(anchor)) {
                continue;
            }
            Vec3d screen = Projection.worldSpaceToScreenSpace(anchor);
            float centerX = (float)screen.x;
            float centerY = (float)screen.y;
            float iconSize = 10.0f;
            float boxSize = iconSize + 4.0f;

            if (this.showIcon.isValue() && !state.icon.isEmpty()) {
                Render2D.defaultDrawStack(e.getDrawContext(), state.icon, centerX - iconSize / 2.0f, centerY - iconSize / 2.0f, false, false, iconSize / 16.0f);
            }
            if (this.showTime.isValue()) {
                // Время до падения: тёмная «таблетка» с полностью скруглёнными краями и чётким полужирным текстом.
                FontRenderer font = Fonts.getSize(12, Fonts.Type.SuisseIntlSemiBold);
                float seconds = state.ticks / 20.0f;
                String text = seconds >= 10.0f ? Math.round(seconds) + " сек" : seconds >= 1.0f ? Math.round(seconds) + " сек" : String.format(Locale.ROOT, "%.1f сек", seconds);
                float textWidth = font.getStringWidth(text);
                float pillH = 13.0f;
                float pillW = textWidth + 12.0f;
                float pillX = Math.round(centerX - pillW / 2.0f);
                float pillY = Math.round(centerY + boxSize / 2.0f + 2.0f);
                MatrixStack matrix = GuiCompat.stack(e.getDrawContext());
                this.pill.render(ShapeProperties.create(matrix, pillX, pillY, pillW, pillH).round(pillH / 2.0f).color(0xD9101012).build());
                Rectangle.flushPending();
                font.drawString(matrix, text, Math.round(pillX + 6.0f), font.getCenteredY(pillY, pillH), 0xFFFFFFFF);
            }
        }
    }

    // ───────────────────────────── prediction ─────────────────────────────

    private void updateSmoothedPredictions(float dt) {
        for (SmoothedState state : this.smoothed.values()) {
            state.active = false;
        }
        if (this.fromHand.isValue()) {
            this.collectFromHand(dt);
        }
        if (this.flying.isValue()) {
            this.collectFlying(dt);
        }
        this.smoothed.entrySet().removeIf(entry -> !entry.getValue().active);
        // Показываем только одну метку: самый свежий летящий снаряд, а если ничего не летит, предмет в руке.
        Integer winner = null;
        for (Integer key : this.smoothed.keySet()) {
            if (key != HELD_KEY && (winner == null || key > winner)) {
                winner = key;
            }
        }
        if (winner == null && this.smoothed.containsKey(HELD_KEY)) {
            winner = HELD_KEY;
        }
        this.renderStates.clear();
        if (winner != null) {
            this.renderStates.add(this.smoothed.get(winner));
        }
    }

    private void pushRaw(int key, Prediction raw, float dt) {
        if (raw == null) {
            return;
        }
        SmoothedState state = this.smoothed.get(key);
        if (state == null) {
            state = SmoothedState.from(raw);
            this.smoothed.put(key, state);
        } else {
            state.blendToward(raw, dt, 100.0f);
        }
        state.active = true;
    }

    private void collectFromHand(float dt) {
        ItemStack main = Trajectories.mc.player.getMainHandStack();
        ItemStack off = Trajectories.mc.player.getOffHandStack();

        if (this.items.isSelected(PEARL)) {
            if (main.isOf(Items.ENDER_PEARL)) {
                this.pushRaw(HELD_KEY, this.simulateThrow(new ItemStack(Items.ENDER_PEARL), 1.5, 0.03, false), dt);
            } else if (off.isOf(Items.ENDER_PEARL)) {
                this.pushRaw(HELD_KEY, this.simulateThrow(new ItemStack(Items.ENDER_PEARL), 1.5, 0.03, true), dt);
            }
        }
        if (this.items.isSelected(ARROW) && Trajectories.mc.player.isUsingItem() && Trajectories.mc.player.getActiveItem().isOf(Items.BOW)) {
            float pull = Trajectories.bowPull(Trajectories.mc.player.getItemUseTimeLeft());
            if (pull >= 0.1f) {
                boolean offHand = Trajectories.mc.player.getActiveHand() == Hand.OFF_HAND;
                this.pushRaw(HELD_KEY, this.simulateThrow(new ItemStack(Items.ARROW), pull * 3.0, 0.05, offHand), dt);
            }
        } else if (this.items.isSelected(ARROW)) {
            ItemStack crossbow = null;
            boolean crossbowOffHand = false;
            if (main.isOf(Items.CROSSBOW)) {
                crossbow = main;
            } else if (off.isOf(Items.CROSSBOW)) {
                crossbow = off;
                crossbowOffHand = true;
            }
            if (crossbow != null && Trajectories.isCrossbowCharged(crossbow)) {
                this.pushRaw(HELD_KEY, this.simulateThrow(this.getCrossbowProjectile(crossbow), 3.15, 0.05, crossbowOffHand), dt);
            }
        }
        if (this.items.isSelected(TRIDENT) && Trajectories.mc.player.isUsingItem() && Trajectories.mc.player.getActiveItem().isOf(Items.TRIDENT)) {
            int useTime = Trajectories.mc.player.getItemUseTimeLeft();
            if (useTime > 0) {
                float force = Math.max(0.1f, Math.min((float)useTime / 10.0f, 1.0f));
                boolean offHand = Trajectories.mc.player.getActiveHand() == Hand.OFF_HAND;
                this.pushRaw(HELD_KEY, this.simulateThrow(new ItemStack(Items.TRIDENT), 2.5 * (double)force, 0.05, offHand), dt);
            }
        }
    }

    private ItemStack getCrossbowProjectile(ItemStack crossbow) {
        ChargedProjectilesComponent charged = (ChargedProjectilesComponent)crossbow.get(DataComponentTypes.CHARGED_PROJECTILES);
        if (charged != null && !charged.getProjectiles().isEmpty()) {
            return charged.getProjectiles().getFirst();
        }
        return new ItemStack(Items.ARROW);
    }

    private void collectFlying(float dt) {
        for (Entity entity : Trajectories.mc.world.getEntities()) {
            if (entity.getVelocity().lengthSquared() < 4.0E-4) {
                continue;
            }
            double gravity;
            ItemStack icon;
            if (entity instanceof EnderPearlEntity) {
                if (!this.items.isSelected(PEARL)) continue;
                gravity = 0.03;
                icon = new ItemStack(Items.ENDER_PEARL);
            } else if (entity instanceof TridentEntity) {
                if (!this.items.isSelected(TRIDENT)) continue;
                if (entity.isOnGround()) continue;
                gravity = 0.05;
                icon = new ItemStack(Items.TRIDENT);
            } else if (entity instanceof ArrowEntity) {
                if (!this.items.isSelected(ARROW)) continue;
                if (entity.isOnGround()) continue;
                gravity = 0.05;
                icon = new ItemStack(Items.ARROW);
            } else {
                continue;
            }
            Vec3d pos = new net.minecraft.util.math.Vec3d(entity.getX(), entity.getY(), entity.getRandomBodyY());
            Vec3d vel = entity.getVelocity();
            this.pushRaw(entity.getId(), this.simulate(pos, vel, gravity, entity, icon), dt);
        }
    }

    private Prediction simulateThrow(ItemStack icon, double speed, double gravity, boolean offHand) {
        float yaw = Trajectories.mc.player.getYaw();
        float pitch = Trajectories.mc.player.getPitch();
        double yawR = Math.toRadians(yaw);
        double pitchR = Math.toRadians(pitch);
        double dx = -Math.sin(yawR) * Math.cos(pitchR);
        double dy = -Math.sin(pitchR);
        double dz = Math.cos(yawR) * Math.cos(pitchR);

        Vec3d dir = new Vec3d(dx, dy, dz).normalize();
        Vec3d velocity = dir.multiply(speed);
        Vec3d playerVel = Trajectories.mc.player.getVelocity();
        velocity = velocity.add(playerVel.x, Trajectories.mc.player.isOnGround() ? 0.0 : playerVel.y, playerVel.z);

        Vec3d start = Trajectories.mc.player.getEyePos().add(dir.multiply(0.2));
        return this.simulate(start, velocity, gravity, Trajectories.mc.player, icon);
    }

    private Prediction simulate(Vec3d pos, Vec3d vel, double gravity, Entity ignore, ItemStack icon) {
        List<Vec3d> points = new ArrayList<Vec3d>(128);
        points.add(pos);
        double step = 0.25;
        double drag = Math.pow(0.99, step);
        double stepGravity = gravity * step;

        for (int tick = 0; tick < MAX_STEPS; ++tick) {
            for (int sub = 0; sub < 4; ++sub) {
                Vec3d next = pos.add(vel.multiply(step));
                BlockHitResult blockHit = RaycastAngle.raycast(pos, next, RaycastContext.ShapeType.COLLIDER, ignore);
                boolean isBlock = blockHit.getType() == HitResult.Type.BLOCK;
                Vec3d segmentEnd = isBlock ? blockHit.getPos() : next;

                Vec3d entityImpact = null;
                Entity hitEntity = null;
                Box sweep = new Box(pos, segmentEnd).expand(0.25);
                for (Entity ent : Trajectories.mc.world.getOtherEntities(ignore, sweep, other -> other instanceof LivingEntity && other.isAlive() && !other.isSpectator())) {
                    if (!ent.getBoundingBox().expand(0.25).intersects(pos, segmentEnd)) continue;
                    hitEntity = ent;
                    entityImpact = segmentEnd;
                    break;
                }
                if (hitEntity != null) {
                    points.add(entityImpact);
                    return new Prediction(Trajectories.densify(points), entityImpact, tick + 1, hitEntity, icon);
                }
                if (isBlock) {
                    points.add(segmentEnd);
                    return new Prediction(Trajectories.densify(points), segmentEnd, tick + 1, null, icon);
                }
                points.add(next);
                pos = next;
                vel = vel.multiply(drag).add(0.0, -stepGravity, 0.0);
                if (pos.y < -128.0) {
                    return null;
                }
            }
        }
        return null;
    }

    private static List<Vec3d> densify(List<Vec3d> points) {
        if (points.size() < 2) {
            return points;
        }
        List<Vec3d> out = new ArrayList<Vec3d>(points.size() * 2);
        out.add(points.getFirst());
        for (int i = 0; i + 1 < points.size(); ++i) {
            Vec3d a = points.get(i);
            Vec3d b = points.get(i + 1);
            out.add(Trajectories.lerp(a, b, 0.33));
            out.add(Trajectories.lerp(a, b, 0.66));
            out.add(b);
        }
        return out;
    }

    private static float bowPull(int useTicks) {
        float t = (float)useTicks / 20.0f;
        t = (t * t + t * 2.0f) / 3.0f;
        return Math.min(t, 1.0f);
    }

    private static boolean isCrossbowCharged(ItemStack stack) {
        ChargedProjectilesComponent charged = (ChargedProjectilesComponent)stack.get(DataComponentTypes.CHARGED_PROJECTILES);
        return charged != null && !charged.getProjectiles().isEmpty();
    }

    // ───────────────────────────── ribbon ─────────────────────────────

    private static final class RibbonData {
        Vec3d[] abs;
        Vec3d[] side;
        float[] alpha;
        float[] halfW;
        int cr;
        int cg;
        int cb;
    }

    private RibbonData buildRibbon(SmoothedState state, Vec3d cam) {
        List<Vec3d> points = state.points;
        int start = 0;
        while (points.size() - start > 2 && points.get(start).squaredDistanceTo(cam) < 0.16) {
            ++start;
        }
        int n = points.size() - start;
        if (n < 2) {
            return null;
        }
        boolean livingHit = state.hitEntity instanceof LivingEntity;
        int baseColor = livingHit ? ColorAssist.getColor(64, 255, 64, 255) : ColorAssist.fade(1);

        RibbonData data = new RibbonData();
        data.cr = baseColor >> 16 & 0xFF;
        data.cg = baseColor >> 8 & 0xFF;
        data.cb = baseColor & 0xFF;
        data.abs = new Vec3d[n];
        data.side = new Vec3d[n];
        data.alpha = new float[n];
        data.halfW = new float[n];

        float widthPx = this.lineWidth.getValue();
        Vec3d[] rel = new Vec3d[n];
        for (int i = 0; i < n; ++i) {
            data.abs[i] = points.get(start + i);
            rel[i] = data.abs[i].subtract(cam);
        }
        double dist = 0.0;
        Vec3d prevSide = new Vec3d(0.0, 1.0, 0.0);
        for (int i = 0; i < n; ++i) {
            Vec3d tangent = rel[Math.min(i + 1, n - 1)].subtract(rel[Math.max(i - 1, 0)]);
            Vec3d s = Trajectories.cross(tangent, rel[i]);
            s = s.lengthSquared() < 1.0E-8 ? prevSide : s.normalize();
            data.side[i] = prevSide = s;
            // constant screen thickness ~ proportional to distance from camera
            data.halfW[i] = (float)(0.0018 * (double)widthPx * rel[i].length());
            if (i > 0) {
                dist += points.get(start + i).distanceTo(points.get(start + i - 1));
            }
            data.alpha[i] = (float)MathHelper.clamp((dist - 0.1) / 0.55, 0.0, 1.0);
        }
        return data;
    }

    private static void emitRibbon(BufferBuilder buf, MatrixStack.Entry m, RibbonData d) {
        int n = d.abs.length;
        for (int i = 0; i < n - 1; ++i) {
            int a1 = (int)(d.alpha[i] * 255.0f);
            int a2 = (int)(d.alpha[i + 1] * 255.0f);
            if (a1 <= 0 && a2 <= 0) continue;
            float t1 = (float)i / (float)(n - 1);
            float t2 = (float)(i + 1) / (float)(n - 1);
            int r1 = Trajectories.lighten(d.cr, t1), g1 = Trajectories.lighten(d.cg, t1), b1 = Trajectories.lighten(d.cb, t1);
            int r2 = Trajectories.lighten(d.cr, t2), g2 = Trajectories.lighten(d.cg, t2), b2 = Trajectories.lighten(d.cb, t2);
            Vec3d c1 = d.abs[i], c2 = d.abs[i + 1];
            Vec3d o1 = d.side[i].multiply(d.halfW[i]), o2 = d.side[i + 1].multiply(d.halfW[i + 1]);
            Vec3d i1 = o1.multiply(0.7), i2 = o2.multiply(0.7);
            // dense core
            Trajectories.vtx(buf, m, c1.subtract(i1), r1, g1, b1, a1);
            Trajectories.vtx(buf, m, c2.subtract(i2), r2, g2, b2, a2);
            Trajectories.vtx(buf, m, c2.add(i2), r2, g2, b2, a2);
            Trajectories.vtx(buf, m, c1.add(i1), r1, g1, b1, a1);
            // edge A
            Trajectories.vtx(buf, m, c1.add(i1), r1, g1, b1, a1);
            Trajectories.vtx(buf, m, c2.add(i2), r2, g2, b2, a2);
            Trajectories.vtx(buf, m, c2.add(o2), r2, g2, b2, 0);
            Trajectories.vtx(buf, m, c1.add(o1), r1, g1, b1, 0);
            // edge B
            Trajectories.vtx(buf, m, c1.subtract(i1), r1, g1, b1, a1);
            Trajectories.vtx(buf, m, c2.subtract(i2), r2, g2, b2, a2);
            Trajectories.vtx(buf, m, c2.subtract(o2), r2, g2, b2, 0);
            Trajectories.vtx(buf, m, c1.subtract(o1), r1, g1, b1, 0);
        }
    }

    private static void emitGlowRibbon(BufferBuilder buf, MatrixStack.Entry m, RibbonData d) {
        int n = d.abs.length;
        for (int i = 0; i < n - 1; ++i) {
            int a1 = (int)(d.alpha[i] * 0.3f * 255.0f);
            int a2 = (int)(d.alpha[i + 1] * 0.3f * 255.0f);
            if (a1 <= 0 && a2 <= 0) continue;
            Vec3d c1 = d.abs[i], c2 = d.abs[i + 1];
            Vec3d o1 = d.side[i].multiply(d.halfW[i] * 3.0f), o2 = d.side[i + 1].multiply(d.halfW[i + 1] * 3.0f);
            Trajectories.vtx(buf, m, c1, d.cr, d.cg, d.cb, a1);
            Trajectories.vtx(buf, m, c2, d.cr, d.cg, d.cb, a2);
            Trajectories.vtx(buf, m, c2.add(o2), d.cr, d.cg, d.cb, 0);
            Trajectories.vtx(buf, m, c1.add(o1), d.cr, d.cg, d.cb, 0);
            Trajectories.vtx(buf, m, c1, d.cr, d.cg, d.cb, a1);
            Trajectories.vtx(buf, m, c2, d.cr, d.cg, d.cb, a2);
            Trajectories.vtx(buf, m, c2.subtract(o2), d.cr, d.cg, d.cb, 0);
            Trajectories.vtx(buf, m, c1.subtract(o1), d.cr, d.cg, d.cb, 0);
        }
    }

    private void drawLandingMarkerFill(BufferBuilder fill, MatrixStack.Entry m, SmoothedState state) {
        float radius = this.indicatorSize.getValue();
        if (radius <= 0.01f) {
            return;
        }
        Vec3d impact = state.impact;
        boolean livingHit = state.hitEntity instanceof LivingEntity;
        int baseColor = livingHit ? ColorAssist.getColor(64, 255, 64, 160) : ColorAssist.multAlpha(ColorAssist.fade(1), 0.6f);
        int edgeColor = livingHit ? ColorAssist.getColor(64, 255, 64, 40) : ColorAssist.multAlpha(ColorAssist.fade(1), 0.15f);
        double cx = impact.x;
        double cy = impact.y + 0.02;
        double cz = impact.z;
        int cr = baseColor >> 16 & 0xFF, cg = baseColor >> 8 & 0xFF, cb = baseColor & 0xFF, ca = Trajectories.alphaOf(baseColor);
        int er = edgeColor >> 16 & 0xFF, eg = edgeColor >> 8 & 0xFF, eb = edgeColor & 0xFF, ea = Trajectories.alphaOf(edgeColor);
        for (int i = 0; i < 24; ++i) {
            double a0 = Math.PI * 2 * i / 24;
            double a1 = Math.PI * 2 * (i + 1) / 24;
            double x0 = cx + Math.cos(a0) * radius, z0 = cz + Math.sin(a0) * radius;
            double x1 = cx + Math.cos(a1) * radius, z1 = cz + Math.sin(a1) * radius;
            Trajectories.vtx(fill, m, new Vec3d(cx, cy, cz), cr, cg, cb, ca);
            Trajectories.vtx(fill, m, new Vec3d(x0, cy, z0), er, eg, eb, ea);
            Trajectories.vtx(fill, m, new Vec3d(x1, cy, z1), er, eg, eb, ea);
            Trajectories.vtx(fill, m, new Vec3d(cx, cy, cz), cr, cg, cb, ca);
        }
    }

    private void drawLandingMarkerCross(SmoothedState state) {
        float radius = this.indicatorSize.getValue();
        if (radius <= 0.01f) {
            return;
        }
        Vec3d impact = state.impact;
        boolean livingHit = state.hitEntity instanceof LivingEntity;
        int crossColor = livingHit ? ColorAssist.getColor(64, 255, 64, 200) : ColorAssist.fade(1);
        double cx = impact.x, cy = impact.y + 0.02, cz = impact.z;
        float cross = Math.max(0.15f, radius * 0.35f);
        Render3D.drawLine(new Vec3d(cx - cross, cy, cz), new Vec3d(cx + cross, cy, cz), crossColor, 2.0f, false);
        Render3D.drawLine(new Vec3d(cx, cy, cz - cross), new Vec3d(cx, cy, cz + cross), crossColor, 2.0f, false);
    }

    // ───────────────────────────── low-level buffer ─────────────────────────────

    private static BufferBuilder beginQuads() {
        return Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
    }

    private static void endQuads(BufferBuilder buf, boolean glow) {
        BuiltBuffer built = buf.endNullable();
        if (built != null) {
            WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.QUADS, true, false, glow), built);
        }
    }

    private static void vtx(BufferBuilder buf, MatrixStack.Entry m, Vec3d v, int r, int g, int b, int a) {
        buf.vertex(m, (float)v.x, (float)v.y, (float)v.z).color(Trajectories.argb(r, g, b, a));
    }

    private static int argb(int r, int g, int b, int a) {
        a = MathHelper.clamp(a, 0, 255);
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF;
    }

    private static int lighten(int c, float t) {
        return c + (int)((float)(255 - c) * 0.45f * t);
    }

    private static int alphaOf(int color) {
        int a = color >> 24 & 0xFF;
        return a == 0 ? 255 : a;
    }

    private static Vec3d lerp(Vec3d a, Vec3d b, double t) {
        return a.add(b.subtract(a).multiply(t));
    }

    private static Vec3d cross(Vec3d a, Vec3d b) {
        return new Vec3d(
                a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x);
    }

    // ───────────────────────────── state ─────────────────────────────

    private record Prediction(List<Vec3d> points, Vec3d impact, int ticks, Entity hitEntity, ItemStack icon) {
    }

    private static final class SmoothedState {
        List<Vec3d> points = new ArrayList<Vec3d>();
        Vec3d impact = Vec3d.ZERO;
        int ticks;
        Entity hitEntity;
        ItemStack icon = ItemStack.EMPTY;
        boolean active;

        static SmoothedState from(Prediction raw) {
            SmoothedState state = new SmoothedState();
            state.points = new ArrayList<Vec3d>(raw.points());
            state.impact = raw.impact();
            state.ticks = raw.ticks();
            state.hitEntity = raw.hitEntity();
            state.icon = raw.icon().copy();
            return state;
        }

        void blendToward(Prediction raw, float dt, float speed) {
            float k = 1.0f - (float)Math.exp(-dt * speed);
            if (this.impact.squaredDistanceTo(raw.impact()) > 4096.0) {
                this.points = new ArrayList<Vec3d>(raw.points());
                this.impact = raw.impact();
            } else {
                this.impact = Trajectories.lerp(this.impact, raw.impact(), k);
                this.blendPoints(raw.points(), k);
            }
            this.ticks = raw.ticks();
            this.hitEntity = raw.hitEntity();
            this.icon = raw.icon().copy();
        }

        private void blendPoints(List<Vec3d> target, float k) {
            if (target.isEmpty()) {
                this.points.clear();
                return;
            }
            int count = Math.max(this.points.size(), target.size());
            if (count <= 1) {
                this.points = new ArrayList<Vec3d>(target);
                return;
            }
            List<Vec3d> from = SmoothedState.resample(this.points, count);
            List<Vec3d> to = SmoothedState.resample(target, count);
            this.points = new ArrayList<Vec3d>(count);
            for (int i = 0; i < count; ++i) {
                float weight = k * (0.15f + 0.85f * (float)i / (float)(count - 1));
                this.points.add(Trajectories.lerp(from.get(i), to.get(i), weight));
            }
        }

        /**
         * Равномерная передискретизация ломаной за один проход: длины отрезков считаются один раз, индекс
         * текущего отрезка только растёт. Раньше каждая точка искалась перебором с начала (O(n²)) —
         * это отъедало почти половину времени кадра.
         */
        private static List<Vec3d> resample(List<Vec3d> src, int count) {
            List<Vec3d> out = new ArrayList<Vec3d>(count);
            if (src.isEmpty()) {
                return out;
            }
            if (src.size() == 1 || count <= 1) {
                Vec3d only = src.getFirst();
                for (int i = 0; i < count; ++i) {
                    out.add(only);
                }
                return out;
            }
            int segments = src.size() - 1;
            double[] cumulative = new double[src.size()];
            for (int i = 0; i < segments; ++i) {
                cumulative[i + 1] = cumulative[i] + src.get(i).distanceTo(src.get(i + 1));
            }
            double total = cumulative[segments];
            out.add(src.getFirst());
            int segment = 0;
            for (int i = 1; i < count - 1; ++i) {
                double dist = total * i / (count - 1);
                while (segment < segments - 1 && cumulative[segment + 1] < dist) {
                    ++segment;
                }
                double length = cumulative[segment + 1] - cumulative[segment];
                double t = length <= 1.0E-8 ? 0.0 : (dist - cumulative[segment]) / length;
                out.add(Trajectories.lerp(src.get(segment), src.get(segment + 1), Math.max(0.0, Math.min(1.0, t))));
            }
            out.add(src.getLast());
            return out;
        }

        private static Vec3d sampleAtDistance(List<Vec3d> src, double dist) {
            double walked = 0.0;
            for (int i = 0; i + 1 < src.size(); ++i) {
                Vec3d a = src.get(i);
                Vec3d b = src.get(i + 1);
                double seg = a.distanceTo(b);
                if (walked + seg >= dist) {
                    double t = seg <= 1.0E-8 ? 0.0 : (dist - walked) / seg;
                    return Trajectories.lerp(a, b, t);
                }
                walked += seg;
            }
            return src.getLast();
        }
    }
}
