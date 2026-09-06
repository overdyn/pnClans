package ua.inventorytype.pnclans.impl.util

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerQuitEvent
import ru.privatenull.pnlibrary.api.tasks.TaskHandle
import ua.inventorytype.pnclans.BukkitPlugin
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe, NMS-free chat input listener for temporary text prompts.
 *
 * Each player may have one active prompt. A replacement, timeout, chat response, player quit,
 * or explicit cancellation always removes the corresponding scheduled task.
 */
object ChatInputPrompt {

    private data class PromptSession(
        val onInput: (String) -> Unit,
        val onTimeout: () -> Unit,
        val timeoutTask: TaskHandle
    )

    private val activePrompts = ConcurrentHashMap<UUID, PromptSession>()
    private var registeredPlugin: BukkitPlugin? = null

    private fun ensureListener(plugin: BukkitPlugin) {
        if (registeredPlugin === plugin) return

        registeredPlugin = plugin
        Bukkit.getPluginManager().registerEvents(object : Listener {
            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            fun onChat(event: AsyncPlayerChatEvent) {
                val session = activePrompts.remove(event.player.uniqueId) ?: return
                event.isCancelled = true
                session.timeoutTask.cancel()
                val message = event.message.trim()

                plugin.pnLibraryIntegration.tasks.entity(event.player, Runnable {
                    session.onInput(message)
                })
            }

            @EventHandler
            fun onQuit(event: PlayerQuitEvent) {
                cancel(event.player)
            }
        }, plugin)
    }

    /**
     * Starts a timed chat prompt for a player.
     *
     * @param plugin The owning plugin used for listener registration and scheduling.
     * @param player The player expected to provide a chat response.
     * @param timeoutTicks Maximum wait time before [onTimeout] runs.
     * @param onInput Called on the primary server thread after a response is received.
     * @param onTimeout Called on the primary server thread when the prompt expires.
     */
    fun prompt(
        plugin: BukkitPlugin,
        player: Player,
        timeoutTicks: Long,
        onInput: (String) -> Unit,
        onTimeout: () -> Unit
    ) {
        ensureListener(plugin)
        cancel(player)

        val playerId = player.uniqueId
        lateinit var session: PromptSession
        val timeoutTask = plugin.pnLibraryIntegration.tasks.laterEntity(player, Duration.ofMillis(timeoutTicks.coerceAtLeast(1L) * 50L), Runnable {
            if (activePrompts.remove(playerId, session)) {
                session.onTimeout()
            }
        })
        session = PromptSession(onInput, onTimeout, timeoutTask)
        activePrompts[playerId] = session
        player.closeInventory()
    }

    /** Cancels a player's active prompt without dispatching an input or timeout callback. */
    fun cancel(player: Player) {
        activePrompts.remove(player.uniqueId)?.timeoutTask?.cancel()
    }

    /** Cancels every active prompt. Intended for plugin shutdown. */
    fun cancelAll() {
        activePrompts.values.forEach { it.timeoutTask.cancel() }
        activePrompts.clear()
    }

    /** Clears active sessions and allows a fresh plugin instance to register its listener. */
    fun shutdown() {
        cancelAll()
        registeredPlugin = null
    }
}
