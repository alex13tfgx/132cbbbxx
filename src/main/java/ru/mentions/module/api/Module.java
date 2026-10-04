package ru.mentions.module.api;


/** Оболочка над модулем Wios, сохраняющая API модулей mentions (@ModuleInfo, onEnable/onDisable, isEnabled). */
public abstract class Module extends code.wiosvisual.features.module.Module {
    protected Module() {
        super(info().name().trim().replace(" ", ""), info().name(), info().category().wios);
    }

    private static ModuleInfo info() {
        // super() не может обратиться к this, поэтому конкретный класс модуля ищется по стеку вызовов.
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(frames -> frames
            .map(StackWalker.StackFrame::getDeclaringClass)
            .filter(c -> c != Module.class && Module.class.isAssignableFrom(c) && c.isAnnotationPresent(ModuleInfo.class))
            .findFirst()
            .map(c -> c.getAnnotation(ModuleInfo.class))
            .orElseThrow(() -> new IllegalStateException("@ModuleInfo not found")));
    }

    public boolean isEnabled() {
        return this.isState();
    }

    public void setEnabled(boolean enabled) {
        this.setState(enabled);
    }

    public void toggle() {
        this.switchState();
    }

    @Override
    public void activate() {
        this.onEnable();
    }

    @Override
    public void deactivate() {
        this.onDisable();
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }
}
