package code.wiosvisual.features.impl.render;

import code.wiosvisual.events.keyboard.HotBarScrollEvent;
import code.wiosvisual.events.render.FovEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BindSetting;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import org.lwjgl.glfw.GLFW;

/**
 * Плавное приближение камеры по удержанию клавиши, перенесено из System.
 * Работает через FovEvent: пока клавиша зажата, FOV плавно уходит к значению зума,
 * колесо мыши (если включено) меняет уровень приближения.
 */
public class Zoom
extends Module {
    private static final float MIN_FOV = 1.0f;
    private static final float MAX_FOV = 120.0f;

    private final BindSetting key = new BindSetting("Клавиша зума", "Зажмите для приближения").setKey(67);
    private final SliderSettings zoomFov = new SliderSettings("FOV зума", "Уровень приближения (меньше — ближе)").setValue(30.0f).range(MIN_FOV, MAX_FOV).step(1.0f);
    private final SliderSettings speed = new SliderSettings("Скорость", "Скорость анимации").setValue(4.0f).range(1.0f, 15.0f).step(0.5f);
    private final BooleanSetting scrollControl = new BooleanSetting("Скролл", "Колесо мыши меняет уровень зума").setValue(true);
    private final SliderSettings scrollStep = new SliderSettings("Шаг скролла", "Насколько меняется FOV за один скролл").setValue(5.0f).range(1.0f, 20.0f).step(1.0f).visible(this.scrollControl::isValue);

    private float current = 0.0f;
    private float target = 30.0f;
    private long lastTime = 0L;

    public Zoom() {
        super("Zoom", "Zoom", ModuleCategory.RENDER);
        this.setup(this.key, this.zoomFov, this.speed, this.scrollControl, this.scrollStep);
    }

    public static Zoom getInstance() {
        return Instance.get(Zoom.class);
    }

    @Override
    public void activate() {
        super.activate();
        this.current = 0.0f;
        this.target = this.zoomFov.getValue();
        this.lastTime = System.nanoTime();
    }

    private boolean holding() {
        return Zoom.mc.currentScreen == null && this.key.getKey() != -1 && GLFW.glfwGetKey((long)Zoom.mc.getWindow().getHandle(), (int)this.key.getKey()) == 1;
    }

    private void advance() {
        long now = System.nanoTime();
        if (this.lastTime != 0L && now - this.lastTime <= 1_000_000_000L) {
            float dt = (float)(now - this.lastTime) / 1.0E9f;
            float sp = this.speed.getValue();
            this.current = this.holding() ? Math.min(1.0f, this.current + dt * sp) : Math.max(0.0f, this.current - dt * sp);
        }
        this.lastTime = now;
        if (!this.scrollControl.isValue()) {
            this.target = this.zoomFov.getValue();
        }
    }

    private static float ease(float f) {
        return f < 0.5f ? 4.0f * f * f * f : 1.0f - (float)Math.pow((double)(-2.0f * f + 2.0f), 3.0) / 2.0f;
    }

    @EventHandler
    public void onFov(FovEvent e) {
        this.advance();
        if (this.current <= 0.001f) {
            return;
        }
        int base = ((Integer)Zoom.mc.options.getFov().getValue()).intValue();
        float goal = this.scrollControl.isValue() ? this.target : this.zoomFov.getValue();
        float fov = (float)base + (goal - (float)base) * ease(this.current);
        e.setFov(Math.round(Math.max(MIN_FOV, Math.min((float)base, fov))));
        e.cancel();
    }

    @EventHandler
    public void onScroll(HotBarScrollEvent e) {
        if (this.scrollControl.isValue() && this.holding()) {
            this.target = Math.max(MIN_FOV, Math.min(MAX_FOV, this.target - (float)e.getVertical() * this.scrollStep.getValue()));
            e.cancel();
        }
    }
}
