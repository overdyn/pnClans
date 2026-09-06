package ua.inventorytype.pnclans.impl.config

import com.charleskorn.kaml.YamlComment
import kotlinx.serialization.Serializable
import ua.inventorytype.pnclans.api.Action
import ua.inventorytype.pnclans.api.OpenGuiAction
import ua.inventorytype.pnclans.api.SoundAction

@Serializable
data class GuiItemConfig(
    val slot: Int = 0,
    val material: String = "STONE",
    val name: String = "",
    val lore: List<String> = emptyList(),
    val glow: Boolean = false,
    val permission: String? = null,
    val actions: List<Action> = emptyList()
)

@Serializable
data class GuiMenuConfig(
    val title: String = "Клан",
    val rows: Int = 6,
    val items: Map<String, GuiItemConfig> = emptyMap()
)

@Serializable
data class GuiBackgroundConfig(
    @YamlComment("Включает декоративный фон в тех GUI, где он добавлен явно")
    val enabled: Boolean = true,
    @YamlComment("Материал основных ячеек фона")
    val primaryMaterial: String = "BLACK_STAINED_GLASS_PANE",
    @YamlComment("Материал акцентных ячеек фона")
    val secondaryMaterial: String = "ORANGE_STAINED_GLASS_PANE",
    @YamlComment("Слоты основных ячеек фона")
    val primarySlots: List<Int> = listOf(0, 2, 3, 4, 5, 6, 8, 10, 16, 17, 18, 26, 27, 35, 37, 43, 45, 47, 48, 50, 51, 53),
    @YamlComment("Слоты акцентных ячеек фона")
    val secondarySlots: List<Int> = listOf(1, 7, 9, 11, 12, 13, 14, 15, 19, 25, 28, 34, 36, 38, 39, 40, 41, 42, 44, 46, 52)
)

/**
 * Configuration of the main clan dashboard with dynamic state labels.
 *
 * @property display Text values injected by [ua.inventorytype.pnclans.impl.ux.MainUX]
 * into the dashboard item templates.
 */
@Serializable
data class MainMenuConfig(
    val title: String = "&#FC7D37« Штаб Клана »",
    val rows: Int = 6,
    val items: Map<String, GuiItemConfig> = emptyMap(),
    @YamlComment("Расширения главного меню от аддонов. Ключ используется как configId в API; item и action ссылаются на зарегистрированные ID вида addon-id:item-id.")
    val addons: Map<String, AddonGuiItemConfig> = emptyMap(),
    @YamlComment("Тексты динамических состояний главного меню")
    val display: MainMenuDisplayConfig = MainMenuDisplayConfig()
)

@Serializable
data class AddonGuiItemConfig(
    @YamlComment("Слот предмета в главном меню: от 0 до 53.")
    val slot: Int = 0,
    @YamlComment("ID provider, зарегистрированного аддоном, например clan-missions:daily-quest.")
    val item: String,
    @YamlComment("Необязательный ID click action, зарегистрированный аддоном.")
    val action: String? = null
)

/** Dynamic state labels used by the main clan dashboard. */
@Serializable
data class MainMenuDisplayConfig(
    @YamlComment("Статус открытого кланового хранилища")
    val chestOpen: String = "&#5EFD7DОткрыт",

    @YamlComment("Статус закрытого кланового хранилища")
    val chestClosed: String = "&#FC3737Закрыт",

    @YamlComment("Значение баланса без права на просмотр")
    val hiddenBalance: String = "&#FC3737Скрыт",

    @YamlComment("Название опасного действия для главы клана")
    val leaderLeaveName: String = "&#FC3737✖ Распустить клан",

    @YamlComment("Предупреждение опасного действия для главы клана")
    val leaderLeaveWarning: String = "Клан будет удалён навсегда.",

    @YamlComment("Название опасного действия для участника клана")
    val memberLeaveName: String = "&#FC3737✖ Покинуть клан",

    @YamlComment("Предупреждение опасного действия для участника клана")
    val memberLeaveWarning: String = "Вы потеряете доступ к клану и его функциям."
)

/** Configuration of the clan Hall of Fame ranking screen. */
@Serializable
data class TopMenuConfig(
    val title: String = "&#FC65DF« Зал Славы » &7({page}/{pages})",
    val rows: Int = 6,
    val items: Map<String, GuiItemConfig> = emptyMap(),
    @YamlComment("Слоты карточек кланов на одной странице")
    val entrySlots: List<Int> = listOf(20, 21, 22, 23, 24, 29, 30, 31, 32, 33),
    @YamlComment("Материал клана на первом месте")
    val firstMaterial: String = "DRAGON_EGG",
    @YamlComment("Материал клана на втором месте")
    val secondMaterial: String = "NETHER_STAR",
    @YamlComment("Материал клана на третьем месте")
    val thirdMaterial: String = "DIAMOND",
    @YamlComment("Материал остальных кланов в рейтинге")
    val otherMaterial: String = "AMETHYST_SHARD",
    @YamlComment("Цвет первого места")
    val firstColor: String = "&#FFD700",
    @YamlComment("Цвет второго места")
    val secondColor: String = "&#C0C0C0",
    @YamlComment("Цвет третьего места")
    val thirdColor: String = "&#CD7F32",
    @YamlComment("Цвет остальных мест")
    val otherColor: String = "&#A9A9A9"
)

/**
 * 100% Config-Driven Menu Design.
 * Every single title, item slot, material, display name, lore line, glow effect,
 * decorative border glass pane, and click action is configured in menus.yml.
 */
@Serializable
class MenusConfig {

    @YamlComment("Общий декоративный фон. Каждый GUI подключает его явно")
    val background: GuiBackgroundConfig = GuiBackgroundConfig()

    @YamlComment("Меню для игрока, который пока не состоит в клане")
    val noClanMenu: GuiMenuConfig = defaultNoClanMenu()

    @YamlComment("Меню выбора визуальной метки соклановцев")
    val clanColorMenu: GuiMenuConfig = defaultClanColorMenu()

    @YamlComment("История операций казны")
    val treasuryHistoryMenu: TreasuryHistoryMenuConfig = TreasuryHistoryMenuConfig()

