package ua.inventorytype.pnclans.impl.config

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.KSerializer
import ru.privatenull.pnlibrary.bukkit.config.CodeFirstYaml
import ru.privatenull.pnlibrary.bukkit.config.ConfigCodec
import ru.privatenull.pnlibrary.bukkit.config.ConfigGroup
import ru.privatenull.pnlibrary.bukkit.config.ConfigValueValidator
import ru.privatenull.pnlibrary.bukkit.config.ManagedConfig
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import ua.inventorytype.pnclans.BukkitPlugin
import ua.inventorytype.pnclans.api.Action
import ua.inventorytype.pnclans.api.ActionContext
import ua.inventorytype.pnclans.api.clan.ClanRole
import java.io.File

/**
 * Service responsible for loading, saving, and managing all plugin configuration files.
 *
 * Managed YAML files:
 * - `config.yml`   → [Settings] — general plugin settings, storage type, economy options
 * - `menus.yml`    → [MenusConfig] — config-driven GUI layout, item slots, actions
 * - `messages.yml` → [MessagesConfig] — player-facing event responses as [Action] lists
 * - `points.yml`   → [ClanPointsConfig] — anti-farm, personal point and point-history rules
 * - `shop.yml`     → [ClanShopConfig] — clan shop catalogue and display
 * - `quests.yml`   → [ClanQuestsConfig] — shared quest objectives, cycles, prerequisites, and rewards
 * - `battles.yml`  → [ClanBattlesConfig] — arena battles, rules, rewards, and battle GUI
 *
 * Uses [Yaml] with [PolymorphismStyle.Tag] to support polymorphic [ua.inventorytype.pnclans.api.Action]
 * deserialization across both `menus.yml` and `messages.yml`.
 *
 * @param plugin The owning Bukkit plugin instance.
 */
class ConfigService(private val plugin: Plugin) {

    /**
     * Kaml YAML serializer configured with:
     * - `encodeDefaults = true` — always write default values to generated config files.
     * - `strictMode = false`   — silently ignore unknown keys for forward compatibility.
     * - `polymorphismStyle = Tag` — enables `!message`, `!sound`, `!title`, etc. tag syntax.
     */
    val yaml: Yaml = Yaml(
        configuration = YamlConfiguration(
            encodeDefaults = true,
            strictMode = false,
            polymorphismStyle = PolymorphismStyle.Tag
        )
    )

    private val settingsFile = managed("config.yml", Settings.serializer(), Settings())
    private val menusFile = managed("menus.yml", MenusConfig.serializer(), MenusConfig())
    private val messagesFile = managed("messages.yml", MessagesConfig.serializer(), MessagesConfig())
    private val pointsFile = managed("points.yml", ClanPointsConfig.serializer(), ClanPointsConfig())
    private val shopFile = managed("shop.yml", ClanShopConfig.serializer(), ClanShopConfig())
    private val questsFile = managed("quests.yml", ClanQuestsConfig.serializer(), ClanQuestsConfig())
    private val battlesFile = managed("battles.yml", ClanBattlesConfig.serializer(), ClanBattlesConfig())

    private val files = ConfigGroup()
        .add(settingsFile).add(menusFile).add(messagesFile).add(pointsFile)
        .add(shopFile).add(questsFile).add(battlesFile)

    /** Loaded general plugin settings from `config.yml`. */
    lateinit var settings: Settings private set

    /** Loaded GUI menu configuration from `menus.yml`. */
    lateinit var menus: MenusConfig private set

    /**
     * Loaded player-facing event responses from `messages.yml`.
     * Each entry is a [List] of [Action] objects, allowing arbitrary combinations of
     * `!message`, `!sound`, `!title`, `!actionbar`, `!particle`, etc.
     */
    lateinit var messages: MessagesConfig private set

    /** Loaded point, history and anti-farm rules from `points.yml`. */
    lateinit var points: ClanPointsConfig private set

    /** Loaded clan shop definition from `shop.yml`. */
    lateinit var shop: ClanShopConfig private set

    /** Loaded clan quest definitions from `quests.yml`. */
    lateinit var quests: ClanQuestsConfig private set

    /** Loaded clan battle rules, arenas, and GUI display definitions. */
    lateinit var battles: ClanBattlesConfig private set

