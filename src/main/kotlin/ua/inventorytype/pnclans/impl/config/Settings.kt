package ua.inventorytype.pnclans.impl.config

import com.charleskorn.kaml.YamlComment
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Available activation methods for the private clan chat channel. */
@Serializable
enum class ClanChatMode {
    COMMAND,
    PREFIX
}

/** GitHub Release channel accepted by the automatic updater. */
@Serializable
enum class UpdateChannel {
    STABLE,
    BETA,
    ALPHA
}

@Serializable
data class ClanActivityPointsConfig(
    @YamlComment("Включить начисление клановых очков за активность игроков.")
    val enabled: Boolean = true,
    @YamlComment("Количество очков за один активный интервал.")
    val pointsPerInterval: Long = 1L,
    @YamlComment("Интервал начисления в секундах.")
    val intervalSeconds: Long = 600L,
    @YamlComment("Сколько секунд без действий считать игрока AFK.")
    val afkTimeoutSeconds: Long = 300L,
    @YamlComment("Максимум очков за активность одному клану за календарный день.")
    val dailyClanLimit: Long = 100L
)

@Serializable
data class ClanCreationConfig(
    @YamlComment("Сколько секунд игрок может вводить название нового клана в чат.")
    val promptTimeoutSeconds: Int = 30,
    @YamlComment("Слова, которыми игрок может отменить ввод названия. Сравнение без учёта регистра.")
    val cancelInputs: List<String> = listOf("cancel", "отмена")
)

@Serializable
data class ClanHighlightDisplayConfig(
    @YamlComment("Текст состояния выбранного варианта в меню метки.")
    val selectedYes: String = "&#5EFD7DДа",
    @YamlComment("Текст состояния невыбранного варианта в меню метки.")
    val selectedNo: String = "&#FC3737Нет",
    @YamlComment("Текст включённого состояния метки.")
    val enabledText: String = "&#5EFD7DВключена",
    @YamlComment("Текст выключенного состояния метки.")
    val disabledText: String = "&#FC3737Выключена",
    @YamlComment("Состояние метки после кнопки сброса.")
    val resetEnabled: Boolean = true,
    @YamlComment("Тип метки после сброса: ARMOR или GLOW.")
    val resetType: String = "ARMOR",
    @YamlComment("Цвет метки после сброса. Используйте один из доступных ClanHighlightColor.")
    val resetColor: String = "AQUA"
)

@Serializable
data class ClanChatMenuItemConfig(
    @YamlComment("Название предмета в меню настроек клана для этого режима. Поддерживаются HEX- и &-цвета.")
    val name: String,
    @YamlComment("Описание (lore) предмета в меню для этого режима. Можно использовать {state}, {action}, {command} и {prefix}.")
    val lore: List<String>
)

@Serializable
data class ClanChatConfig(
    @YamlComment("Способ отправки сообщений в клановый чат. COMMAND: /<command> <сообщение>. PREFIX: обычное сообщение начинается с prefix.")
    val mode: ClanChatMode = ClanChatMode.PREFIX,
    @YamlComment("Название команды для режима COMMAND без символа '/'.")
    val command: String = "cc",
    @YamlComment("Префикс сообщения для режима PREFIX.")
    val prefix: String = "!",
    @YamlComment("Тексты предмета кланового чата в меню настроек для режима COMMAND.")
    val commandMenuItem: ClanChatMenuItemConfig = ClanChatMenuItemConfig(
        name = "&#5EA9FD✎ Клановый чат",
        lore = listOf(
            "", "&#9EFC65 «Состояние»", " &7- &fКанал общения: {state}", " &7- &fРежим: &eКоманда /{command}",
            "", "&#FC65DF «Использование»", " &7- &fНапишите &e/{command} <сообщение>", " &7- &fчтобы отправить сообщение соклановцам.",
            "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы {action}"
        )
    ),
    @YamlComment("Тексты предмета кланового чата в меню настроек для режима PREFIX.")
    val prefixMenuItem: ClanChatMenuItemConfig = ClanChatMenuItemConfig(
        name = "&#5EA9FD✎ Клановый чат",
        lore = listOf(
            "", "&#9EFC65 «Состояние»", " &7- &fКанал общения: {state}", " &7- &fРежим: &eПрефикс {prefix}",
            "", "&#FC65DF «Использование»", " &7- &fНачните сообщение с &e{prefix}", " &7- &fчтобы отправить его соклановцам.",
            "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы {action}"
        )
    ),
    @YamlComment("Текст статуса, если клановый чат доступен.")
    val enabledState: String = "&#5EFD7DДоступен",
    @YamlComment("Текст статуса, если клановый чат закрыт лидером.")
    val disabledState: String = "&#FC3737Закрыт",
    @YamlComment("Текст действия, если чат сейчас доступен.")
    val disableAction: String = "&#FC3737выключить",
    @YamlComment("Текст действия, если чат сейчас закрыт.")
    val enableAction: String = "&#5EFD7Dвключить"
)