    @YamlComment("Главное меню клана (/clan menu)")
    val mainMenu: MainMenuConfig = MainMenuConfig(
        title = "&#FC7D37« Штаб Клана »",
        rows = 6,
        items = mapOf(
            "stats" to GuiItemConfig(
                slot = 13,
                material = "NETHER_STAR",
                name = "&#FC7D37✦ {clan}",
                lore = listOf(
                    "",
                    "&#9EFC65 «Командный профиль»",
                    " &7- &fУровень: &#5EFD7D{clan_level}",
                    " &7- &fРейтинг MMR: &#FFD700{clan_mmr}",
                    " &7- &fУчастники: &#5EA9FD{clan_online} &7/ &f{clan_members}",
                    "",
                    "&#FC65DF «Боевая сводка»",
                    " &7- &fУбийств: &#5EFD7D{clan_kills}",
                    " &7- &fСмертей: &#FC3737{clan_deaths}",
                    " &7- &fБитвы: &#5EFD7D{clan_battle_wins} побед &7/ &#FC3737{clan_battle_losses} поражений",
                    " &7- &fK/D: &e{clan_kda}",
                    "",
                    "&#FF8702➥ &fВыберите модуль управления ниже"
                ),
                glow = true
            ),
            "members" to GuiItemConfig(
                slot = 20,
                material = "ARMOR_STAND",
                name = "&#FC7D37☷ Состав клана",
                lore = listOf(
                    "",
                    "&#9EFC65 «Участники»",
                    " &7- &fВсего в клане: &#5EA9FD{clan_members}",
                    " &7- &fСейчас в сети: &#5EFD7D{clan_online}",
                    "",
                    "&#FC65DF «Управление»",
                    " &7- &fРоли, повышение, понижение",
                    " &7- &fи исключение участников.",
                    "",
                    "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы открыть состав"
                ),
                actions = listOf(SoundAction("UI_BUTTON_CLICK"))
            ),
            "chest" to GuiItemConfig(
                slot = 21,
                material = "BARREL",
                name = "&#FFD700❂ Хранилище клана",
                lore = listOf(
                    "",
                    "&#9EFC65 «Склад»",
                    " &7- &fДоступ: {chest_state}",
                    " &7- &fВместимость: &e{chest_slots} &fслотов",
                    "",
                    "&#FC65DF «Назначение»",
                    " &7- &fОбщее хранилище ресурсов",
                    " &7- &fдля всех участников клана.",
                    "",
                    "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы открыть склад"
                ),
                actions = listOf(SoundAction("BLOCK_BARREL_OPEN"))
            ),
            "treasury" to GuiItemConfig(
                slot = 22,
                material = "GOLD_INGOT",
                name = "&#FFD700⛁ Казна клана",
                lore = listOf(
                    "",
                    "&#9EFC65 «Финансы»",
                    " &7- &fБаланс: {clan_balance_animated} ⛁",
                    " &7- &fСтатус: &eОбщий счёт клана",
                    "",
                    "&#FC65DF «Назначение»",
                    " &7- &fПополнение, снятие и журнал",
                    " &7- &fфинансовых операций.",
                    "",
                    "&#FF8702➥ &fНажмите, &eЛКМ &fдля управления казной"
                ),
                actions = listOf(SoundAction("UI_BUTTON_CLICK"))
            ),
            "homes" to GuiItemConfig(
                slot = 23,
                material = "RED_BED",
                name = "&#FC7D37⌂ Клановые дома",
                lore = listOf(
                    "",
                    "&#9EFC65 «Точки телепортации»",
                    " &7- &fУстановлено: &e{homes_set} &7/ &f{homes_unlocked}",
                    " &7- &fВсего точек: &#5EA9FD{homes_total}",
                    "",
                    "&#FC65DF «Назначение»",
                    " &7- &fОбщие точки сбора, базы",
                    " &7- &fи быстрых перемещений клана.",
                    "",
                    "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы открыть дома"
                ),
                actions = listOf(SoundAction("UI_BUTTON_CLICK"))
            ),
            "invite" to GuiItemConfig(
                slot = 24,
                material = "WRITABLE_BOOK",
                name = "&#5EA9FD✉ Пригласить игрока",
                lore = listOf(
                    "",
                    "&#9EFC65 «Новый участник»",
                    " &7- &fНикнейм вводится в чат.",
                    " &7- &fВремя на ввод: &e{prompt_seconds} сек.",
                    "",
                    "&#FC65DF «Проверки»",
                    " &7- &fПроверяется клан игрока, лимит",
                    " &7- &fучастников и ваши права доступа.",
                    "",
                    "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы пригласить"
                ),
                actions = listOf(SoundAction("UI_BUTTON_CLICK"))
            ),
            "top" to GuiItemConfig(
                slot = 29,
                material = "DRAGON_EGG",
                name = "&#FC65DF♛ Зал славы",
                lore = listOf(
                    "",
                    "&#9EFC65 «Рейтинг серверов»",
                    " &7- &fСравните силу кланов по MMR",
                    " &7- &fи боевой статистике.",
                    "",
                    "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы открыть рейтинг"
                ),
                actions = listOf(SoundAction("UI_BUTTON_CLICK"))
            ),
            "upgrade" to GuiItemConfig(
                slot = 31,
                material = "BEACON",
                name = "&#FC7D37✦ Эволюция клана",
                lore = listOf(
                    "",
                    "&#9EFC65 «Прогресс»",
                    " &7- &fТекущий уровень: &e{clan_level} лвл.",
                    " &7- &fMMR клана: &#FFD700{clan_mmr}",
                    " &7- &fКазна: &e{clan_balance}⛁",
                    "",
                    "&#FC65DF «Награды прокачки»",
                    " &7- &fНовые слоты участников и складов,",
                    " &7- &fновые клановые дома и преимущества.",
                    "",
                    "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы улучшить клан"
                ),
                glow = true
            ),
            "settings" to GuiItemConfig(
                slot = 32,
                material = "COMPARATOR",
                name = "&#FC7D37⚙ Настройки клана",
                lore = listOf(
                    "",
                    "&#9EFC65 «Системы клана»",
                    " &7- &fPvP, чат, склад и оповещения.",
                    " &7- &fПрава и иерархия ролей.",
                    "",
                    "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы открыть настройки"
                ),
                actions = listOf(SoundAction("UI_BUTTON_CLICK"))
            ),
            "help" to GuiItemConfig(
                slot = 40,
                material = "ENCHANTED_BOOK",
                name = "&#5EA9FD⌕ Помощь по клану",
                lore = listOf(
                    "",
                    "&#9EFC65 «С чего начать»",
                    " &7- &fНайдите тиммейтов через &e/clan",
                    " &7- &fи пригласите их сюда.",
                    " &7- &fУбивайте врагов и копите MMR.",
                    "",
                    "&#FC65DF «Что открыто»",
                    " &7- &fУровень {clan_level}: {clan_slots} участников,",
                    " &7- &f{clan_homes} точек домов и {clan_kills} убийств.",
                    "",
                    "&#FF8702➥ &fОткройте подробную инструкцию"
                )
            ),
            "leave" to GuiItemConfig(
                slot = 33,
                material = "RED_DYE",
                name = "{leave_name}",
                lore = listOf(
                    "",
                    "&#FC3737 «Внимание»",
                    " &7- &f{leave_warning}",
                    " &7- &fДействие потребует подтверждения.",
                    "",
                    "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы продолжить"
                )
            ),
            "shop" to GuiItemConfig(
                slot = 30,
                material = "EMERALD",
                name = "&#5EFD7D✦ Клановый магазин",
                lore = listOf("", "&#9EFC65 «Награды за развитие»", " &7- &fКлановые очки: &#FFD700{clan_points}", " &7- &fПредметы, бонусы и редкие награды.", "", "&#FC65DF «Оплата на выбор»", " &7- &fОчки клана, Vault или PlayerPoints.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы открыть магазин"),
                actions = listOf(SoundAction("UI_BUTTON_CLICK"))
            ),
            "quests" to GuiItemConfig(
                slot = 34,
                material = "WRITABLE_BOOK",
                name = "&#5EA9FD✎ Клановые квесты",
                lore = listOf("", "&#9EFC65 «Общие цели»", " &7- &fСражайтесь, исследуйте и развивайтесь.", " &7- &fОткрывайте награды для всего клана.", "", "&#FC65DF «Вклад каждого важен»", " &7- &fЛюбое действие приближает общую цель.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы открыть квесты"),
                actions = listOf(SoundAction("UI_BUTTON_CLICK"))
            ),
            "battles" to GuiItemConfig(
                slot = 35,
                material = "CROSSBOW",
                name = "&#FC3737⚔ Клановые битвы",
                lore = listOf(
                    "",
                    "&#9EFC65 «Честный вызов»",
                    " &7- &fСравните MMR и выберите соперника.",
                    " &7- &fСражайтесь на отдельной арене.",
                    "",
                    "&#FC65DF «Награды»",
                    " &7- &fПобеды приносят MMR и клановые очки.",
                    " &7- &fБои продвигают боевые квесты.",
                    "",
                    "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы открыть поле боя"
                ),
                actions = listOf(SoundAction("ITEM_CROSSBOW_LOADING_START"))
            )
        )
    )

