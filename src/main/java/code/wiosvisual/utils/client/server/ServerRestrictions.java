package code.wiosvisual.utils.client.server;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

/**
 * Ограничения по правилам серверов. Пока игрок на сервере из списка, запрещённые там
 * модули скрыты из ClickGUI, выключены и не включаются ни кнопкой, ни биндом, а
 * запрещённые функции внутри разрешённых модулей просто не работают.
 * На других серверах и в одиночной игре всё возвращается: модули снова видны,
 * а те, что выключили мы, включаются обратно.
 *
 * Правила каждого сервера — в RULES ниже; это единственное место, которое нужно
 * править, чтобы добавить сервер или поменять набор запретов.
 */
public final class ServerRestrictions
implements QuickImports {
    /** Функции внутри модулей, которые запрещены отдельно от самого модуля. */
    public enum Feature {
        /** NoRender «Плохие эффекты»: слепота и прочие (FT: RemoveBlindness). */
        REMOVE_BAD_EFFECTS,
        /** NoRender «Тьма»: эффект тьмы вардена (FT: No Darkness Effect). */
        REMOVE_DARKNESS,
        /** NoRender «Растительность»: трава не мешает целиться (RW: Grass Bypass). */
        REMOVE_VEGETATION,
        /** HitboxCustomizer «Подсвечивать друзей» (FT: Friend Highlighter). */
        FRIEND_HIGHLIGHT
    }

    /** Правила одного сервера: его домены, запрещённые модули и функции. */
    private record Rules(String name, Set<String> domains, Set<String> modules, Set<Feature> features) {
    }

    private static final List<Rules> RULES = List.of(
            new Rules("FunTime",
                    Set.of("funtime.ru"),
                    Set.of(
                            "NoFriendDamage",   // Don't hit teammates
                            "TapeMouse",        // AutoAttack
                            "Assist",           // Firework Helper и аналоги: предмет из инвентаря по кнопке
                            "ItemScroller",     // InventoryControlTweaks / Quickstack / Inventory Profiles Next
                            "ShulkerBypass",    // то же: перекладывание из хранилища по кнопке
                            "AuctionHelper",    // AucHelper / funtime-ah-helper
                            "BodyGlow"),        // Re:Entity Outline — подсветка игроков
                    Set.of(Feature.REMOVE_BAD_EFFECTS, Feature.REMOVE_DARKNESS, Feature.FRIEND_HIGHLIGHT)),
            new Rules("ReallyWorld",
                    Set.of("reallyworld.ru", "rwdonate.ru"),
                    Set.of(),
                    Set.of(Feature.REMOVE_VEGETATION)),   // Grass Bypass
            new Rules("CakeWorld",
                    Set.of("cakeworld.pw"),
                    Set.of(
                            "TapeMouse",        // автокликер в ПВП
                            "Assist",           // макросы, дающие преимущество в ПВП
                            "ShiftTab",
                            "ShulkerBypass",    // автовзаимодействие с хранилищем (ItemScroller разрешён)
                            "BodyGlow"),        // ESP: подсветка игроков
                    Set.of()),
            // HolyWorld, п. 2.4 правил (Лайт и Прайм анархии): wiki.holyworld.me/lite/rules, /prime/rules.
            // Работает и без ответа LiteAPI; блок-лист Feature Control, если придёт, добавится сверху.
            new Rules("HolyWorld",
                    Set.of("holyworld.ru", "holyworld.me"),
                    Set.of(
                            "Trajectories",     // прямо назван: «Trajectories/Predict»
                            "TapeMouse",        // автокликер в ПВП
                            "ShiftTab",         // ПВП-макрос: присед в момент удара
                            "BodyGlow",         // ESP: подсветка игроков
                            "Assist",           // макросы: /fix, авто-закрытие меню, предметы по бинду
                            "ShulkerBypass",    // автовзаимодействие с хранилищем (ItemScroller разрешён)
                            "AutoClanUpgrade",  // макрос: ставит и ломает факел
                            "AutoInvest",       // автоматизация действий по порогу баланса
                            "AutoCommand",      // скрипт: команды по таймеру
                            "AutoTpAccept"),    // сценарий, реагирующий на окружение (запросы в чате)
                    Set.of(Feature.REMOVE_BAD_EFFECTS, Feature.REMOVE_DARKNESS, Feature.REMOVE_VEGETATION)));

    private static final ServerRestrictions INSTANCE = new ServerRestrictions();

    /** Модули, которые выключили мы (а не игрок) — их включаем обратно, когда запрет снят. */
    private final Set<Module> suppressed = new LinkedHashSet<>();
    private Rules lastRules;

    private ServerRestrictions() {
    }

    public static ServerRestrictions getInstance() {
        return INSTANCE;
    }

    /** Правила сервера, на котором игрок сейчас, или null. */
    private static Rules currentRules() {
        if (mc == null || mc.isInSingleplayer()) {
            return null;
        }
        ServerInfo entry = mc.getCurrentServerEntry();
        if (entry == null || entry.address == null) {
            return null;
        }
        String host = ServerRestrictions.hostOf(entry.address);
        for (Rules rules : RULES) {
            for (String domain : rules.domains()) {
                if (host.equals(domain) || host.endsWith("." + domain)) {
                    return rules;
                }
            }
        }
        return null;
    }

    /**
     * Модуль запрещён там, где игрок находится прямо сейчас: статичными правилами сервера
     * или блок-листом, который прислал сервер через HolyWorld LiteAPI (Feature Control).
     */
    public static boolean isForbiddenHere(Module module) {
        Rules rules = ServerRestrictions.currentRules();
        return rules != null && rules.modules().contains(module.getName()) || HolyWorldLiteApi.isBlocked(module);
    }

    /** Функция внутри модуля запрещена там, где игрок находится прямо сейчас. */
    public static boolean isForbiddenHere(Feature feature) {
        Rules rules = ServerRestrictions.currentRules();
        return rules != null && rules.features().contains(feature);
    }

    /** «mc.FunTime.ru:25565.» → «mc.funtime.ru». */
    private static String hostOf(String address) {
        String host = address.trim().toLowerCase(Locale.ROOT);
        int colon = host.lastIndexOf(':');
        // Порт отрезаем, только если двоеточие одно: иначе это IPv6 без порта.
        if (colon > 0 && host.indexOf(':') == colon) {
            host = host.substring(0, colon);
        }
        while (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        return host;
    }

    /**
     * Раз в тик сверяем модули с правилами текущего сервера. По тику, а не по пакету
     * входа — так ловится и переход между серверами через прокси, и смена одного
     * сервера с ограничениями на другой, где запреты другие.
     */
    @EventHandler
    public void onTick(TickEvent e) {
        this.enforceNow();
    }

    /** Модуль включён игроком, но сейчас выключен нами из-за запрета — в конфиг он сохраняется включённым. */
    public static boolean isSuppressed(Module module) {
        return INSTANCE.suppressed.contains(module);
    }

    /** Сверить модули с запретами прямо сейчас (например, сразу после нового блок-листа LiteAPI). */
    public void enforceNow() {
        if (mc.player == null) {
            return;
        }
        Rules rules = ServerRestrictions.currentRules();
        boolean rulesChanged = rules != this.lastRules;
        this.lastRules = rules;

        // Возвращаем то, что больше не запрещено (ушли с сервера или перешли на другой).
        Iterator<Module> it = this.suppressed.iterator();
        while (it.hasNext()) {
            Module module = it.next();
            if (ServerRestrictions.isForbiddenHere(module)) {
                continue;
            }
            it.remove();
            if (!module.isState()) {
                // Тихо: после каждого входа на сервер с LiteAPI возвращаются все модули разом.
                module.setStateQuietly(true);
            }
        }
        List<String> turnedOff = new ArrayList<>();
        for (Module module : ServerRestrictions.allModules()) {
            if (!module.isState() || !ServerRestrictions.isForbiddenHere(module)) {
                continue;
            }
            this.suppressed.add(module);
            module.setState(false);
            // Блокировки HolyWorld не называем: игрок не должен видеть даже упоминания функции.
            if (rules != null && rules.modules().contains(module.getName())) {
                turnedOff.add(module.getVisibleName());
            }
        }
        if (rules != null && rulesChanged) {
            // Пишем весь список убранного, а не только то, что было включено.
            List<String> hidden = new ArrayList<>();
            for (Module module : ServerRestrictions.allModules()) {
                if (rules.modules().contains(module.getName())) {
                    hidden.add(module.getVisibleName());
                }
            }
            if (!hidden.isEmpty() || !rules.features().isEmpty()) {
                String msg = "§7[Wios] §fПо правилам " + rules.name() + " убрано: §c" + String.join(", ", hidden);
                if (!rules.features().isEmpty()) {
                    msg += (hidden.isEmpty() ? "" : "§f; ") + "§fв NoRender отключено: §c" + featureNames(rules.features());
                }
                if (!turnedOff.isEmpty()) {
                    msg += "§7 (выключено: " + String.join(", ", turnedOff) + ")";
                }
                mc.player.sendMessage(Text.literal(msg), false);
            }
        }
    }

    private static String featureNames(Set<Feature> features) {
        List<String> names = new ArrayList<>();
        for (Feature feature : features) {
            names.add(switch (feature) {
                case REMOVE_BAD_EFFECTS -> "плохие эффекты";
                case REMOVE_DARKNESS -> "тьма";
                case REMOVE_VEGETATION -> "растительность";
                case FRIEND_HIGHLIGHT -> "подсветка друзей";
            });
        }
        return String.join(", ", names);
    }

    private static List<Module> allModules() {
        Wiosvisual client = Wiosvisual.getInstance();
        if (client == null || client.getModuleRepository() == null) {
            return List.of();
        }
        return client.getModuleRepository().modules();
    }
}