@Serializable
data class ModuleConfig(
    @YamlComment("Включить модуль клановых точек дома (/clan home, меню точек в GUI)")
    val homes: Boolean = true,
    @YamlComment("Включить модуль казны и банка клана (пополнение, снятие, история операций)")
    val treasury: Boolean = true,
    @YamlComment("Включить модуль общего кланового склада (/clan chest, виртуальный сундук)")
    val chest: Boolean = true,
    @YamlComment("Включить модуль эволюции и прокачки уровней клана")
    val upgrades: Boolean = true,
    @YamlComment("Включить модуль управления PvP режимом соклановцев")
    val pvp: Boolean = true
)

/** Global configuration settings for pnClans. */
@Serializable
class Settings {
    @YamlComment("Настройки кланового чата.")
    val clanChat: ClanChatConfig = ClanChatConfig()

    @YamlComment("Настройки ввода названия при создании нового клана.")
    val clanCreation: ClanCreationConfig = ClanCreationConfig()

    @YamlComment("Ограниченное начисление клановых очков за активность.")
    val clanActivityPoints: ClanActivityPointsConfig = ClanActivityPointsConfig()

    @YamlComment("Модули фреймворка. Отключите ненужный функционал, чтобы скрыть кнопки в GUI и отключить подкоманды.")
    val modules: ModuleConfig = ModuleConfig()

    @YamlComment("Тип хранилища данных кланов и сундуков: SQLITE (рекомендуется) или JSON")
    var storageType: String = "SQLITE"

    @YamlComment("Канал автоматических обновлений GitHub. STABLE — только полностью выпущенные и проверенные версии. BETA — stable + тестовые beta-сборки; рекомендуется по умолчанию. ALPHA — все alpha, beta и stable-сборки, включая самые ранние версии в разработке.")
    val updateChannel: UpdateChannel = UpdateChannel.BETA

    @Transient
    val checkUpdates: Boolean = true

    @YamlComment("Автоматически скачивать найденные обновления. Если выключено, проверка, уведомления и ручные действия обновления продолжат работать.")
    val autoUpdate: Boolean = true

    @YamlComment("Стоимость создания клана в монетах экономики Vault (0 — бесплатно)")
    val createClanCost: Double = 1000.0

    @YamlComment("Количество клановых очков за убийство другого игрока.")
    val clanPointsPerPlayerKill: Long = 10L

    @YamlComment("Награды клановыми очками за убийство мобов. Ключ — EntityType.")
    val clanPointsPerMobKill: Map<String, Long> = mapOf(
        "ZOMBIE" to 1L,
        "SKELETON" to 1L,
        "CREEPER" to 2L,
        "SPIDER" to 1L,
        "WITHER" to 50L,
        "ENDER_DRAGON" to 100L
    )

    @YamlComment("Время ввода ника при приглашении через меню в секундах")
    val invitePromptTimeoutSeconds: Int = 15

    @YamlComment("Время жизни отправленного приглашения в секундах")
    val inviteLifetimeSeconds: Int = 60

    @YamlComment("Быстрые суммы пополнения казны, выводятся в виде отдельных кнопок.")
    val treasuryDepositPresets: List<Int> = listOf(500, 1000)

    @YamlComment("Слоты быстрых кнопок пополнения. Сопоставляются с treasuryDepositPresets по индексу.")
    val treasuryDepositPresetSlots: List<Int> = listOf(28, 29)

    @YamlComment("Быстрые суммы снятия казны, выводятся в виде отдельных кнопок.")
    val treasuryWithdrawPresets: List<Int> = listOf(500, 1000)

    @YamlComment("Слоты быстрых кнопок снятия. Сопоставляются с treasuryWithdrawPresets по индексу.")
    val treasuryWithdrawPresetSlots: List<Int> = listOf(33, 34)

    @YamlComment("Время ожидания произвольной суммы казны в чате, в секундах.")
    val treasuryPromptTimeoutSeconds: Int = 30

    @YamlComment("Слова для отмены ввода суммы казны. Сравнение выполняется без учёта регистра.")
    val treasuryPromptCancelInputs: List<String> = listOf("cancel", "отмена")

    @YamlComment("Требовать ли пустое хранилище предметов перед распуском клана")
    val disbandRequireEmptyChest: Boolean = true

    @YamlComment("Требовать ли вручную вывести все деньги из казны перед распуском")
    val disbandRequireEmptyBank: Boolean = true

    @YamlComment("Автоматически возвращать остаток казны лидеру при распуске")
    val disbandAutoRefundBank: Boolean = false