    /**
     * Loads or generates all plugin configuration files on startup.
     *
     * If a file does not yet exist, its default values are serialized and written to disk.
     * Called once during [ua.inventorytype.pnclans.BukkitPlugin.onEnable].
     */
    fun loadAll() {
        val previousSettings = if (::settings.isInitialized) settings else null
        val previousMenus = if (::menus.isInitialized) menus else null
        val previousMessages = if (::messages.isInitialized) messages else null
        val previousPoints = if (::points.isInitialized) points else null
        val previousShop = if (::shop.isInitialized) shop else null
        val previousQuests = if (::quests.isInitialized) quests else null
        val previousBattles = if (::battles.isInitialized) battles else null
        try {
            if (!plugin.dataFolder.exists()) {
                plugin.dataFolder.mkdirs()
            }

            settings = settingsFile.reloadValue()
            menus = menusFile.reloadValue()
            messages = messagesFile.reloadValue()
            points = pointsFile.reloadValue()
            val defaultShop = ClanShopConfig()
            val shopDiskFile = File(plugin.dataFolder, "shop.yml")
            val existingShopContent = shopDiskFile.takeIf(File::exists)?.readText()
            val existingShopVersion = LegacyShopMigrator.schemaVersion(existingShopContent, defaultShop.schemaVersion)
            val explicitProductRarities = LegacyShopMigrator.explicitProductRarityIds(existingShopContent)
            shop = shopFile.reloadValue()
            if (existingShopVersion < defaultShop.schemaVersion) {
                shop = LegacyShopMigrator.migrate(shop, existingShopVersion, explicitProductRarities)
                saveShop()
            }
            val defaultQuests = ClanQuestsConfig()
            val questFile = File(plugin.dataFolder, "quests.yml")
            val existingQuestVersion = LegacyShopMigrator.schemaVersion(questFile.takeIf(File::exists)?.readText(), defaultQuests.schemaVersion)
            val loadedQuests = questsFile.reloadValue()
            quests = if (existingQuestVersion < defaultQuests.schemaVersion) {
                loadedQuests.copy(
                    schemaVersion = defaultQuests.schemaVersion,
                    display = defaultQuests.display,
                    quests = loadedQuests.quests.filterKeys { it !in defaultQuests.quests } + defaultQuests.quests
                ).also { saveQuests(it) }
            } else {
                loadedQuests
            }
            val defaultBattles = ClanBattlesConfig()
            val battleFile = File(plugin.dataFolder, "battles.yml")
            val existingBattleVersion = LegacyShopMigrator.schemaVersion(battleFile.takeIf(File::exists)?.readText(), defaultBattles.schemaVersion)
            val loadedBattles = battlesFile.reloadValue()
            battles = if (existingBattleVersion < defaultBattles.schemaVersion) {
                loadedBattles.copy(
                    schemaVersion = defaultBattles.schemaVersion,
                    display = defaultBattles.display
                ).also { saveBattles(it) }
            } else {
                loadedBattles
            }
        } catch (error: Throwable) {
            previousSettings?.let { settings = it }
            previousMenus?.let { menus = it }
            previousMessages?.let { messages = it }
            previousPoints?.let { points = it }
            previousShop?.let { shop = it }
            previousQuests?.let { quests = it }
            previousBattles?.let { battles = it }
            throw error
        }
    }

    /** Persists shop changes made by the in-game administrator editor. */
    fun saveShop() {
        shopFile.save(shop)
    }

    private fun saveQuests(value: ClanQuestsConfig) {
        questsFile.save(value)
    }

    private fun saveBattles(value: ClanBattlesConfig) {
        battlesFile.save(value)
    }

    /** Перечитывает и проверяет все семь конфигураций pnClans. */
    fun reloadAll() = loadAll()

    /** Сохраняет текущие типизированные значения всех конфигураций. */
    fun saveAll() {
        settingsFile.save(settings)
        menusFile.save(menus)
        messagesFile.save(messages)
        pointsFile.save(points)
        shopFile.save(shop)
        questsFile.save(quests)
        battlesFile.save(battles)
    }

    /** Выгружает все конфигурации из памяти, не удаляя файлы. */
    fun unloadAll() = files.unloadAll()

    /**
     * Retrieves the configurable display name for a [ClanRole] from `config.yml`.
     *
     * @param role The clan role to look up.
     * @return The localized display name string defined in [Settings].
     */
    fun getRoleDisplayName(role: ClanRole): String {
        return when (role) {
            ClanRole.LEADER -> settings.roleLeader
            ClanRole.DEPUTY -> settings.roleDeputy
            ClanRole.ELDER -> settings.roleElder
            ClanRole.MEMBER -> settings.roleMember
        }
    }

