package ua.inventorytype.pnclans.impl.integration

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.ChatColor
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Locale
import java.util.jar.JarFile

/** Installs the latest stable Bukkit runtime, falling back to the newest prerelease when no stable exists. */
object PnLibraryBootstrapInstaller {
    private const val STABLE_API = "https://api.github.com/repos/pnFolder/pnLibrary/releases/latest"
    private const val RECENT_API = "https://api.github.com/repos/pnFolder/pnLibrary/releases?per_page=1"
    private const val MAX_BYTES = 512L * 1024L * 1024L
    private val assetPattern = Regex(
        "\\\"digest\\\"\\s*:\\s*\\\"sha256:([a-fA-F0-9]{64})\\\"[^}]*" +
            "\\\"browser_download_url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"",
    )

    fun ensureInstalled(plugin: JavaPlugin) {
        if (plugin.server.pluginManager.getPlugin("pnLibrary") != null) return
        try {
            install(plugin)
        } catch (error: Throwable) {
            showFailure(plugin, error)
            throw IllegalStateException(
                "Запуск pnClans остановлен: обязательную pnLibrary не удалось установить",
                error,
            )
        }
    }

    private fun install(plugin: JavaPlugin) {
        plugin.logger.warning("pnLibrary не найдена; устанавливаю последнюю стабильную версию…")
        val stable = readOrNull(STABLE_API)
        val json = stable ?: read(RECENT_API).also {
            plugin.logger.warning("Стабильного релиза pnLibrary пока нет; устанавливаю последний доступный prerelease")
        }
        val asset = assetPattern.findAll(json).map { match ->
            ReleaseAsset(
                sha256 = match.groupValues[1].lowercase(Locale.ROOT),
                url = match.groupValues[2].replace("\\/", "/"),
            )
        }.firstOrNull { candidate ->
            candidate.url.substringAfterLast('/').let {
                it.startsWith("pnLibrary-") && it.contains("-bukkit-") && it.endsWith(".jar")
            }
        } ?: error("В релизе pnLibrary отсутствует Bukkit JAR с SHA-256")
        val url = asset.url
        val plugins = plugin.dataFolder.parentFile.toPath()
        Files.createDirectories(plugins)
        val temp = Files.createTempFile(plugins, "pnlibrary-bootstrap-", ".tmp")
        try {
            download(url, temp)
            require(Files.size(temp) in 1..MAX_BYTES) { "Некорректный размер pnLibrary" }
            require(sha256(temp) == asset.sha256) { "SHA-256 pnLibrary не совпадает" }
            JarFile(temp.toFile()).use { require(it.getJarEntry("plugin.yml") != null) { "Некорректный pnLibrary JAR" } }
            val target = plugins.resolve("pnLibrary.jar")
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING)
            val loaded = plugin.server.pluginManager.loadPlugin(target.toFile())
                ?: error("Сервер не смог загрузить pnLibrary")
            plugin.server.pluginManager.enablePlugin(loaded)
            require(loaded.isEnabled) { "pnLibrary установлена, но не включилась" }
            plugin.logger.info("pnLibrary автоматически установлена и включена")
        } finally { Files.deleteIfExists(temp) }
    }

    private fun showFailure(plugin: JavaPlugin, error: Throwable) {
        val console = plugin.server.consoleSender
        val reason = error.message?.takeIf(String::isNotBlank) ?: error.javaClass.simpleName
        console.sendMessage("")
        console.sendMessage("${ChatColor.DARK_RED}          ━━━━━━━━━━━ ОШИБКА ЗАПУСКА ━━━━━━━━━━━")
        console.sendMessage("${ChatColor.RED} /\\_/\\")
        console.sendMessage("${ChatColor.RED}( x.x )     ${ChatColor.WHITE}pnClans ${ChatColor.DARK_GRAY}› ${ChatColor.RED}pnLibrary недоступна")
        console.sendMessage("${ChatColor.RED} > ^ <      ${ChatColor.GRAY}Запуск плагина остановлен")
        console.sendMessage("")
        console.sendMessage("${ChatColor.DARK_GRAY}            ◆ ${ChatColor.WHITE}Причина")
        console.sendMessage("${ChatColor.DARK_GRAY}              └ ${ChatColor.RED}$reason")
        console.sendMessage("${ChatColor.DARK_GRAY}            ◆ ${ChatColor.WHITE}Что делать")
        console.sendMessage("${ChatColor.DARK_GRAY}              ├ ${ChatColor.GRAY}Проверьте доступ сервера к GitHub")
        console.sendMessage("${ChatColor.DARK_GRAY}              ├ ${ChatColor.GRAY}Скачайте Bukkit JAR вручную:")
        console.sendMessage("${ChatColor.DARK_GRAY}              │ ${ChatColor.YELLOW}https://github.com/pnFolder/pnLibrary/releases/latest")
        console.sendMessage("${ChatColor.DARK_GRAY}              └ ${ChatColor.GRAY}Положите его в plugins и перезапустите ядро")
        console.sendMessage("")
        console.sendMessage("${ChatColor.RED}          ■ pnClans не был запущен")
        console.sendMessage("${ChatColor.DARK_RED}          ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        console.sendMessage("")
    }

    private fun read(url: String): String = connection(url).run { inputStream.bufferedReader().use { it.readText() }.also { disconnect() } }
    private fun readOrNull(url: String): String? {
        val connection = rawConnection(url)
        return when (val code = connection.responseCode) {
            in 200..299 -> connection.inputStream.bufferedReader().use { it.readText() }.also { connection.disconnect() }
            404 -> null.also { connection.disconnect() }
            else -> error("GitHub вернул HTTP $code")
        }
    }
    private fun download(url: String, target: java.nio.file.Path) {
        val c = connection(url)
        require(c.contentLengthLong < 0 || c.contentLengthLong <= MAX_BYTES) { "pnLibrary превышает 512 МБ" }
        c.inputStream.use { input -> Files.newOutputStream(target).use { output ->
            val buffer = ByteArray(16 * 1024)
            var total = 0L
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                require(total <= MAX_BYTES) { "pnLibrary превышает 512 МБ" }
                output.write(buffer, 0, count)
            }
        } }
        c.disconnect()
    }
    private fun sha256(path: java.nio.file.Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(16 * 1024)
            while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    private data class ReleaseAsset(val url: String, val sha256: String)
    private fun rawConnection(url: String) = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
        connectTimeout=8_000; readTimeout=30_000; instanceFollowRedirects=true
        setRequestProperty("Accept", "application/vnd.github+json"); setRequestProperty("User-Agent", "pnClans-pnLibrary-Bootstrap")
    }
    private fun connection(url: String) = rawConnection(url).apply {
        require(responseCode in 200..299) { "GitHub вернул HTTP $responseCode" }
    }
}
