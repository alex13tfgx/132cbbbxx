/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.emotions;

public enum EmotionType {
    HELLO("\u041f\u0440\u0438\u0432\u0435\u0442\u0441\u0442\u0432\u0438\u0435", "\u041c\u0430\u0448\u0435\u0442 \u0440\u0443\u043a\u043e\u0439 \u0438 \u0437\u0434\u043e\u0440\u043e\u0432\u0430\u0435\u0442\u0441\u044f", "HI", 2500L),
    HAPPY("\u0420\u0430\u0434\u043e\u0441\u0442\u044c", "\u041f\u043e\u0434\u043d\u0438\u043c\u0430\u0435\u0442 \u0440\u0443\u043a\u0438 \u0432\u0432\u0435\u0440\u0445 \u043e\u0442 \u0441\u0447\u0430\u0441\u0442\u044c\u044f", "HP", 2500L),
    CLAP("\u0425\u043b\u043e\u043f\u043a\u0438", "\u0410\u043f\u043b\u043e\u0434\u0438\u0440\u0443\u0435\u0442 \u043e\u0431\u0435\u0438\u043c\u0438 \u0440\u0443\u043a\u0430\u043c\u0438", "CL", 2500L),
    CRY("\u041f\u043b\u0430\u0447", "\u0417\u0430\u043a\u0440\u044b\u0432\u0430\u0435\u0442 \u043b\u0438\u0446\u043e \u0438 \u043f\u043b\u0430\u0447\u0435\u0442", "CR", 2500L),
    DEB("\u0414\u0435\u0431", "\u0420\u0435\u0437\u043a\u0430\u044f \u043f\u043e\u0437\u0430 \u0441 \u043d\u0430\u043a\u043b\u043e\u043d\u043e\u043c \u0433\u043e\u043b\u043e\u0432\u044b", "DB", 2500L),
    FLOSS("\u0424\u043b\u043e\u0441\u0441", "\u0422\u0430\u043d\u0446\u0435\u0432\u0430\u043b\u044c\u043d\u043e\u0435 \u0434\u0432\u0438\u0436\u0435\u043d\u0438\u0435 \u0432 \u0441\u0442\u043e\u0440\u043e\u043d\u044b", "FL", 4500L),
    GET_GRIDDY("Griddy", "\u0411\u0435\u0433\u0443\u0449\u0438\u0439 \u0442\u0430\u043d\u0435\u0446 \u0441 \u043a\u0430\u0447\u043e\u043c \u043a\u043e\u0440\u043f\u0443\u0441\u0430", "GG", 5500L),
    POINT("\u0423\u043a\u0430\u0437\u0430\u0442\u044c", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0435\u0442 \u0440\u0443\u043a\u043e\u0439 \u0432\u043f\u0435\u0440\u0451\u0434", "PT", 2500L),
    SALUTE("\u0421\u0430\u043b\u044e\u0442", "\u041f\u0440\u0438\u043a\u043b\u0430\u0434\u044b\u0432\u0430\u0435\u0442 \u0440\u0443\u043a\u0443 \u043a \u0432\u0438\u0441\u043a\u0443", "SA", 2500L),
    THINK("\u0417\u0430\u0434\u0443\u043c\u0430\u0442\u044c\u0441\u044f", "\u041f\u043e\u0434\u043f\u0438\u0440\u0430\u0435\u0442 \u043f\u043e\u0434\u0431\u043e\u0440\u043e\u0434\u043e\u043a \u0438 \u0441\u043c\u043e\u0442\u0440\u0438\u0442 \u0432 \u0441\u0442\u043e\u0440\u043e\u043d\u0443", "TH", 2500L),
    STRETCH("\u041f\u043e\u0442\u044f\u043d\u0443\u0442\u044c\u0441\u044f", "\u0422\u044f\u043d\u0435\u0442\u0441\u044f \u0432\u0432\u0435\u0440\u0445 \u043e\u0431\u0435\u0438\u043c\u0438 \u0440\u0443\u043a\u0430\u043c\u0438", "UP", 2500L),
    DANCE("\u0422\u0430\u043d\u0435\u0446", "\u0421\u0432\u043e\u0431\u043e\u0434\u043d\u044b\u0439 \u0442\u0430\u043d\u0435\u0446 \u0441 \u043f\u043e\u043a\u0430\u0447\u0438\u0432\u0430\u043d\u0438\u044f\u043c\u0438", "DN", 4500L),
    FACEPALM("\u0424\u0435\u0439\u0441\u043f\u0430\u043b\u043c", "\u041f\u0440\u0438\u043a\u043b\u0430\u0434\u044b\u0432\u0430\u0435\u0442 \u043b\u0430\u0434\u043e\u043d\u044c \u043a \u043b\u0438\u0446\u0443", "FP", 2500L),
    SHRUG("\u041f\u043e\u0436\u0430\u0442\u044c \u043f\u043b\u0435\u0447\u0430\u043c\u0438", "\u0420\u0430\u0437\u0432\u043e\u0434\u0438\u0442 \u0440\u0443\u043a\u0430\u043c\u0438 \u0432 \u043d\u0435\u0434\u043e\u0443\u043c\u0435\u043d\u0438\u0438", "SH", 2500L),
    AIR_DRUM("Барабаны", "Играет на воображаемых барабанах", "AD", "air_drum"),
    ARM_STRETCH("Растяжка рук", "Разминает руки", "AS", "arm_stretch"),
    ARMS_CROSSED("Руки на груди", "Скрещивает руки на груди", "AC", "arms_crossed"),
    BACKFLIP("Сальто", "Кувырок назад", "BF", "backflip"),
    BASIC_DANCES("Простой танец", "Шаг в сторону и покачивание", "BD", "basic_dances"),
    BOW("Поклон", "Вежливо кланяется", "BW", "bow"),
    CHEER("Ура", "Радостно кричит ура", "CH", "cheer"),
    FLEX_MUSCLE("Показать мышцы", "Демонстрирует бицепсы", "FM", "flex_muscle"),
    GANGNAM_STYLE("Gangnam Style", "Знаменитый танец наездника", "GS", "gangnam_style"),
    HANDS_ON_HIPS("Руки в боки", "Упирается руками в бока", "HH", "hands_on_hips"),
    HANDS_UP("Руки вверх", "Поднимает руки вверх", "HU", "hands_up"),
    HEAD_AFFIRM("Кивок", "Кивает головой", "HA", "head_affirm"),
    HIDE("Прятаться", "Прячется и закрывается", "HD", "hide"),
    HUG("Обнимашки", "Обнимает воздух", "HG", "hug"),
    KNEEL("На колено", "Опускается на колено", "KN", "kneel"),
    LAUGH("Смех", "Громко смеётся", "LA", "laugh"),
    LAY("Лежать", "Ложится на землю", "LY", "lay"),
    LEAN("Облокотиться", "Опирается на невидимую стену", "LE", "lean"),
    ROBOT_DANCE("Танец робота", "Дёрганый танец робота", "RD", "robot_dance"),
    SIT("Сесть", "Садится на месте", "ST", "sit"),
    TAKE_THE_L("Take the L", "Показывает букву L", "TL", "take_the_l"),
    TWERK("Тверк", "Танцевальное движение", "TW", "twerk"),
    WAVE("Помахать", "Машет рукой", "WV", "wave"),
    YAWN("Зевок", "Широко зевает", "YW", "yawn");

    private final String title;
    private final String description;
    private final String shortLabel;
    private final long durationMs;
    /** Имя файла из assets/dimasik/player_animation без .json; null у встроенных эмоций. */
    private final String animationFile;

    private EmotionType(String title, String description, String shortLabel, long durationMs) {
        this.title = title;
        this.description = description;
        this.shortLabel = shortLabel;
        this.durationMs = durationMs;
        this.animationFile = null;
    }

    private EmotionType(String title, String description, String shortLabel, String animationFile) {
        this.title = title;
        this.description = description;
        this.shortLabel = shortLabel;
        this.durationMs = JsonEmotes.durationOf(animationFile);
        this.animationFile = animationFile;
    }

    public String getAnimationFile() {
        return this.animationFile;
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    public String getShortLabel() {
        return this.shortLabel;
    }

    public long getDurationMs() {
        return this.durationMs;
    }
}
