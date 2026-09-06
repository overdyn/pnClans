package ua.inventorytype.pnclans.impl.integration

import org.bukkit.plugin.java.JavaPlugin
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Locale
import java.util.jar.JarFile

/** Installs the latest stable Bukkit runtime when pnLibrary is absent. */
object PnLibraryBootstrapInstaller {
    private const val API = "https://api.github.com/repos/pnFolder/pnLibrary/releases/latest"
    private const val MAX_BYTES = 512L * 1024L * 1024L
    private val assetPattern = Regex("\\\"browser_download_url\\\"\\s*:\\s*\\\"([^\\\"]*pnLibrary-bukkit-[^\\\"]*\\.jar)\\\"")
    private val checksumsPattern = Regex("\\\"browser_download_url\\\"\\s*:\\s*\\\"([^\\\"]*checksums\\.sha256)\\\"")

    fun ensureInstalled(plugin: JavaPlugin) {
        if (plugin.server.pluginManager.getPlugin("pnLibrary") != null) return
        plugin.logger.warning("pnLibrary не найдена; устанавливаю последнюю стабильную версию…")
        val json = read(API)
        val url = assetPattern.find(json)?.groupValues?.get(1)?.replace("\\/", "/")
            ?: error("В последнем стабильном релизе pnLibrary отсутствует Bukkit JAR")
        val checksumsUrl = checksumsPattern.find(json)?.groupValues?.get(1)?.replace("\\/", "/")
            ?: error("В последнем стабильном релизе pnLibrary отсутствует checksums.sha256")
        val plugins = plugin.dataFolder.parentFile.toPath()
        Files.createDirectories(plugins)
        val temp = Files.createTempFile(plugins, "pnlibrary-bootstrap-", ".tmp")
        try {
            download(url, temp)
            require(Files.size(temp) in 1..MAX_BYTES) { "Некорректный размер pnLibrary" }
            val fileName = url.substringAfterLast('/').substringBefore('?')
            val expected = read(checksumsUrl).lineSequence().map(String::trim)
                .firstOrNull { it.endsWith(fileName) }?.substringBefore(' ')?.lowercase(Locale.ROOT)
                ?: error("В checksums.sha256 отсутствует $fileName")
            require(sha256(temp) == expected) { "SHA-256 pnLibrary не совпадает" }
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

    private fun read(url: String): String = connection(url).run { inputStream.bufferedReader().use { it.readText() }.also { disconnect() } }
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
    private fun connection(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout=8_000; readTimeout=30_000; instanceFollowRedirects=true
        setRequestProperty("Accept", "application/vnd.github+json"); setRequestProperty("User-Agent", "pnClans-pnLibrary-Bootstrap")
        require(responseCode in 200..299) { "GitHub вернул HTTP $responseCode" }
    }
}
