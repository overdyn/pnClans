package ua.inventorytype.pnclans.impl.ux

import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import ru.privatenull.pnlibrary.api.tasks.TaskHandle
import ua.inventorytype.pnclans.BukkitPlugin
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Displays one self-cleaning timed BossBar per player.
 *
 * Starting a new bar for the same player replaces the old one. Progress decreases every
 * server tick and all bars are removed when their time expires, their player quits, or
 * the plugin shuts down.
 *
 * @param plugin The owning plugin used for scheduling and listener registration.
 */
class TimedBossBarService(private val plugin: BukkitPlugin) : Listener {

    private data class ActiveBar(
        val bar: BossBar,
        val task: TaskHandle
    )

    private val activeBars = ConcurrentHashMap<UUID, ActiveBar>()

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    /**
     * Shows a timed BossBar with an animated countdown.
     *
     * @param player The player who sees the BossBar.
     * @param title Already formatted BossBar title.
     * @param color Configured BossBar color name.
     * @param style Configured BossBar style name.
     * @param durationSeconds Lifetime in seconds, clamped to at least one second.
     */
    fun show(player: Player, title: String, color: String, style: String, durationSeconds: Int) {
        remove(player)

        val durationTicks = durationSeconds.coerceAtLeast(1).toLong() * TICKS_PER_SECOND
        val bar = Bukkit.createBossBar(
            title,
            parseColor(color),
            parseStyle(style)
        )
        bar.progress = 1.0
        bar.addPlayer(player)

        var elapsedTicks = 0L
        val task = plugin.pnLibraryIntegration.tasks.repeatEntity(
            player, Duration.ofMillis(50), Duration.ofMillis(50), Runnable {
            val active = activeBars[player.uniqueId]
            if (active?.bar !== bar || !player.isOnline) {
                remove(player)
                return@Runnable
            }

            elapsedTicks += 1
            bar.progress = ((durationTicks - elapsedTicks).toDouble() / durationTicks).coerceIn(0.0, 1.0)

            if (elapsedTicks >= durationTicks) {
                remove(player)
            }
        })

        activeBars[player.uniqueId] = ActiveBar(bar, task)
    }

    /** Removes the active BossBar for the given player, if any. */
    fun remove(player: Player) {
        activeBars.remove(player.uniqueId)?.let { active ->
            active.task.cancel()
            active.bar.removeAll()
        }
    }

    /** Removes every managed BossBar. Intended for plugin shutdown. */
    fun clearAll() {
        activeBars.keys.toList().forEach { uuid ->
            activeBars.remove(uuid)?.let { active ->
                active.task.cancel()
                active.bar.removeAll()
            }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        remove(event.player)
    }

    private fun parseColor(value: String): BarColor =
        COLOR_CACHE.computeIfAbsent(value.uppercase()) { key ->
            runCatching { BarColor.valueOf(key) }.getOrDefault(BarColor.YELLOW)
        }

    private fun parseStyle(value: String): BarStyle =
        STYLE_CACHE.computeIfAbsent(value.uppercase()) { key ->
            runCatching { BarStyle.valueOf(key) }.getOrDefault(BarStyle.SOLID)
        }

    private companion object {
        const val TICKS_PER_SECOND = 20L
        private val COLOR_CACHE = ConcurrentHashMap<String, BarColor>()
        private val STYLE_CACHE = ConcurrentHashMap<String, BarStyle>()
    }
}
