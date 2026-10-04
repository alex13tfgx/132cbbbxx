package code.wiosvisual.display.hud;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.common.animation.Animation;
import code.wiosvisual.common.animation.Direction;
import code.wiosvisual.common.animation.implement.Decelerate;
import code.wiosvisual.common.animation.implement.EaseOut;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.utils.client.managers.api.draggable.AbstractDraggable;
import code.wiosvisual.utils.client.packet.network.ServerUtil;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import code.wiosvisual.utils.interactions.interact.PlayerInteractionHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * TargetHud в раскладке Mytheria: плашка 100×32 со скруглением 8, голова 18×18 слева,
 * ник и строка «HP / N» справа от неё, во всю ширину снизу — тонкая полоса здоровья
 * с четырьмя слоями свечения по краям. Пока плашка появляется, содержимое съезжает
 * вниз на 6 пикселей и одновременно проявляется.
 */
public class TargetHud
extends AbstractDraggable {
    private static final int TEXT = new Color(240, 240, 242, 255).getRGB();

    private static final float WIDTH = 100.0f;
    private static final float HEIGHT = 32.0f;
    private static final float PANEL_ROUND = 8.0f;

    private static final float HEAD_X = 4.0f;
    private static final float HEAD_Y = 3.5f;
    private static final float HEAD_SIZE = 18.0f;
    private static final float HEAD_ROUND = 2.0f;

    /** Левый край текстового блока и его вертикальные позиции до сдвига появления. */
    private static final float CONTENT_X = 26.0f;
    private static final float NAME_Y = 2.0f;
    private static final float HP_Y = 11.0f;
    /** На столько содержимое съезжает вниз, пока плашка появляется. */
    private static final float APPEAR_SHIFT = 6.0f;

    private static final float BAR_X = 4.0f;
    private static final float BAR_Y = 25.0f;
    private static final float BAR_W = 92.0f;
    private static final float BAR_H = 2.4f;
    private static final float BAR_ROUND = 0.5f;

    /** Сколько плашка остаётся на экране после того, как цель пропала из прицела. */
    private static final long HOLD_MS = 2000L;

    private static TargetHud instance;
    private long lastTargetSeen;
    private final Animation toggleAnimation;
    private final Animation faceAlphaAnimation;
    /** Появление содержимого: стартует, когда сама плашка уже проявилась. */
    private final Animation appearAnimation;
    private final FontRenderer nameFont = Fonts.getSize(13, Fonts.Type.SuisseIntlRegular);
    private final FontRenderer hpFont = Fonts.getSize(11, Fonts.Type.SuisseIntlSemiBold);
    private LivingEntity target;
    private Identifier cachedFaceTexture;
    private float displayedHealth;
    private float healthBarProgress;
    /** Обрезка ника меряет текст побуквенно — держим результат, пока исходная строка та же. */
    private String lastRawName;
    private String lastDisplayName = "";
    /** Число здоровья меняется не каждый кадр, а форматирование стоит дороже сравнения. */
    private int lastHealthTenths = Integer.MIN_VALUE;
    private String lastHealthText = "0.0";
    private long lastFrameTime;
    private boolean showOnHover = true;
    /** Сохраняется в конфиге элементов; в раскладке Mytheria брони на плашке нет. */
    private int armorMode = 1;

    public static TargetHud getInstance() {
        if (instance == null) {
            instance = new TargetHud();
        }
        return instance;
    }

    public TargetHud() {
        super("Target Hud", 10, 120, (int)WIDTH, (int)HEIGHT, true);
        instance = this;
        this.scaleAnimation = new EaseOut().setValue(1.0).setMs(150);
        this.toggleAnimation = new Decelerate().setMs(250).setValue(1.0);
        this.faceAlphaAnimation = new Decelerate().setMs(125).setValue(1.0);
        this.appearAnimation = new Decelerate().setMs(300).setValue(1.0);
    }

    /** Текущая цель плашки (в открытом чате — сам игрок), либо null. */
    public LivingEntity getCurrentTarget() {
        return this.toggleAnimation.getOutput().floatValue() > 0.01f ? this.target : null;
    }

    public boolean isShowOnHover() {
        return this.showOnHover;
    }

    public void setShowOnHover(boolean showOnHover) {
        this.showOnHover = showOnHover;
    }

    public int getArmorMode() {
        return this.armorMode;
    }

    public void setArmorMode(int armorMode) {
        this.armorMode = armorMode;
    }

    @Override
    public boolean canDraw(Hud hud, AbstractDraggable draggable) {
        if (!hud.isState()) return false;
        if (!Hud.interfaceSettings.isSelected("Target Hud")) return false;
        return this.visible();
    }

    @Override
    public boolean visible() {
        if (Hud.getInstance() == null) {
            return false;
        }
        boolean isActive = Hud.interfaceSettings.isSelected("Target Hud") && Hud.getInstance().state;
        return isActive && this.toggleAnimation.getOutput().floatValue() > 0.01f;
    }

    @Override
    public void tick() {
        if (!Hud.interfaceSettings.isSelected("Target Hud") || Hud.getInstance() == null || !Hud.getInstance().state) {
            return;
        }
        LivingEntity nextTarget = this.resolveTarget();
        long now = System.currentTimeMillis();
        if (nextTarget != null) {
            this.lastTargetSeen = now;
        } else if (this.target != null && now - this.lastTargetSeen < HOLD_MS && TargetHud.mc.world != null && !this.target.isRemoved()
                && (this.target == TargetHud.mc.player || TargetHud.mc.player != null && TargetHud.mc.player.canSee((Entity)this.target))) {
            // Цель ушла из прицела — держим плашку ещё пару секунд, а не прячем сразу.
            nextTarget = this.target;
        }
        if (nextTarget != null) {
            if (this.target != nextTarget) {
                this.displayedHealth = ServerUtil.getHealthValue(nextTarget);
                this.healthBarProgress = this.healthFraction(nextTarget);
                this.cachedFaceTexture = null;
            }
            this.target = nextTarget;
            this.startAnimation();
            this.toggleAnimation.setDirection(Direction.FORWARDS);
            this.faceAlphaAnimation.setDirection(Direction.FORWARDS);
            this.updateCachedFaceTexture();
        } else {
            this.toggleAnimation.setDirection(Direction.BACKWARDS);
            this.faceAlphaAnimation.setDirection(Direction.BACKWARDS);
            if (this.toggleAnimation.isFinished(Direction.BACKWARDS)) {
                this.target = null;
                this.cachedFaceTexture = null;
            }
        }
    }

    private LivingEntity resolveTarget() {
        LivingEntity lookedAt;
        if (this.showOnHover && (lookedAt = this.raycastLiving(50.0f)) != null) {
            return lookedAt;
        }
        if (PlayerInteractionHelper.isChat(TargetHud.mc.currentScreen) && TargetHud.mc.player != null) {
            return TargetHud.mc.player;
        }
        return null;
    }

    /** Луч из прицела на {@code distance} блоков. */
    private LivingEntity raycastLiving(float distance) {
        Entity hitEntity;
        ClientPlayerEntity player = TargetHud.mc.player;
        if (player == null || TargetHud.mc.world == null) {
            return null;
        }
        float tickDelta = tickCounter.getTickProgress(false);
        Vec3d eyes = player.getCameraPosVec(tickDelta);
        Vec3d look = player.getRotationVec(tickDelta);
        Vec3d reach = eyes.add(look.multiply((double)distance));
        Box box = player.getBoundingBox().stretch(look.multiply((double)distance)).expand(1.0);
        EntityHitResult hit = ProjectileUtil.raycast((Entity)player, (Vec3d)eyes, (Vec3d)reach, (Box)box, entity -> entity instanceof LivingEntity && entity != player && entity.isAlive(), (double)(distance * distance));
        if (hit == null || !((hitEntity = hit.getEntity()) instanceof LivingEntity)) {
            return null;
        }
        // Луч по сущностям проходит сквозь блоки — отсекаем цели за стенами.
        Vec3d hitPos = hit.getPos();
        HitResult blockHit = TargetHud.mc.world.raycast(new RaycastContext(eyes, hitPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)player));
        if (blockHit != null && blockHit.getType() != HitResult.Type.MISS) {
            return null;
        }
        return (LivingEntity)hitEntity;
    }

    /**
     * Скин берётся напрямую у сущности игрока: дёргать EntityRenderDispatcher
     * из тика нельзя, там ещё не настроена камера.
     */
    private void updateCachedFaceTexture() {
        LivingEntity entity = this.target;
        if (entity instanceof AbstractClientPlayerEntity) {
            AbstractClientPlayerEntity clientPlayer = (AbstractClientPlayerEntity)entity;
            this.cachedFaceTexture = clientPlayer.getSkin() == null ? null : net.minecraft.util.Identifier.of("textures/entity/player/wide/steve.png") /* TODO port: skin API class_12081 */;
        }
    }

    private float healthFraction(LivingEntity entity) {
        float maxHp = ServerUtil.getMaxHealth(entity);
        if (maxHp <= 0.0f || !entity.isAlive()) {
            return 0.0f;
        }
        return MathHelper.clamp((float)(ServerUtil.getHealthValue(entity) / maxHp), (float)0.0f, (float)1.0f);
    }

    @Override
    public void drawDraggable(DrawContext context) {
        float alpha;
        if (!Hud.interfaceSettings.isSelected("Target Hud") || Hud.getInstance() == null || !Hud.getInstance().state) {
            return;
        }
        if ((alpha = this.toggleAnimation.getOutput().floatValue()) <= 0.01f || this.target == null) {
            return;
        }
        this.render(context, GuiCompat.stack(context), this.target, alpha);
    }

    private void render(DrawContext context, MatrixStack matrix, LivingEntity target, float alpha) {
        long now = System.currentTimeMillis();
        float delta = this.lastFrameTime == 0L ? 0.0f : Math.min(0.1f, (float)(now - this.lastFrameTime) / 1000.0f);
        this.lastFrameTime = now;

        // Содержимое проявляется только после того, как сама плашка полностью пришла.
        this.appearAnimation.setDirection(alpha >= 0.99f ? Direction.FORWARDS : Direction.BACKWARDS);
        float appear = MathHelper.clamp((float)this.appearAnimation.getOutput().floatValue(), (float)0.0f, (float)1.0f);
        float contentAlpha = alpha * appear;
        float shift = APPEAR_SHIFT * appear;

        matrix.push();
        float x = this.getX();
        float y = this.getY();

        this.drawPanel(matrix, x, y, alpha);
        this.drawFace(context, x + HEAD_X, y + HEAD_Y, contentAlpha);
        this.drawContent(matrix, target, x, y, shift, contentAlpha);
        this.drawHealthBar(matrix, target, x, y, delta, contentAlpha);

        this.setWidth((int)WIDTH);
        this.setHeight((int)HEIGHT);
        matrix.pop();
    }

    /** Подложка — один скруглённый прямоугольник цвета темы, без обводки. */
    private void drawPanel(MatrixStack matrix, float x, float y, float alpha) {
        int fill = ColorAssist.multAlpha(HudTheme.panel(255), alpha * HudTheme.backgroundOpacity());
        rectangle.render(ShapeProperties.create(matrix, x, y, WIDTH, HEIGHT).round(PANEL_ROUND).color(fill).build());
        Rectangle.flushPending();
    }

    /** Голова цели 18×18 со скруглением 2; у мобов вместо скина — первая буква имени. */
    private void drawFace(DrawContext context, float faceX, float faceY, float alpha) {
        float faceAlpha = MathHelper.clamp((float)(alpha * this.faceAlphaAnimation.getOutput().floatValue()), (float)0.0f, (float)1.0f);
        if (faceAlpha <= 0.01f) {
            return;
        }
        if (this.cachedFaceTexture != null) {
            int color = new Color(255, 255, 255, Math.round(255.0f * faceAlpha)).getRGB();
            try {
                Render2D.drawTexture(context, this.cachedFaceTexture, faceX, faceY, HEAD_SIZE, HEAD_ROUND, 8, 8, 64, ColorAssist.getRect(faceAlpha), color);
            }
            finally {
            }
            return;
        }
        MatrixStack matrix = GuiCompat.stack(context);
        String name = this.target == null ? "?" : this.target.getName().getString();
        String letter = this.target instanceof PlayerEntity && !name.isEmpty() ? name.substring(0, 1).toUpperCase(Locale.ROOT) : "?";
        rectangle.render(ShapeProperties.create(matrix, faceX, faceY, HEAD_SIZE, HEAD_SIZE).round(HEAD_ROUND).color(ColorAssist.multAlpha(new Color(128, 128, 128, 24).getRGB(), faceAlpha)).build());
        Rectangle.flushPending();
        float letterW = this.nameFont.getStringWidth(letter);
        this.nameFont.drawString(matrix, letter, faceX + (HEAD_SIZE - letterW) / 2.0f, faceY + HEAD_SIZE / 2.0f - 3.5f, ColorAssist.multAlpha(TEXT, faceAlpha));
    }

    /** Ник и строка «HP / N» — обе съезжают вниз вместе с появлением плашки. */
    private void drawContent(MatrixStack matrix, LivingEntity target, float x, float y, float shift, float alpha) {
        if (alpha <= 0.01f) {
            return;
        }
        boolean hidden = target.isInvisible();
        String raw = hidden ? "\u041d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u044b\u0439" : target.getName().getString();
        int textColor = ColorAssist.multAlpha(HudTheme.text(255), alpha);
        this.nameFont.drawString(matrix, this.displayName(raw, hidden), x + CONTENT_X, y + NAME_Y + shift, textColor);

        String value = hidden || this.displayedHealth >= 1000.0f ? "?" : this.healthText();
        this.hpFont.drawString(matrix, "HP / " + value, x + CONTENT_X, y + HP_Y + shift, ColorAssist.multAlpha(HudTheme.text(255), alpha * 0.7f));
    }

    /**
     * Полоса здоровья: дорожка во всю ширину, затем четыре слоя свечения вокруг
     * заполненной части — каждый на 0.5 пикселя шире предыдущего и вдвое ярче.
     */
    private void drawHealthBar(MatrixStack matrix, LivingEntity target, float x, float y, float delta, float alpha) {
        if (alpha <= 0.01f) {
            return;
        }
        float hp = Math.max(0.0f, ServerUtil.getHealthValue(target));
        float maxHp = Math.max(1.0f, ServerUtil.getMaxHealth(target));
        if (maxHp < hp) {
            maxHp = hp;
        }
        this.displayedHealth += (hp - this.displayedHealth) * Math.min(1.0f, delta * 8.0f);
        this.healthBarProgress += (Math.min(1.0f, hp / maxHp) - this.healthBarProgress) * Math.min(1.0f, delta * 8.0f);

        float barX = x + BAR_X;
        float barY = y + BAR_Y;
        int track = ColorAssist.multAlpha(HudTheme.track(255), alpha);
        rectangle.render(ShapeProperties.create(matrix, barX, barY, BAR_W, BAR_H).round(BAR_ROUND).color(track).build());

        float filled = BAR_W * MathHelper.clamp((float)this.healthBarProgress, (float)0.0f, (float)1.0f);
        if (filled > 0.01f) {
            int accent = HudTheme.targetIcon(255);
            float[] spread = new float[]{2.0f, 1.5f, 1.0f, 0.5f};
            float[] glowRound = new float[]{1.5f, 1.25f, 1.0f, 0.75f};
            float[] glowAlpha = new float[]{0.05f, 0.1f, 0.15f, 0.2f};
            for (int i = 0; i < spread.length; ++i) {
                float pad = spread[i];
                rectangle.render(ShapeProperties.create(matrix, barX - pad, barY - pad, filled + pad * 2.0f, BAR_H + pad * 2.0f)
                        .round(glowRound[i]).color(ColorAssist.multAlpha(accent, alpha * glowAlpha[i])).build());
            }
            rectangle.render(ShapeProperties.create(matrix, barX, barY, filled, BAR_H).round(BAR_ROUND).color(ColorAssist.multAlpha(accent, alpha)).build());
        }
        Rectangle.flushPending();
    }

    /**
     * Имя для отрисовки: снятие цветовых кодов и обрезка делаются один раз на смену ника,
     * а не каждый кадр: обрезка меряет ширину строки на каждой букве.
     */
    private String displayName(String raw, boolean hidden) {
        if (raw.equals(this.lastRawName)) {
            return this.lastDisplayName;
        }
        String clean = raw;
        if (!hidden) {
            String stripped = ColorAssist.removeFormatting(raw);
            clean = stripped == null ? "" : stripped;
        }
        this.lastRawName = raw;
        this.lastDisplayName = this.trimName(clean, WIDTH - 36.0f);
        return this.lastDisplayName;
    }

    /** «19.4» без {@code String.format}: тот создаёт Formatter на каждый вызов. */
    private String healthText() {
        int tenths = Math.max(0, Math.round(this.displayedHealth * 10.0f));
        if (tenths != this.lastHealthTenths) {
            this.lastHealthTenths = tenths;
            this.lastHealthText = tenths / 10 + "." + tenths % 10;
        }
        return this.lastHealthText;
    }

    /** Длинные ники обрезаются многоточием, чтобы не выезжать за плашку. */
    private String trimName(String text, float maxWidth) {
        if (this.nameFont.getStringWidth(text) <= maxWidth) {
            return text;
        }
        StringBuilder builder = new StringBuilder();
        for (char symbol : text.toCharArray()) {
            if (this.nameFont.getStringWidth(builder.toString() + symbol + "..") > maxWidth) break;
            builder.append(symbol);
        }
        return builder.append("..").toString();
    }
}