    @YamlComment("Подробная инструкция по функциям клана и режиму прокачки")
    val helpMenu: GuiMenuConfig = GuiMenuConfig(
        title = "&#5EA9FD« Справка Клана »",
        rows = 6,
        items = mapOf(
            "evolution" to GuiItemConfig(slot = 13, material = "BEACON", name = "&#FC7D37✦ Путь эволюции", lore = listOf("", "&#9EFC65 «Как прокачиваться»", " &7- &fПовышайте MMR в боях и сражениях.", " &7- &fКопите казну, выполняйте квесты.", " &7- &fКаждый уровень открывает новые слоты,", " &7- &fдома и уникальные перки клана.", "", "&#FF8702➥ &fТекущая задача: достичь уровня &e{next_level}"), glow = true),
            "rewards" to GuiItemConfig(slot = 20, material = "TOTEM_OF_UNDYING", name = "&#FC65DF✦ Награды за уровни", lore = listOf("", "&#9EFC65 «Бонусы прокачки»", " &7- &f+5 слотов участников за уровень.", " &7- &f+1 ряд в клановом сундуке за уровень.", " &7- &f+10 точек домов на максимальном уровне.", " &7- &f+Кастомные эффекты над головой.", "", "&#FF8702➥ &fПланируйте апгрейды")),
            "earning" to GuiItemConfig(slot = 22, material = "EXPERIENCE_BOTTLE", name = "&#5EFD7D✦ Источники очков", lore = listOf("", "&#9EFC65 «Где брать прогресс»", " &7- &fУбийства врагов: +10 MMR.", " &7- &fСмерти: -5 MMR (но не меньше 0).", " &7- &fПобеда в клановом бою: до +30 MMR.", " &7- &fАктивность в чате: ежедневный бонус.", "", "&#FF8702➥ &fСледите за статистикой в Зале славы")),
            "back" to GuiItemConfig(slot = 49, material = "RED_CANDLE", name = "&#FC3737⏎ Вернуться в штаб", lore = listOf("", "&#FC65DF «Переход»", " &7- &fОткрывает главное меню клана.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы вернуться"), actions = listOf(OpenGuiAction("MAIN")))
        )
    )

    @YamlComment("Окно подтверждения выхода из клана или его расформирования")
    val leaveConfirmMenu: GuiMenuConfig = GuiMenuConfig(
        title = "&#FC3737« Подтверждение »",
        rows = 3,
        items = mapOf(
            "confirmDisband" to GuiItemConfig(slot = 11, material = "RED_DYE", name = "&#FC3737✖ Распустить клан", lore = listOf("", "&#FC3737 «Подтверждение»", " &7- &fКлан &#5EA9FD{clan} &fбудет удалён навсегда.", " &7- &fЭто действие нельзя отменить.", "", "&#FC3737➥ &fНажмите, &eЛКМ &fчтобы подтвердить")),
            "confirmLeave" to GuiItemConfig(slot = 11, material = "RED_DYE", name = "&#FC3737✖ Покинуть клан", lore = listOf("", "&#FC3737 «Подтверждение»", " &7- &fВы покинете клан &#5EA9FD{clan}&f.", " &7- &fПрава и доступы будут потеряны.", "", "&#FC3737➥ &fНажмите, &eЛКМ &fчтобы подтвердить")),
            "info" to GuiItemConfig(slot = 13, material = "ENCHANTED_BOOK", name = "&#FFD700❂ Важное решение", lore = listOf("", "&#9EFC65 «Проверьте действие»", " &7- &fПеред подтверждением убедитесь,", " &7- &fчто выбрали правильное решение.", "", "&#FC65DF «Безопасность»", " &7- &fДля возврата выберите отмену справа.")),
            "battleWarningLeader" to GuiItemConfig(slot = 0, material = "PAPER", name = "&#FC3737Активная битва — роспуск", lore = listOf("", "&#FC3737 «Активная битва»", " &7- &fРоспуск немедленно завершит бой.", " &7- &fКлану засчитают техническое поражение", " &7- &fи снимут MMR.", "")),
            "battleWarningMember" to GuiItemConfig(slot = 0, material = "PAPER", name = "&#FC3737Активная битва — выход", lore = listOf("", "&#FC3737 «Активная битва»", " &7- &fПосле выхода вы больше не сможете", " &7- &fучаствовать в текущей битве.", "")),
            "cancel" to GuiItemConfig(slot = 15, material = "LIME_CANDLE", name = "&#5EFD7D↶ Отменить действие", lore = listOf("", "&#9EFC65 «Безопасный возврат»", " &7- &fНичего не будет изменено.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы вернуться"))
        )
    )

