package ru.mentions.gui;

/** Звуки интерфейса mentions в Wios не используются: все вызовы намеренно пустые. */
public final class GuiSounds {
    private GuiSounds() {}

    public static void open() {}
    public static void close() {}
    public static void scroll() {}
    public static void type() {}
    public static void erase() {}
    public static void button() {}
    public static void sliderGrab() {}
    public static void sliderRelease() {}
    public static void sliderTick(float progress) {}
    public static void colorTick(float progress) {}
    public static void bindStart() {}
    public static void bindReset() {}
    public static void bindSet() {}
    public static void expand(boolean opened) {}
    public static void editStart() {}
    public static void editCommit() {}
    public static void editCancel() {}
    public static void searchClear() {}
    public static void category(int index, int total) {}
    public static void chip(int index, int total) {}
    public static void chipMulti(boolean enabled) {}
    public static void toggle(boolean enabled) {}
    public static void picker(boolean opened) {}
}
