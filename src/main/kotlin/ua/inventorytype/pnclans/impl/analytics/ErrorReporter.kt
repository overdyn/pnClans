package ua.inventorytype.pnclans.impl.analytics

import org.bukkit.entity.Player
import ua.inventorytype.pnclans.BukkitPlugin
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level

/** Local-only error reporting with duplicate suppression. */
object ErrorReporter {
    private const val DUPLICATE_COOLDOWN_MS = 5_000L

    private val lastReportTimes = ConcurrentHashMap<String, Long>()
    private val totalErrorsCount = AtomicInteger(0)
    private var pluginInstance: BukkitPlugin? = null

    fun init(plugin: BukkitPlugin) {
        pluginInstance = plugin
        plugin.logger.info("[ErrorReporter] Локальная система сбора ошибок включена.")
    }

    fun shutdown() {
        pluginInstance = null
        lastReportTimes.clear()
        totalErrorsCount.set(0)
    }

    fun report(
        context: String,
        throwable: Throwable,
        player: Player? = null,
        extraData: Map<String, String> = emptyMap()
    ) {
        val plugin = pluginInstance ?: return
        val rootCause = getRootCause(throwable)
        val exceptionKey = "${rootCause.javaClass.name}:${rootCause.stackTrace.firstOrNull()}"
        val now = System.currentTimeMillis()
        val previous = lastReportTimes.putIfAbsent(exceptionKey, now)
        if (previous != null && now - previous < DUPLICATE_COOLDOWN_MS) return
        lastReportTimes[exceptionKey] = now

        val details = buildString {
            append("[pnClans] ")
            append(context)
            player?.let { append(" (player=${it.name})") }
            if (extraData.isNotEmpty()) append(" ").append(extraData)
            append("; error #").append(totalErrorsCount.incrementAndGet())
        }
        plugin.logger.log(Level.SEVERE, details, throwable)
        if (plugin.hasPnLibraryIntegration()) {
            plugin.pnLibraryIntegration.reportError(
                context = context,
                throwable = throwable,
                fields = mapOf(
                    "playerPresent" to (player != null),
                    "extraFieldNames" to extraData.keys.sorted(),
                ),
            )
        }
    }

    private fun getRootCause(throwable: Throwable): Throwable {
        var cause = throwable
        while (cause.cause != null && cause.cause !== cause) {
            cause = cause.cause!!
        }
        return cause
    }
}
