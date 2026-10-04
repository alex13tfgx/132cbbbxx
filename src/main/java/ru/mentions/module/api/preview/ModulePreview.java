package ru.mentions.module.api.preview;

/** В Wios предпоказа нет: интерфейс оставлен, чтобы модули mentions компилировались без правок. */
public interface ModulePreview {

    PreviewSettings previewSettings();

    default boolean isPreviewActive() {
        return false;
    }

    void previewSpawn(PreviewContext ctx);

    default boolean previewNeedsDummy() {
        return false;
    }

    default boolean previewControlsDummy() {
        return false;
    }

    default void previewStart(PreviewContext ctx) {
    }

    default void previewTick(PreviewContext ctx) {
    }

    default void previewStop() {
    }
}
