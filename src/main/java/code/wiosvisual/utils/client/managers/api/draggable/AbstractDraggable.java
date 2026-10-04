/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_332
 *  net.minecraft.class_3532
 *  org.lwjgl.glfw.GLFW
 */
package code.wiosvisual.utils.client.managers.api.draggable;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.common.animation.Animation;
import code.wiosvisual.common.animation.Direction;
import code.wiosvisual.common.animation.implement.Decelerate;
import code.wiosvisual.events.container.SetScreenEvent;
import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.utils.client.managers.api.draggable.Draggable;
import code.wiosvisual.utils.client.managers.file.impl.ElementsFile;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.interfaces.QuickLogger;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public abstract class AbstractDraggable
implements Draggable,
QuickImports,
QuickLogger {
    private String name;
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean dragging;
    private boolean canDrag;
    private int dragX;
    private int dragY;
    private boolean resizeModeActive = false;
    private long lastClickTime = 0L;
    private static final long DOUBLE_CLICK_DELAY_MS = 300L;
    private static AbstractDraggable currentlyEditing = null;
    private float scaleFactor = 1.0f;
    private float renderX;
    private float renderY;
    private boolean renderInit = false;
    private float swayAngle = 0.0f;
    private long lastMotionTime = 0L;
    public Animation scaleAnimation = new Decelerate().setValue(1.0).setMs(100);

    public AbstractDraggable(String name, int x, int y, int width, int height, boolean canDrag) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.canDrag = canDrag;
    }

    @Override
    public boolean visible() {
        return true;
    }

    @Override
    public void tick() {
        this.validPosition();
    }

    @Override
    public void packet(PacketEvent e) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.dragging) {
            this.dragX = 0;
            this.dragY = 0;
        }
        float mouseDragX = mouseX + this.dragX;
        float mouseDragY = mouseY + this.dragY;
        if (this.resizeModeActive && this.isHovered(mouseX, mouseY)) {
            GLFW.glfwSetCursor((long)mc.getWindow().getHandle(), (long)GLFW.glfwCreateStandardCursor((int)221191));
        }
        if (this.dragging) {
            this.x = (int)mouseDragX;
            this.y = (int)mouseDragY;
            this.renderX = this.x;
            this.renderY = this.y;
            this.clampToScreen();
        }
    }

    @Override
    public void setScreen(SetScreenEvent e) {
        if (!PlayerInteractionHelper.isChat(e.getScreen())) {
            this.dragging = false;
            this.resizeModeActive = false;
            this.dragX = 0;
            this.dragY = 0;
            if (currentlyEditing == this) {
                currentlyEditing = null;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return button == 0 && this.beginDrag(mouseX, mouseY);
    }

    public boolean beginDrag(double mouseX, double mouseY) {
        if (this.canDrag && this.isHovered(mouseX, mouseY)) {
            this.dragging = true;
            this.dragX = this.x - (int)mouseX;
            this.dragY = this.y - (int)mouseY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.dragging) {
            try {
                Wiosvisual.getInstance().getFileController().saveFile(ElementsFile.class);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.dragging = false;
        this.dragX = 0;
        this.dragY = 0;
        this.clampToScreen();
        GLFW.glfwSetCursor((long)mc.getWindow().getHandle(), (long)GLFW.glfwCreateStandardCursor((int)221185));
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }

    public void increaseScale() {
        this.scaleFactor = Math.min(2.0f, (float)Math.round((this.scaleFactor + 0.05f) * 100.0f) / 100.0f);
        this.clampToScreen();
    }

    public void decreaseScale() {
        this.scaleFactor = Math.max(0.4f, (float)Math.round((this.scaleFactor - 0.05f) * 100.0f) / 100.0f);
        this.clampToScreen();
    }

    public float getScaleFactor() {
        return this.scaleFactor;
    }

    public void setScaleFactor(float scale) {
        this.scaleFactor = scale;
        this.clampToScreen();
    }

    public void updateEditorMotion() {
        long now = System.currentTimeMillis();
        float dt = this.lastMotionTime == 0L ? 0.0f : (float)(now - this.lastMotionTime) / 1000.0f;
        this.lastMotionTime = now;
        dt = MathHelper.clamp((float)dt, (float)0.0f, (float)0.05f);
        if (!this.renderInit) {
            this.renderX = this.x;
            this.renderY = this.y;
            this.renderInit = true;
        }
        float follow = MathHelper.clamp((float)(dt * 16.0f), (float)0.0f, (float)1.0f);
        float prevRenderX = this.renderX;
        this.renderX += ((float)this.x - this.renderX) * follow;
        this.renderY += ((float)this.y - this.renderY) * follow;
        float velocityX = dt > 0.0f ? (this.renderX - prevRenderX) / dt : 0.0f;
        float swayTarget = this.dragging ? MathHelper.clamp((float)(velocityX * 0.011f), (float)-10.0f, (float)10.0f) : 0.0f;
        float swaySpeed = MathHelper.clamp((float)(dt * 11.0f), (float)0.0f, (float)1.0f);
        this.swayAngle += (swayTarget - this.swayAngle) * swaySpeed;
        this.clampRenderPosition();
    }

    public void clampToScreen() {
    }

    private void clampRenderPosition() {
    }

    public abstract void drawDraggable(DrawContext var1);

    public void drawRect(float x, float y, float width, float height) {
        Render2D.drawQuad(x, y, width, height, ColorAssist.getText(0.5f));
    }

    public void stopAnimation() {
        this.scaleAnimation.setDirection(Direction.BACKWARDS);
    }

    public void startAnimation() {
        this.scaleAnimation.setDirection(Direction.FORWARDS);
    }

    public void validPosition() {
        this.clampToScreen();
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= (double)this.x && mouseX <= (double)(this.x + this.width + 1) && mouseY >= (double)this.y && mouseY <= (double)(this.y + this.height + 1);
    }

    public boolean isCloseAnimationFinished() {
        return this.scaleAnimation.isFinished(Direction.BACKWARDS);
    }

    public boolean canDraw(Hud hud, AbstractDraggable draggable) {
        return hud != null && hud.isState() && this.visible();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    public void setCanDrag(boolean canDrag) {
        this.canDrag = canDrag;
    }

    public void setDragX(int dragX) {
        this.dragX = dragX;
    }

    public void setDragY(int dragY) {
        this.dragY = dragY;
    }

    public void setResizeModeActive(boolean resizeModeActive) {
        this.resizeModeActive = resizeModeActive;
    }

    public void setLastClickTime(long lastClickTime) {
        this.lastClickTime = lastClickTime;
    }

    public void setRenderX(float renderX) {
        this.renderX = renderX;
    }

    public void setRenderY(float renderY) {
        this.renderY = renderY;
    }

    public void setRenderInit(boolean renderInit) {
        this.renderInit = renderInit;
    }

    public void setSwayAngle(float swayAngle) {
        this.swayAngle = swayAngle;
    }

    public void setLastMotionTime(long lastMotionTime) {
        this.lastMotionTime = lastMotionTime;
    }

    public void setScaleAnimation(Animation scaleAnimation) {
        this.scaleAnimation = scaleAnimation;
    }

    public String getName() {
        return this.name;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public boolean isCanDrag() {
        return this.canDrag;
    }

    public int getDragX() {
        return this.dragX;
    }

    public int getDragY() {
        return this.dragY;
    }

    public boolean isResizeModeActive() {
        return this.resizeModeActive;
    }

    public long getLastClickTime() {
        return this.lastClickTime;
    }

    public float getRenderX() {
        return this.renderX;
    }

    public float getRenderY() {
        return this.renderY;
    }

    public boolean isRenderInit() {
        return this.renderInit;
    }

    public float getSwayAngle() {
        return this.swayAngle;
    }

    public long getLastMotionTime() {
        return this.lastMotionTime;
    }

    public Animation getScaleAnimation() {
        return this.scaleAnimation;
    }
}