    @YamlComment("Доступные цвета подсветки соклановцев в меню настройки цвета")
    val clanHighlightColors: List<String> = listOf("AQUA", "BLUE", "DARK_AQUA", "GREEN", "RED", "GOLD", "YELLOW", "LIGHT_PURPLE", "WHITE")

    @YamlComment("Отображаемые состояния и значения сброса метки соклановцев.")
    val clanHighlightDisplay: ClanHighlightDisplayConfig = ClanHighlightDisplayConfig()

    @YamlComment("Названия ролей клана для отображения в чате, меню и плейсхолдерах {clan_role}")
    val roleLeader: String = "Лидер"
    val roleDeputy: String = "Заместитель"
    val roleElder: String = "Старейшина"
    val roleMember: String = "Участник"

    @YamlComment("Устаревшие строковые сообщения для обратной совместимости. Новые игровые ответы находятся в messages.yml.")
    val msgClanCreated: String = "&aВы успешно создали клан &e{clan}&a!"
    val msgClanDisbanded: String = "&cКлан {clan} был распущен лидером."
    val msgAlreadyInClan: String = "&cВы уже состоите в клане."
    val msgNoClan: String = "&cВы не состоите в клане."
    val msgInviteSent: String = "&aВы отправили приглашение игроку &e{target}&a."
    val msgInviteReceived: String = "&aИгрок &e{sender} &aприглашает вас в клан &e{clan}&a! Используйте &e/clan accept &aили &c/clan deny&a."
    val msgInviteAccepted: String = "&aВы успешно вступили в клан &e{clan}&a!"
    val msgInviteDenied: String = "&cВы отклонили приглашение в клан {clan}."
    val msgNoMoney: String = "&cУ вас недостаточно денег (требуется {cost}$)."
    val msgDepositSuccess: String = "&aВы внесли &e{amount} ⛁ &aв казну клана {clan}."
    val msgWithdrawSuccess: String = "&aВы сняли &e{amount} ⛁ &aиз казны клана {clan}."
    val msgNoPermission: String = "&cУ вас недостаточно прав для выполнения этого действия."
    val msgPvpDisabled: String = "&c[pnClans] Урон по соклановцам отключён!"
    val msgChatFormat: String = "&8[&6Клан &e{clan}&8] &7[{role}] &7{player}&8: &f{message}"
    @YamlComment("Сообщение игроку, когда клановый чат закрыт лидером.")
    val msgClanChatDisabled: String = "&cЧат вашего клана закрыт лидером."
    @YamlComment("Подсказка для пустой команды кланового чата в режиме COMMAND.")
    val msgClanChatCommandUsage: String = "&eИспользование: /{command} <сообщение>"
    val msgJoinNotice: String = "&8[&6Клан&8] &aУчастник &e{player} &aвошёл на сервер!"
    val msgQuitNotice: String = "&8[&6Клан&8] &cУчастник &e{player} &cвышел с сервера!"

    @YamlComment("Анимационные кадры, используемые в анимированных плейсхолдерах.")
    val animations: AnimationConfig = AnimationConfig()
}

@Serializable
data class AnimationConfig(
    @YamlComment("Интервал между кадрами анимации в миллисекундах")
    val frameIntervalMs: Int = 600,
    @YamlComment("Кадры для скрытого баланса казны.")
    val hiddenBalance: List<String> = listOf(
        "&#FC3737∗ &8∗ &5∗ &8∗ &6∗",
        "&#FC3737∗ &8∗ &5∗ &6∗ &8∗",
        "&#FC3737∗ &8∗ &6∗ &5∗ &8∗",
        "&#FC3737∗ &6∗ &5∗ &8∗ &8∗"
    ),
    @YamlComment("Кадры маяка эволюции в состоянии ожидания.")
    val upgradeIdle: List<String> = listOf(
        "&#FC7D37✦ Подготовка", "&#FC7D37✦ Подготовка.", "&#FC7D37✦ Подготовка..", "&#FC7D37✦ Подготовка..."
    ),
    @YamlComment("Кадры маяка, когда требования эволюции выполнены.")
    val upgradeReady: List<String> = listOf(
        "&#FFD700✦ Ритуал готов", "&#FFD700✦ Ритуал готов.", "&#FFD700✦ Ритуал готов..", "&#FFD700✦ Ритуал готов..."
    ),
    @YamlComment("Кадры маяка, когда требований эволюции не хватает.")
    val upgradeBusy: List<String> = listOf(
        "&#FC3737✦ Нужны ресурсы", "&#FC3737✦ Нужны ресурсы.", "&#FC3737✦ Нужны ресурсы..", "&#FC3737✦ Нужны ресурсы..."
    )
)