    /**
     * Formats a message template by processing PlaceholderAPI placeholders, internal `{key}` tokens,
     * hex color codes (`&#RRGGBB`), and legacy `&` color codes.
     *
     * @param player The player context used for PlaceholderAPI resolution.
     * @param template The raw template string (from any config file).
     * @param customPlaceholders Additional `{key}` → `value` pairs to replace in the template.
     * @return The fully formatted and colorized message string.
     */
    fun formatMessage(player: Player, template: String, customPlaceholders: Map<String, String> = emptyMap()): String {
        val bukkitPlugin = plugin as? BukkitPlugin ?: return template
        return bukkitPlugin.placeholderRegistry.process(player, template, customPlaceholders)
    }

    /**
     * Returns the active animation frame for the given frame list.
     *
     * The frame index is computed from the current time using [AnimationConfig.frameIntervalMs]
     * so multiple players see a synchronised animation without needing a scheduler.
     *
     * @param frames Frame list from [AnimationConfig]. Empty list returns the fallback text.
     * @param fallback Text returned when [frames] is empty.
     */
    fun animatedFrame(frames: List<String>, fallback: String = ""): String {
        if (frames.isEmpty()) return fallback
        val interval = settings.animations.frameIntervalMs.toLong().coerceAtLeast(MIN_FRAME_INTERVAL_MS)
        val frame = ((System.currentTimeMillis() / interval) % frames.size).toInt()
        return frames[frame]
    }

    /**
     * Convenience helper that resolves a named animation collection from [AnimationConfig].
     *
     * @param key One of "hiddenBalance", "upgradeIdle", "upgradeReady", "upgradeBusy".
     * @return The matching frame list, or an empty list if the key is unknown.
     */
    fun animationFrames(key: String): List<String> = when (key) {
        AnimationKey.HIDDEN_BALANCE -> settings.animations.hiddenBalance
        AnimationKey.UPGRADE_IDLE -> settings.animations.upgradeIdle
        AnimationKey.UPGRADE_READY -> settings.animations.upgradeReady
        AnimationKey.UPGRADE_BUSY -> settings.animations.upgradeBusy
        else -> emptyList()
    }

    private companion object {
        const val MIN_FRAME_INTERVAL_MS = 100L
    }

    /**
     * Executes a list of [Action] objects for the given player, applying optional placeholder tokens.
     *
     * This is the central dispatch method for all config-driven event responses.
     * Each action in the list runs sequentially in declaration order.
     *
     * Example usage:
     * ```kotlin
     * val cfg = clanService.plugin.configService
     * cfg.send(player, cfg.messages.homes.teleported, mapOf("home" to homeName))
     * ```
     *
     * @param player The recipient player.
     * @param actions The list of [Action] objects to execute (from [MessagesConfig]).
     * @param placeholders Optional map of `{key}` → `value` replacements applied to every action.
     */
    fun send(
        player: Player,
        actions: List<Action>,
        placeholders: Map<String, String> = emptyMap(),
        durationSeconds: Int? = null
    ) {
        val bukkitPlugin = plugin as? BukkitPlugin ?: return
        val context = ActionContext(
            player = player,
            placeholderRegistry = bukkitPlugin.placeholderRegistry,
            placeholders = placeholders,
            plugin = bukkitPlugin,
            durationSeconds = durationSeconds
        )
        actions.forEach { it.execute(context) }
    }

    /**
     * Loads a YAML configuration file from the plugin data folder.
     * If the file does not exist, it is created with the provided default instance.
     *
     * @param T The serializable configuration type.
     * @param fileName The name of the YAML file relative to the plugin data folder.
     * @param serializer The Kotlinx [KSerializer] for type [T].
     * @param default The default instance to serialize and write if the file is missing.
     * @return The deserialized configuration instance.
     */
    private fun <T> managed(fileName: String, serializer: KSerializer<T>, default: T): ManagedConfig<T> =
        CodeFirstYaml(
            file = File(plugin.dataFolder, fileName),
            defaults = default,
            codec = object : ConfigCodec<T> {
                override fun encode(value: T): String = yaml.encodeToString(serializer, value)
                override fun decode(yaml: String): T = this@ConfigService.yaml.decodeFromString(serializer, yaml)
            },
            logger = plugin.logger,
            validator = ConfigValueValidator { emptyList() },
        )
}

/** Named animation slots exposed through [ConfigService.animationFrames]. */
object AnimationKey {
    const val HIDDEN_BALANCE = "hiddenBalance"
    const val UPGRADE_IDLE = "upgradeIdle"
    const val UPGRADE_READY = "upgradeReady"
    const val UPGRADE_BUSY = "upgradeBusy"
}
