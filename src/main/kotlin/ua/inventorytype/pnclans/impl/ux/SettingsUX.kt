package ua.inventorytype.pnclans.impl.ux

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import ru.privatenull.pnlibrary.bukkit.inventory.*
import ua.inventorytype.pnclans.api.clan.Clan
import ua.inventorytype.pnclans.api.clan.ClanRole
import ua.inventorytype.pnclans.api.clan.ClanSetting
import ua.inventorytype.pnclans.api.permission.ClanPerms
import ua.inventorytype.pnclans.api.permission.Permission
import ua.inventorytype.pnclans.impl.clan.ClanService
import ua.inventorytype.pnclans.impl.config.ClanChatMode
import ua.inventorytype.pnclans.impl.config.GuiItemConfig
import ua.inventorytype.pnclans.impl.inventory.builder.ItemBuilder
import ua.inventorytype.pnclans.impl.util.ColorUtil

/** Настройки клана, построенные целиком на GUI API pnLibrary. */
class SettingsUX(
    private val clanService: ClanService,
    val editorRolesUX: EditorRolesUX = EditorRolesUX(clanService),
) {
    private val cfg get() = clanService.plugin.configService
    private val menu: Menu = createMenu()

    fun open(player: Player) {
        if (clanService.getClanUser(player) == null) return
        PnMenus.get().open(clanService.plugin, player, menu)
    }

    private fun createMenu(): Menu {
        val menuCfg = cfg.menus.settingsMenu
        val builder = Menus.chest(ColorUtil.color(menuCfg.title)).rows(menuCfg.rows)
        addBackground(builder)

        if (cfg.settings.modules.pvp) toggle(builder, "pvp", ClanSetting.PVP, ClanPerms.Settings.TOGGLE_PVP)
        toggle(builder, "chat", ClanSetting.CHAT, ClanPerms.Settings.TOGGLE_CHAT)
        if (cfg.settings.modules.chest) toggle(builder, "chest", ClanSetting.CHEST, ClanPerms.Action.OPEN_CHEST)
        toggle(builder, "join", ClanSetting.JOIN, ClanPerms.Settings.TOGGLE_JOIN)

        action(builder, "color") { openColor(it.player) }
        action(builder, "roles") { openRoles(it.player) }
        action(builder, "back") { MainUX(clanService).open(it.player) }
        return builder.render(MenuRenderer(::render)).build()
    }

    private fun toggle(builder: MenuBuilder, key: String, setting: ClanSetting, permission: Permission) =
        action(builder, key) { click ->
            val clan = clanService.getClanUser(click.player) ?: return@action
            val user = clan.getMember(click.player.uniqueId) ?: return@action
            if (!clan.hasPermission(user, permission)) {
                cfg.send(click.player, cfg.messages.settings.noPermission)
                return@action
            }
            if (clanService.changeSetting(clan, setting, !clan.isSettingEnabled(setting)).isSuccess) click.refresh()
        }

    private fun action(builder: MenuBuilder, key: String, handler: (MenuClick) -> Unit) {
        val slot = cfg.menus.settingsMenu.items[key]?.slot ?: return
        builder.slot(slot, null, click = MenuClickHandler(handler))
    }

    private fun render(session: MenuSession) {
        val player = session.player
        val clan = clanService.getClanUser(player) ?: run { session.close(); return }
        if (cfg.settings.modules.pvp) renderSetting(session, clan, "pvp", ClanSetting.PVP)
        renderChat(session, clan)
        if (cfg.settings.modules.chest) renderSetting(session, clan, "chest", ClanSetting.CHEST)
        renderSetting(session, clan, "join", ClanSetting.JOIN)
        renderColor(session, clan)
        renderRoles(session, clan)
        renderInfo(session, clan, "overview")
        renderInfo(session, clan, "hint")
        cfg.menus.settingsMenu.items["back"]?.let { session.set(it.slot, item(player, it, emptyMap())) }
    }

    private fun renderSetting(session: MenuSession, clan: Clan, key: String, setting: ClanSetting) {
        val config = cfg.menus.settingsMenu.items[key] ?: return
        val enabled = clan.isSettingEnabled(setting)
        val state = when (key) {
            "chest" -> if (enabled) "&#5EFD7DОткрыто" else "&#FC3737Закрыто"
            "join" -> if (enabled) "&#5EFD7DВключены" else "&#FC3737Выключены"
            else -> if (enabled) "&#5EFD7DВключён" else "&#FC3737Выключен"
        }
        val placeholders = commonPlaceholders(session.player, clan) + mapOf(
            "state" to state,
            "pvp_damage" to if (enabled) "&#5EFD7DРазрешён" else "&#FC3737Заблокирован",
            "action" to if (enabled) "&#FC3737выключить" else "&#5EFD7Dвключить",
        )
        session.set(config.slot, item(session.player, config, placeholders, enabled))
    }

    private fun renderChat(session: MenuSession, clan: Clan) {
        val config = cfg.menus.settingsMenu.items["chat"] ?: return
        val enabled = clan.isSettingEnabled(ClanSetting.CHAT)
        val chat = cfg.settings.clanChat
        val text = when (chat.mode) {
            ClanChatMode.COMMAND -> chat.commandMenuItem
            ClanChatMode.PREFIX -> chat.prefixMenuItem
        }
        val placeholders = commonPlaceholders(session.player, clan) + mapOf(
            "state" to if (enabled) chat.enabledState else chat.disabledState,
            "action" to if (enabled) chat.disableAction else chat.enableAction,
            "command" to chat.command.trim().removePrefix("/"),
            "prefix" to chat.prefix,
        )
        session.set(config.slot, buildItem(config, format(session.player, text.name, placeholders),
            format(session.player, text.lore, placeholders), config.glow || enabled))
    }

    private fun renderColor(session: MenuSession, clan: Clan) {
        val config = cfg.menus.settingsMenu.items["color"] ?: return
        val user = clan.getMember(session.player.uniqueId) ?: return
        val placeholders = commonPlaceholders(session.player, clan) +
            ("role" to cfg.getRoleDisplayName(clan.getUserRole(user)))
        session.set(config.slot, item(session.player, config, placeholders))
    }

    private fun renderRoles(session: MenuSession, clan: Clan) {
        val config = cfg.menus.settingsMenu.items["roles"] ?: return
        val user = clan.getMember(session.player.uniqueId) ?: return
        val placeholders = commonPlaceholders(session.player, clan) + mapOf(
            "roles" to ClanRole.entries.size.toString(),
            "role" to cfg.getRoleDisplayName(clan.getUserRole(user)),
        )
        session.set(config.slot, item(session.player, config, placeholders))
    }

    private fun renderInfo(session: MenuSession, clan: Clan, key: String) {
        val config = cfg.menus.settingsMenu.items[key] ?: return
        val user = clan.getMember(session.player.uniqueId)
        val placeholders = commonPlaceholders(session.player, clan) +
            ("role" to if (user == null) "-" else cfg.getRoleDisplayName(clan.getUserRole(user)))
        session.set(config.slot, item(session.player, config, placeholders))
    }

    private fun openColor(player: Player) {
        val clan = clanService.getClanUser(player) ?: return
        val user = clan.getMember(player.uniqueId) ?: return
        if (!clan.hasPermission(user, ClanPerms.Settings.TOGGLE_COLOR)) {
            cfg.send(player, cfg.messages.settings.noPermission)
            return
        }
        ClanColorUX(clanService).open(player)
    }

    private fun openRoles(player: Player) {
        val clan = clanService.getClanUser(player) ?: return
        val user = clan.getMember(player.uniqueId) ?: return
        if (clan.getUserRole(user) != ClanRole.LEADER) {
            cfg.send(player, cfg.messages.settings.noPermissionRoles)
            return
        }
        editorRolesUX.open(player)
    }

    private fun addBackground(builder: MenuBuilder) {
        val background = cfg.menus.background
        if (!background.enabled) return
        val primary = backgroundItem(background.primaryMaterial)
        val secondary = backgroundItem(background.secondaryMaterial)
        background.primarySlots.forEach { builder.slot(it, primary) }
        background.secondarySlots.forEach { builder.slot(it, secondary) }
    }

    private fun backgroundItem(material: String): ItemStack =
        ItemBuilder(parseMaterial(material, Material.BLACK_STAINED_GLASS_PANE)).apply { name(" ") }.build()

    private fun item(player: Player, config: GuiItemConfig, placeholders: Map<String, String>, glow: Boolean = config.glow) =
        buildItem(config, format(player, config.name, placeholders), format(player, config.lore, placeholders), glow)

    private fun buildItem(config: GuiItemConfig, name: String, lore: List<String>, glow: Boolean): ItemStack =
        ItemBuilder(parseMaterial(config.material, Material.STONE)).apply {
            name(name)
            lore(lore)
            glow(glow)
        }.build()

    private fun commonPlaceholders(player: Player, clan: Clan): Map<String, String> = mapOf(
        "members" to clan.users.size.toString(),
        "online" to clan.onlineCount.toString(),
        "clan_color" to clan.highlightColor.displayName,
        "clan_highlight_status" to if (clan.highlightEnabled) "Включена" else "Выключена",
        "clan_highlight_type" to clan.highlightType.displayName,
        "player" to player.name,
    )

    private fun format(player: Player, template: String, placeholders: Map<String, String>) =
        cfg.formatMessage(player, template, placeholders)

    private fun format(player: Player, templates: List<String>, placeholders: Map<String, String>) =
        templates.map { format(player, it, placeholders) }

    private fun parseMaterial(name: String, fallback: Material): Material =
        runCatching { Material.valueOf(name.uppercase()) }.getOrDefault(fallback)
}
