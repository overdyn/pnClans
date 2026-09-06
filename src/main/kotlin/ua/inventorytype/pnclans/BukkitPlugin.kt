package ua.inventorytype.pnclans

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.ServicePriority
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.util.TimeStampMode
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import ua.inventorytype.pnclans.api.PnClansApi
import ua.inventorytype.pnclans.api.clan.ClanPoints
import ua.inventorytype.pnclans.api.command.ClanSubcommand
import ua.inventorytype.pnclans.api.menu.ClanMainMenuButton
import ua.inventorytype.pnclans.api.gui.ClanAddonGuiRegistry
import ua.inventorytype.pnclans.api.placeholder.PlaceholderRegistry
import ua.inventorytype.pnclans.impl.clan.ClanInviteService
import ua.inventorytype.pnclans.impl.clan.ClanHighlightService
import ua.inventorytype.pnclans.impl.clan.ClanService
import ua.inventorytype.pnclans.impl.clan.ClanPointsService
import ua.inventorytype.pnclans.impl.clan.ClanPointsAntiFarmService
import ua.inventorytype.pnclans.impl.clan.ClanActivityPointsService
import ua.inventorytype.pnclans.impl.clan.ClanQuestService
import ua.inventorytype.pnclans.impl.clan.ClanBattleService
import ua.inventorytype.pnclans.impl.shop.ClanShopService
import ua.inventorytype.pnclans.impl.api.PnClansApiImpl
import ua.inventorytype.pnclans.impl.command.ClanCommand
import ua.inventorytype.pnclans.impl.config.ConfigMigrationSafety
import ua.inventorytype.pnclans.impl.config.ConfigService
import ua.inventorytype.pnclans.impl.config.ConfigValidator
import ua.inventorytype.pnclans.impl.config.MenuConfigValidator
import ua.inventorytype.pnclans.impl.config.PointsConfigValidator
import ua.inventorytype.pnclans.impl.economy.EconomyService
import ua.inventorytype.pnclans.impl.inventory.listener.GuiListener
import ua.inventorytype.pnclans.impl.listener.ClanListener
import ua.inventorytype.pnclans.impl.placeholder.PnClansExpansion
import ua.inventorytype.pnclans.impl.teleport.TeleportService
import ua.inventorytype.pnclans.impl.ux.TimedBossBarService
import ua.inventorytype.pnclans.impl.util.ChatInputPrompt
import ua.inventorytype.pnclans.impl.integration.PnLibraryIntegration
import ua.inventorytype.pnclans.impl.integration.PnLibraryBootstrapInstaller

class BukkitPlugin : JavaPlugin() {
    private var startupBlocked = false

    override fun onLoad() {
        if (!PnLibraryBootstrapInstaller.ensureInstalled(this)) {
            startupBlocked = true
            return
        }
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().load()
        logger.info("[pnClans] PacketEvents loaded from ${PacketEvents::class.java.protectionDomain.codeSource?.location}")
    }

    internal lateinit var pnLibraryIntegration: PnLibraryIntegration
        private set

    internal fun hasPnLibraryIntegration(): Boolean = ::pnLibraryIntegration.isInitialized

    lateinit var economyService: EconomyService
        private set

    lateinit var configService: ConfigService
        private set

    lateinit var placeholderRegistry: PlaceholderRegistry
        private set

    lateinit var clanService: ClanService
        private set

    lateinit var clanPointsService: ClanPoints
        private set

    internal lateinit var clanPointsAntiFarmService: ClanPointsAntiFarmService
        private set

    lateinit var clanActivityPointsService: ClanActivityPointsService
        private set

    internal lateinit var clanQuestService: ClanQuestService
        private set

    internal lateinit var clanBattleService: ClanBattleService
        private set

    internal lateinit var clanShopService: ClanShopService
        private set

    lateinit var inviteService: ClanInviteService
        private set

    lateinit var teleportService: TeleportService
        private set

    lateinit var timedBossBarService: TimedBossBarService
        private set

    lateinit var guiListener: GuiListener
        private set

    lateinit var clanHighlightService: ClanHighlightService
        private set

    private lateinit var publicApi: PnClansApi

    fun registerClanSubcommand(owner: org.bukkit.plugin.Plugin, command: ClanSubcommand): Boolean =
        publicApi.subcommands.register(owner, command)

    fun publicSubcommand(name: String): ClanSubcommand? =
        publicApi.subcommands.all().firstOrNull { it.name.equals(name, true) || it.aliases.any { alias -> alias.equals(name, true) } }

    fun publicSubcommandNames(): List<String> = publicApi.subcommands.all()
        .flatMap { command -> listOf(command.name) + command.aliases }