    @YamlComment("Меню участников клана")
    val membersMenu: GuiMenuConfig = GuiMenuConfig(
        title = "&#FC7D37« Состав Клана »",
        rows = 6,
        items = mapOf(
            "member" to GuiItemConfig(slot = 0, material = "NAME_TAG", name = "&#FC7D37{player} &7• {role}", lore = listOf("", "&#9EFC65 «Профиль»", " &7- &fДолжность: &#5EA9FD{role}", " &7- &fСтатус: {status}", " &7- &fИерархия: &e{weight}", "", "&#5EA9FD «Боевая статистика в клане»", " &7- &fФормат: &#5EFD7Dубийства &7/ &#FC3737смерти", " &7- &fСегодня: &#5EFD7D{kills_today} &7/ &#FC3737{deaths_today}", " &7- &fНеделя: &#5EFD7D{kills_week} &7/ &#FC3737{deaths_week}", " &7- &fМесяц: &#5EFD7D{kills_month} &7/ &#FC3737{deaths_month}", " &7- &fВсё время: &#5EFD7D{kills_total} &7/ &#FC3737{deaths_total}", "&8Периоды рассчитываются по UTC.", "", "&#FC65DF «Управление»", " &7- &fЛКМ: {action_promote}", " &7- &fПКМ: &#FC3737Понизить в должности", " &7- &fShift+ЛКМ: &#5EA9FDПерсональные права игрока", " &7- &fShift+ПКМ: &#FC3737Исключить из клана", "", "&#FF8702➥ &fИспользуйте клики для управления")),
            "member_self" to GuiItemConfig(slot = 0, material = "NAME_TAG", name = "&#FC7D37{player} &7• {role}", lore = listOf("", "&#9EFC65 «Профиль»", " &7- &fДолжность: &#5EA9FD{role}", " &7- &fСтатус: {status}", " &7- &fИерархия: &e{weight}", "", "&#5EA9FD «Боевая статистика в клане»", " &7- &fФормат: &#5EFD7Dубийства &7/ &#FC3737смерти", " &7- &fСегодня: &#5EFD7D{kills_today} &7/ &#FC3737{deaths_today}", " &7- &fНеделя: &#5EFD7D{kills_week} &7/ &#FC3737{deaths_week}", " &7- &fМесяц: &#5EFD7D{kills_month} &7/ &#FC3737{deaths_month}", " &7- &fВсё время: &#5EFD7D{kills_total} &7/ &#FC3737{deaths_total}", "&8Периоды рассчитываются по UTC.", "", "&#FC65DF «Личный профиль»", " &7- &fПолный отчёт: &#FF8702/clan stats")),
            "member_no_permission" to GuiItemConfig(slot = 0, material = "NAME_TAG", name = "&#FC7D37{player} &7• {role}", lore = listOf("", "&#9EFC65 «Профиль»", " &7- &fДолжность: &#5EA9FD{role}", " &7- &fСтатус: {status}", " &7- &fИерархия: &e{weight}", "", "&#5EA9FD «Боевая статистика в клане»", " &7- &fФормат: &#5EFD7Dубийства &7/ &#FC3737смерти", " &7- &fСегодня: &#5EFD7D{kills_today} &7/ &#FC3737{deaths_today}", " &7- &fНеделя: &#5EFD7D{kills_week} &7/ &#FC3737{deaths_week}", " &7- &fМесяц: &#5EFD7D{kills_month} &7/ &#FC3737{deaths_month}", " &7- &fВсё время: &#5EFD7D{kills_total} &7/ &#FC3737{deaths_total}", "&8Периоды рассчитываются по UTC.", "", "&#FC65DF «Просмотр»", " &7- &fПолный отчёт: &#FF8702/clan stats {player}", "", "&c➥ У вас недостаточно прав для управления")),
            "previous" to GuiItemConfig(slot = 47, material = "ARROW", name = "&#5EA9FD◀ Предыдущая страница", lore = listOf("", "&#9EFC65 «Навигация»", " &7- &fСтраница: &e{page} &7/ &f{pages}", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы открыть")),
            "back" to GuiItemConfig(slot = 49, material = "RED_CANDLE", name = "&#FC3737⏎ Вернуться в меню", lore = listOf("", "&#FC65DF «Переход»", " &7- &fОткрывает главное меню клана.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы вернуться"), actions = listOf(OpenGuiAction("MAIN"))),
            "next" to GuiItemConfig(slot = 51, material = "SPECTRAL_ARROW", name = "&#5EA9FDСледующая страница ▶", lore = listOf("", "&#9EFC65 «Навигация»", " &7- &fСтраница: &e{page} &7/ &f{pages}", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы открыть"), glow = true)
        )
    )

    @YamlComment("Меню настроек клана")
    val settingsMenu: GuiMenuConfig = GuiMenuConfig(
        title = "&#FC7D37« Настройки Клана »",
        rows = 6,
        items = mapOf(
            "overview" to GuiItemConfig(slot = 13, material = "COMPARATOR", name = "&#FC7D37✦ Панель управления кланом", lore = listOf("", "&#9EFC65 «Сводка»", " &7- &fУчастников: &e{members}", " &7- &fОнлайн сейчас: &#5EFD7D{online}", " &7- &fВаш ранг: &#5EA9FD{role}", "", "&#FC65DF «Назначение»", " &7- &fЗдесь настраиваются режимы клана,", " &7- &fдоступы к функциям и права ролей.", "", "&#FF8702➥ &fВыберите модуль ниже"), glow = true),
            "pvp" to GuiItemConfig(slot = 20, material = "DIAMOND_SWORD", name = "&#FC7D37⚔ Режим PvP", lore = listOf("", "&#9EFC65 «Состояние»", " &7- &fFriendly Fire: {state}", " &7- &fУрон по своим: {pvp_damage}", "", "&#FC65DF «Описание»", " &7- &fКонтролирует боевой режим", " &7- &fмежду участниками клана.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы {action}")),
            "chat" to GuiItemConfig(slot = 21, material = "WRITABLE_BOOK", name = "&#5EA9FD✎ Клановый чат", lore = listOf("", "&#9EFC65 «Состояние»", " &7- &fКанал общения: {state}", " &7- &fТип: &eВнутриигровой", "", "&#FC65DF «Описание»", " &7- &fЗакрытый чат для общения", " &7- &fтолько между соклановцами.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы {action}")),
            "chest" to GuiItemConfig(slot = 22, material = "BARREL", name = "&#FFD700❂ Доступ к складу", lore = listOf("", "&#9EFC65 «Состояние»", " &7- &fОбщее хранилище: {state}", " &7- &fРесурсы клана: &eПод контролем", "", "&#FC65DF «Описание»", " &7- &fРазрешает или блокирует", " &7- &fдоступ к виртуальному складу.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы {action}")),
            "join" to GuiItemConfig(slot = 23, material = "BELL", name = "&#FC65DF✦ Оповещения входа", lore = listOf("", "&#9EFC65 «Состояние»", " &7- &fУведомления: {state}", " &7- &fОнлайн: &e{online}&7/&f{members}", "", "&#FC65DF «Описание»", " &7- &fСообщает о входе и выходе", " &7- &fучастников клана.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы {action}")),
            "color" to GuiItemConfig(slot = 30, material = "LIME_DYE", name = "&#5EFD7D✦ Метка соклановцев", lore = listOf("", "&#9EFC65 «Сводка»", " &7- &fСтатус: &e{clan_highlight_status}", " &7- &fТип: &e{clan_highlight_type}", " &7- &fТекущий цвет: &e{clan_color}", "", "&#FC65DF «Описание»", " &7- &fВизуальная метка: цветная броня", " &7- &fили светящийся контур.", " &7- &fНастоящий инвентарь не меняется.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы открыть выбор")),
            "roles" to GuiItemConfig(slot = 24, material = "NETHER_STAR", name = "&#FC7D37✵ Управление ролями", lore = listOf("", "&#9EFC65 «Права доступа»", " &7- &fВсего ролей: &e{roles}", " &7- &fУчастников: &e{members}", " &7- &fВаш ранг: &b{role}", "", "&#FC65DF «Описание»", " &7- &fРедактор прав, должностей", " &7- &fи управленческих доступов.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы открыть редактор")),
            "hint" to GuiItemConfig(slot = 31, material = "ENCHANTED_BOOK", name = "&#FFD700❂ Подсказка по доступам", lore = listOf("", "&#9EFC65 «Важно»", " &7- &fЕсли игрок не может нажать кнопку,", " &7- &fпроверьте права его роли.", "", "&#FC65DF «Роли»", " &7- &fГлава может открыть редактор ролей", " &7- &fи выдать точечные разрешения.", "", "&#FF8702➥ &fИспользуйте &eУправление ролями")),
            "back" to GuiItemConfig(slot = 49, material = "RED_CANDLE", name = "&#FC3737⏎ Вернуться в меню", lore = listOf("", "&#FC65DF «Переход»", " &7- &fОткрывает главное меню клана.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы вернуться"), actions = listOf(OpenGuiAction("MAIN")))
        )
    )

    @YamlComment("Редактор ролей")
    val editorRolesMenu: GuiMenuConfig = GuiMenuConfig(
        title = "&#FC7D37« Редактор Ролей »",
        rows = 6,
        items = mapOf(
            "overview" to GuiItemConfig(slot = 13, material = "LECTERN", name = "&#FC7D37✦ Архитектура ролей", lore = listOf("", "&#9EFC65 «Система доступа»", " &7- &fРолей для настройки: &e{roles}", " &7- &fУчастников клана: &e{members}", "", "&#FC65DF «Как работает»", " &7- &fВыберите должность ниже,", " &7- &fчтобы открыть матрицу прав.", "", "&#FF8702➥ &fВыберите роль для настройки"), glow = true),
            "role" to GuiItemConfig(slot = 20, material = "IRON_INGOT", name = "&#FC7D37✦ Роль: {role}", lore = listOf("", "&#9EFC65 «Профиль роли»", " &7- &fИерархия: &#FFD700#{weight}", " &7- &fБазовых прав: &e{permissions}", "", "&#FC65DF «Настройка»", " &7- &fОткроет список разрешений", " &7- &fдля выбранной должности.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы настроить")),
            "permission" to GuiItemConfig(slot = 10, material = "PAPER", name = "&#FC7D37Право: &f{permission}", lore = listOf("", "&#9EFC65 «Состояние»", " &7- &fСтатус: {state}", " &7- &fРоль: &#5EA9FD{role}", "", "&#FC65DF «Описание»", "{description}", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы {action}")),
            "back" to GuiItemConfig(slot = 49, material = "RED_CANDLE", name = "&#FC3737⏎ Вернуться в настройки", lore = listOf("", "&#FC65DF «Переход»", " &7- &fВозвращает в настройки клана.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы вернуться"), actions = listOf(OpenGuiAction("SETTINGS")))
        )
    )

    @YamlComment("Персональные права участника")
    val userPermissionsMenu: GuiMenuConfig = GuiMenuConfig(
        title = "&#FC7D37« Права Игрока »",
        rows = 5,
        items = mapOf(
            "permission" to GuiItemConfig(slot = 10, material = "PAPER", name = "&#FC7D37Право: &f{permission}", lore = listOf("", "&#9EFC65 «Состояние»", " &7- &fСтатус: {state}", " &7- &fРоль: &#5EA9FD{role}", "", "&#FC65DF «Описание»", "{description}", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы {action}")),
            "back" to GuiItemConfig(slot = 36, material = "RED_CANDLE", name = "§x§F§F§0§0§0§0Вернуться", lore = listOf("&7Вернуться к участникам"), actions = listOf(OpenGuiAction("MEMBERS")))
        )
    )

    @YamlComment("Меню банка и финансов клана")
    val treasuryMenu: GuiMenuConfig = GuiMenuConfig(
        title = "&#FC7D37« Финансы Клана »",
        rows = 6,
        items = mapOf(
            "center" to GuiItemConfig(slot = 13, material = "GOLD_BLOCK", name = "&#FFD700Центральный Банк Клана", lore = listOf("", "&#9EFC65 «Счет»", " &7- &fТекущий баланс: &#5EFD7D{balance} ⛁", " &7- &fВладелец: &e{clan}", "", "&#FC65DF «Информация»", " &7- &fСредства используются для", " &7- &fпрокачки клана и покупок.", "", "&#FF8702➥ &fПополнение и снятие ниже"), glow = true),
            "deposit" to GuiItemConfig(slot = 20, material = "EMERALD", name = "&#5EFD7DПополнить Казну", lore = listOf("", "&#9EFC65 «Внос средств»", " &7- &fВвести произвольную сумму", " &7- &fв наковальне для пополнения.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы пополнить")),
            "withdraw" to GuiItemConfig(slot = 24, material = "REDSTONE", name = "&#FC65DFСнять с Казны", lore = listOf("", "&#9EFC65 «Снятие средств»", " &7- &fВвести произвольную сумму", " &7- &fв наковальне для снятия.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы снять")),
            "depositPresets" to GuiItemConfig(slot = 28, material = "LIME_DYE", name = "&#5EFD7D✚ Быстрое пополнение", lore = listOf("", "&#9EFC65 «Суммы из конфига»", " &7- &f+500⛁, +1000⛁", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы внести выбранную сумму")),
            "withdrawPresets" to GuiItemConfig(slot = 34, material = "ORANGE_DYE", name = "&#FC3737✖ Быстрое снятие", lore = listOf("", "&#9EFC65 «Суммы из конфига»", " &7- &f-500⛁, -1000⛁", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы снять выбранную сумму")),
            "history" to GuiItemConfig(slot = 40, material = "WRITABLE_BOOK", name = "&#5EA9FDИстория операций", lore = listOf("", "&#9EFC65 «Информация»", " &7- &fПросмотр истории пополнений и снятий", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы открыть лог")),
            "back" to GuiItemConfig(slot = 49, material = "OAK_DOOR", name = "&#FC3737⏎ Вернуться в меню", lore = listOf("", "&#FC65DF «Переход»", " &7- &fВозврат в главное управление клана.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы вернуться"), actions = listOf(OpenGuiAction("MAIN")))
        )
    )

    @YamlComment("Меню эволюции и уровней клана")
    val upgradeMenu: GuiMenuConfig = GuiMenuConfig(
        title = "&#FC7D37« Эволюция Клана »",
        rows = 6,
        items = mapOf(
            "overview" to GuiItemConfig(slot = 13, material = "EXPERIENCE_BOTTLE", name = "&#FC7D37✦ Путь эволюции клана", lore = listOf("", "&#9EFC65 «Сводка прокачки»", " &7- &fТекущий уровень: &e{clan_level} &7/ &f5", " &7- &fСледующая цель: &6{next_level}", " &7- &fСтоимость: &e{clan_required_money}⛁", " &7- &fТребуемый MMR: &#FFD700{clan_required_mmr}", " &7- &fЗавершения квестов: &3{clan_quests} &7/ &3{clan_required_quests}", "", "&#FC65DF «Награда за уровень»", " &7- &fСлотов участников: &b{clan_slots}", " &7- &fРядов сундука: &e{clan_chest_rows}", " &7- &fКлановых домов: &#5EA9FD{clan_homes}", "", "&#FF8702➥ &fАнимированный {beacon_state} показывает состояние ритуала"), glow = true),
            "level" to GuiItemConfig(slot = 20, material = "COAL", name = "&#5EA9FDЭтап {level}: {level_title}", lore = listOf("", "&#9EFC65 «Статус»", " &7- &fСостояние: {level_state}", " &7- &fЛимит участников: &b{level_max_members}", " &7- &fРяды сундука: &e{level_chest_rows}", "", "&#FC65DF «Уникальный перк»", " &7- &f{level_perk}", "", "&#5EA9FD «Требования для перехода»", " &7- &fКазна: &e{level_cost}⛁", " &7- &fMMR: &#FFD700{level_required_mmr}", " &7- &fНужно завершений: &3{level_required_quests}")),
            "upgrade" to GuiItemConfig(slot = 40, material = "BEACON", name = "{beacon_title}", lore = listOf("", "&#9EFC65 «Текущий прогресс»", " &7- &fКазна: {clan_money_color}{clan_money} &7/ &e{clan_required_money} ⛁", " &7- &fMMR: {clan_mmr_color}{clan_mmr} &7/ &6{clan_required_mmr}", " &7- &fЗавершения квестов: {clan_quests_color}{clan_quests} &7/ &3{clan_required_quests}", "", "&#FC65DF «Состояние ритуала»", " &7- &f{beacon_state}", "", "{beacon_action}"), glow = true),
            "back" to GuiItemConfig(slot = 49, material = "RED_CANDLE", name = "&#FC3737⏎ Вернуться в штаб", lore = listOf("", "&#FC65DF «Переход»", " &7- &fОткрывает главный штаб клана.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы вернуться"), actions = listOf(OpenGuiAction("MAIN")))
        )
    )

    @YamlComment("Зал Славы / Топ Кланов")
    val topMenu: TopMenuConfig = TopMenuConfig(
        title = "&#FC65DF« Зал Славы » &7({page}/{pages})",
        rows = 6,
        items = mapOf(
            "overview" to GuiItemConfig(slot = 13, material = "BEACON", name = "&#FC65DF♛ Рейтинг кланов", lore = listOf("", "&#9EFC65 «Сводка рейтинга»", " &7- &fКланов в таблице: &e{total}", " &7- &fТекущая страница: &#5EA9FD{page} &7/ &f{pages}", "", "&#FC65DF «Как считается»", " &7- &fГлавный параметр: &#FFD700MMR", " &7- &fПри равенстве учитываются убийства.", "", "&#FF8702➥ &fЛучшие кланы отмечены особыми иконками"), glow = true),
            "entry" to GuiItemConfig(slot = 20, material = "AMETHYST_SHARD", name = "{rank_color}#{rank} &7• &#FC7D37{clan}", lore = listOf("", "&#9EFC65 «Позиция в рейтинге»", " &7- &fМесто: {rank_color}#{rank}", " &7- &fЛидер: &#5EA9FD{leader}", " &7- &fУровень клана: &#5EFD7D{level}", "", "&#FC65DF «Боевая мощь»", " &7- &fРейтинг MMR: &#FFD700{mmr}", " &7- &fУбийств: &#5EFD7D{kills} &7/ &fСмертей: &#FC3737{deaths}", " &7- &fОбщий K/D: &e{kda}", "", "&#5EA9FD «Состав»", " &7- &fУчастников: &e{members}", " &7- &fКазна: &#5EFD7D{balance}⛁")),
            "empty" to GuiItemConfig(slot = 22, material = "KNOWLEDGE_BOOK", name = "&#5EA9FD⌁ Рейтинг пока пуст", lore = listOf("", "&#9EFC65 «Зал ожидает легенд»", " &7- &fСоздайте клан, развивайте его", " &7- &fи заработайте первые очки MMR.", "", "&#FF8702➥ &fПервый клан займёт вершину рейтинга")),
            "previous" to GuiItemConfig(slot = 47, material = "ARROW", name = "&#5EA9FD◀ Выше по рейтингу", lore = listOf("", "&#9EFC65 «Навигация»", " &7- &fСтраница: &e{page} &7/ &f{pages}", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы открыть")),
            "previousLocked" to GuiItemConfig(slot = 47, material = "GRAY_DYE", name = "&#FC3737◀ Выше по рейтингу", lore = listOf("", "&#FC3737 «Недоступно»", " &7- &fВы уже на первой странице.", "", "&c➥ Листать назад нельзя")),
            "back" to GuiItemConfig(slot = 49, material = "RED_CANDLE", name = "&#FC3737⏎ Вернуться в штаб", lore = listOf("", "&#FC65DF «Переход»", " &7- &fОткрывает главный штаб клана.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы вернуться"), actions = listOf(OpenGuiAction("MAIN"))),
            "next" to GuiItemConfig(slot = 51, material = "SPECTRAL_ARROW", name = "&#5EA9FDНиже по рейтингу ▶", lore = listOf("", "&#9EFC65 «Навигация»", " &7- &fСтраница: &e{page} &7/ &f{pages}", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы открыть"), glow = true),
            "nextLocked" to GuiItemConfig(slot = 51, material = "GRAY_DYE", name = "&#FC3737Ниже по рейтингу ▶", lore = listOf("", "&#FC3737 «Недоступно»", " &7- &fВы уже на последней странице.", "", "&c➥ Листать вперёд нельзя"))
        )
    )

    @YamlComment("Виртуальное хранилище сундук клана")
    val chestMenu: GuiMenuConfig = GuiMenuConfig(
        title = "&#FC7D37« Хранилище Клана »",
        rows = 6,
        items = mapOf(
            "lockedSlot" to GuiItemConfig(slot = 0, material = "RED_STAINED_GLASS_PANE", name = "&#FF3B3B🔒 СЛОТ ЗАБЛОКИРОВАН", lore = listOf("", "&#9EFC65 «Информация»", " &7- &fСтатус: &#FC3737Закрыт для хранения", " &7- &fТребуется уровень клана: &e{level} лвл.", "", "&#FC65DF «Как разблокировать?»", " &7- &fКаждый уровень клана открывает", " &7- &fдополнительно &e9 новых слотов&f!", "", "&#FF8702➥ &fНажмите &eЭволюция Клана &fдля прокачки!")),
            "stats" to GuiItemConfig(slot = 45, material = "KNOWLEDGE_BOOK", name = "&#5EFD7D⌁ Аналитика склада", lore = listOf("", "&#9EFC65 «Заполненность»", " &7- &fЗанято слотов: &e{stored} &7/ &f{slots}", " &7- &fЗагрузка: {progress} &7(&e{percent}%&7)", "", "&#FC65DF «Финансы»", " &7- &fКазна клана: &#5EFD7D{balance}⛁")),
            "decor_46" to GuiItemConfig(slot = 46, material = "BLACK_STAINED_GLASS_PANE", name = " "),
            "decor_47" to GuiItemConfig(slot = 47, material = "BLACK_STAINED_GLASS_PANE", name = " "),
            "back" to GuiItemConfig(slot = 48, material = "RED_CANDLE", name = "&#FC3737⏎ Вернуться в меню", lore = listOf("", "&#FC65DF «Переход»", " &7- &fСохраняет содержимое склада.", " &7- &fОткрывает главное меню клана.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы вернуться"), actions = listOf(OpenGuiAction("MAIN"))),
            "core" to GuiItemConfig(slot = 49, material = "BEACON", name = "&#FC7D37✦ Ядро хранилища", lore = listOf("", "&#9EFC65 «Статус»", " &7- &fУровень клана: &e{level}", " &7- &fДоступно рядов: &b{rows} &7/ &f5", " &7- &fСохранение данных: &#5EFD7DАктивно", "", "&#FC65DF «Назначение»", " &7- &fЦентральный модуль склада.", " &7- &fПоказывает текущую вместимость."), glow = true),
            "upgrade" to GuiItemConfig(slot = 50, material = "NETHER_STAR", name = "&#FC65DF✵ Эволюция склада", lore = listOf("", "&#9EFC65 «Прокачка»", " &7- &fКаждый уровень открывает", " &7- &fновые ряды хранилища.", "", "&#FC65DF «Сейчас»", " &7- &fДоступно слотов: &e{slots}", " &7- &fУровень клана: &e{level}", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы улучшить"), glow = true, actions = listOf(OpenGuiAction("UPGRADE"))),
            "decor_51" to GuiItemConfig(slot = 51, material = "BLACK_STAINED_GLASS_PANE", name = " "),
            "decor_52" to GuiItemConfig(slot = 52, material = "BLACK_STAINED_GLASS_PANE", name = " "),
            "close" to GuiItemConfig(slot = 53, material = "RED_DYE", name = "&#FC3737✖ Закрыть склад", lore = listOf("", "&#FC3737 «Выход»", " &7- &fСодержимое будет сохранено.", " &7- &fМеню просто закроется.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы закрыть"))
        )
    )

    @YamlComment("Меню клановых домов — точки телепортации")
    val homesMenu: HomesMenuConfig = HomesMenuConfig()
}

@Serializable
data class HomesMenuConfig(
    @YamlComment("Название инвентаря клановых домов")
    val title: String = "&#FC7D37« Клановые Дома » &7({page}/{pages})",
    @YamlComment("Количество рядов (1-6). По умолчанию 6 (54 слота)")
    val rows: Int = 6,
    @YamlComment("Список точек домов")
    val homes: List<HomesEntryConfig> = defaultHomesEntries(),
    @YamlComment("Лор предмета для установленного дома")
    val setLore: List<String> = listOf("", "&#9EFC65 «Статус»", " &7- &fСостояние: &aУстановлена ✔", " &7- &fМир: &b{world}", " &7- &fX: &e{x}  &fY: &e{y}  &fZ: &e{z}", "", "&#FC65DF «Управление»", " &7- &fЛКМ: &aТелепортироваться", " &7- &fПКМ: &eПереставить сюда", " &7- &fShift+ПКМ: &cУдалить", "", "&#FF8702➥ &fНажмите &eЛКМ &fдля телепорта"),
    @YamlComment("Лор предмета для не установленного дома")
    val unsetLore: List<String> = listOf("", "&#FC3737 «Не настроена»", " &7- &fТочка ещё не установлена.", " &7- &fВстаньте в нужном месте", " &7- &fи нажмите &eПКМ &fдля установки.", "", "&#FF8702➥ &fНажмите &eПКМ &fчтобы установить"),
    @YamlComment("Слот информационного предмета")
    val infoSlot: Int = 4,
    @YamlComment("Материал информационного предмета")
    val infoMaterial: String = "COMPASS",
    @YamlComment("Название информационного предмета")
    val infoName: String = "&#FC7D37Клановые Дома",
    @YamlComment("Лор информационного предмета")
    val infoLore: List<String> = listOf("", "&#9EFC65 «Информация»", " &7- &fУстановлено: &e{set} &7/ &f{max} точек", " &7- &fВсего слотов: &b{total}", " &7- &fУровень клана: &e{current_level}", " &7- &fСтраница: &e{page} &7/ &f{pages}", "", "&#FC65DF «Управление»", " &7- &fЛКМ: &aТелепортироваться", " &7- &fПКМ: &eУстановить / Переставить", " &7- &fShift+ПКМ: &cУдалить точку"),
    val previousPageButton: GuiItemConfig = GuiItemConfig(slot = 47, material = "ARROW", name = "&#5EA9FD◀ Предыдущая страница", lore = listOf("", "&#9EFC65 «Навигация»", " &7- &fТекущая страница: &e{page} &7/ &f{pages}", " &7- &fПереход: &#5EA9FDназад", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы открыть")),
    val previousPageLockedButton: GuiItemConfig = GuiItemConfig(slot = 47, material = "GRAY_DYE", name = "&#FC3737◀ Предыдущая страница", lore = listOf("", "&#FC3737 «Недоступно»", " &7- &fВы уже на первой странице.", "", "&c➥ Листать назад нельзя")),
    val nextPageButton: GuiItemConfig = GuiItemConfig(slot = 51, material = "SPECTRAL_ARROW", name = "&#5EA9FDСледующая страница ▶", lore = listOf("", "&#9EFC65 «Навигация»", " &7- &fТекущая страница: &e{page} &7/ &f{pages}", " &7- &fПереход: &#5EA9FDвперёд", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы открыть"), glow = true),
    val nextPageLockedButton: GuiItemConfig = GuiItemConfig(slot = 51, material = "GRAY_DYE", name = "&#FC3737Следующая страница ▶", lore = listOf("", "&#FC3737 «Недоступно»", " &7- &fВы уже на последней странице.", "", "&c➥ Листать вперёд нельзя")),
    val backButton: GuiItemConfig = GuiItemConfig(slot = 49, material = "RED_CANDLE", name = "&#FC3737⏎ Вернуться в меню", lore = listOf("", "&#FC65DF «Переход»", " &7- &fОткрывает главное меню клана.", " &7- &fВсе изменения домов уже сохранены.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы вернуться")),
    val backSlot: Int = 49,
    val backName: String = "&cВернуться в меню",
    val backLore: List<String> = listOf("&7Нажмите, чтобы открыть главное меню.")
)

@Serializable
data class HomesEntryConfig(
    val key: String = "main",
    val label: String = "Главная",
    val colorCode: String = "&#5EFD7D",
    val emoji: String = "🏠",
    val page: Int = 1,
    val slot: Int = 20,
    val requiredLevel: Int = 1,
    val lockedMaterial: String = "RED_STAINED_GLASS_PANE",
    val unsetMaterial: String = "LIGHT_GRAY_STAINED_GLASS_PANE",
    val setMaterial: String = "RED_BED",
    val unlockedMaterial: String = "",
    val lockedName: String = "&#FC3737🔒 {label} &7(ур. {required_level})",
    val lockedLore: List<String> = listOf("", "&#FC3737 «Слот заблокирован»", " &7- &fТребуемый уровень: &#FFD700{required_level}", " &7- &fТекущий уровень: &c{current_level}", " &7- &fСтраница: &e{page} &7/ &f{pages}", "", "&#FC65DF «Как открыть»", " &7- &fПрокачайте клан в меню улучшений.", " &7- &fПосле апгрейда слот станет активным.", "", "&c➥ Этот слот пока нельзя использовать"),
    val unsetName: String = "&#FC7D37{emoji} {label} &7(свободно)",
    val unsetLore: List<String> = listOf("", "&#FC7D37 «Свободный слот»", " &7- &fТочка ещё не установлена.", " &7- &fУровень открытия: &#5EFD7D{required_level}", " &7- &fСтраница: &e{page} &7/ &f{pages}", "", "&#FC65DF «Установка»", " &7- &fВстаньте в нужном месте.", " &7- &fНажмите &eПКМ&f, чтобы сохранить точку.", "", "&#FF8702➥ &fНажмите &eПКМ &fчтобы установить"),
    val setName: String = "{color}{emoji} {label} &7(активна)",
    val setLore: List<String> = listOf("", "&#9EFC65 «Статус»", " &7- &fСостояние: &aУстановлена ✔", " &7- &fМир: &#5EA9FD{world}", " &7- &fX: &e{x}  &fY: &e{y}  &fZ: &e{z}", " &7- &fСтраница: &e{page} &7/ &f{pages}", "", "&#FC65DF «Управление»", " &7- &fЛКМ: &aТелепортироваться", " &7- &fПКМ: &eПереставить сюда", " &7- &fShift+ПКМ: &cУдалить", "", "&#FF8702➥ &fНажмите &eЛКМ &fдля телепорта")
)

private fun defaultHomesEntries(): List<HomesEntryConfig> {
    val slots = listOf(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34)
    val setMaterials = listOf("RED_BED", "BLUE_BED", "PURPLE_BED", "GREEN_BED", "YELLOW_BED", "CYAN_BED", "MAGENTA_BED")
    val colors = listOf("&#FC7D37", "&#5EA9FD", "&#FC65DF", "&#5EFD7D", "&#FFD700")
    return (1..42).map { index ->
        val level = when {
            index <= 4 -> 1
            index <= 8 -> 2
            index <= 14 -> 3
            index <= 24 -> 4
            else -> 5
        }
        val number = index.toString().padStart(2, '0')
        HomesEntryConfig(
            key = "home_$number",
            label = "Точка #$number",
            colorCode = colors[(level - 1).coerceIn(colors.indices)],
            emoji = "⌂",
            page = ((index - 1) / slots.size) + 1,
            slot = slots[(index - 1) % slots.size],
            requiredLevel = level,
            lockedMaterial = "BARRIER",
            unsetMaterial = "ENDER_EYE",
            setMaterial = setMaterials[(index - 1) % setMaterials.size]
        )
    }
}