    fun publicMainMenuButtons(): Collection<ClanMainMenuButton> = publicApi.menus.mainButtons()

    fun publicAddonGui(): ClanAddonGuiRegistry = publicApi.gui

    /** Loads configs through compatibility protection, exposes new keys and reports invalid references. */
    internal fun reloadConfigurations() {
        val migrationSafety = ConfigMigrationSafety.capture(this)
        configService.loadAll()
        if (migrationSafety.reconcile(this, configService)) {
            configService.loadAll()
        }
        ConfigValidator.validate(this, configService)
        PointsConfigValidator.validate(this, configService)
        MenuConfigValidator.validate(this, configService)
    }

    override fun onEnable() {
        if (startupBlocked) {
            server.pluginManager.disablePlugin(this)
            return
        }
        if (!PnLibraryBootstrapInstaller.ensureCompatible(this, PnLibraryIntegration.MINIMUM_LIBRARY_VERSION)) {
            server.pluginManager.disablePlugin(this)
            return
        }
        PacketEvents.getAPI().settings.debug(false).checkForUpdates(false).timeStampMode(TimeStampMode.MILLIS).reEncodeByDefault(true)
        PacketEvents.getAPI().init()
        logger.info("[pnClans] Enabled ${description.version} on ${server.version}; PacketEvents initialized=${PacketEvents.getAPI().isInitialized}")

        economyService = EconomyService()
        val economyConnected = economyService.setup()

        configService = ConfigService(this)
        reloadConfigurations()

        pnLibraryIntegration = PnLibraryIntegration(this)

        ua.inventorytype.pnclans.impl.analytics.ErrorReporter.init(this)

        placeholderRegistry = PlaceholderRegistry()
        clanService = ClanService(this)
        clanPointsService = ClanPointsService(clanService)
        clanPointsAntiFarmService = ClanPointsAntiFarmService(this)
        clanActivityPointsService = ClanActivityPointsService(this)
        clanQuestService = ClanQuestService(this)
        clanBattleService = ClanBattleService(this)
        clanShopService = ClanShopService(this)
        placeholderRegistry.registerDefaults(clanService)
        publicApi = PnClansApiImpl(clanService, clanPointsService)
        server.servicesManager.register(PnClansApi::class.java, publicApi, this, ServicePriority.Normal)

        val addonResults = publicApi.addons.loadDirectory()
        clanHighlightService = ClanHighlightService(this)
        inviteService = ClanInviteService(clanService)
        teleportService = TeleportService(this)
        timedBossBarService = TimedBossBarService(this)

        guiListener = GuiListener(this)
        server.pluginManager.registerEvents(guiListener, this)
        server.pluginManager.registerEvents(ClanListener(this), this)
        server.pluginManager.registerEvents(clanHighlightService, this)
        clanHighlightService.syncAll()
        clanQuestService.deliverPendingRewardsForOnlinePlayers()

        val clanCommand = ClanCommand(this, inviteService)
        getCommand("clan")?.let { cmd ->
            cmd.setExecutor(clanCommand)
            cmd.tabCompleter = clanCommand
        }

        var papiConnected = false
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            PnClansExpansion(this).register()
            papiConnected = true
        }

        pnLibraryIntegration.started(
            economyConnected = economyConnected,
            papiConnected = papiConnected,
            loadedClansCount = clanService.getAllClans().size,
            loadedAddonsCount = addonResults.size
        )
    }

    override fun onDisable() {
        val savedClansCount = if (::clanService.isInitialized) clanService.getAllClans().size else 0

        if (::publicApi.isInitialized) {
            server.servicesManager.unregister(PnClansApi::class.java, publicApi)
        }
        ChatInputPrompt.shutdown()
        if (::timedBossBarService.isInitialized) timedBossBarService.clearAll()
        if (::clanActivityPointsService.isInitialized) clanActivityPointsService.shutdown()
        if (::clanBattleService.isInitialized) clanBattleService.shutdown()
        if (::inviteService.isInitialized) inviteService.clear()
        if (::guiListener.isInitialized) guiListener.forceCloseAll()
        if (::clanHighlightService.isInitialized) clanHighlightService.shutdown()
        if (::clanService.isInitialized) {
            clanService.saveAll(finalizeSessions = true)
            clanService.storage.close()
        }
        if (PacketEvents.getAPI() != null && PacketEvents.getAPI().isInitialized) PacketEvents.getAPI().terminate()
        ua.inventorytype.pnclans.impl.analytics.ErrorReporter.shutdown()
        if (::configService.isInitialized) configService.unloadAll()
        if (::pnLibraryIntegration.isInitialized) {
            pnLibraryIntegration.close(savedClansCount)
        }
    }
}
