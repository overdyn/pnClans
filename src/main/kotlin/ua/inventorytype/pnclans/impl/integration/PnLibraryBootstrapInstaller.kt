package ua.inventorytype.pnclans.impl.integration

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.ChatColor
import ru.privatenull.pnlibrary.api.PnLibrary
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
    private const val TAG_API = "https://api.github.com/repos/pnFolder/pnLibrary/releases/tags/"
    private const val MAX_BYTES = 512L * 1024L * 1024L
    private val assetPattern = Regex(
        "\\\"digest\\\"\\s*:\\s*\\\"sha256:([a-fA-F0-9]{64})\\\"[^}]*" +
            "\\\"browser_download_url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"",
    )

    fun ensureInstalled(plugin: JavaPlugin, minimumVersion: String): Boolean {
        if (plugin.server.pluginManager.getPlugin("pnLibrary") != null) return true
        try {
            install(plugin, minimumVersion)
            return true
        } catch (error: Throwable) {
            showFailure(plugin, error)
            return false
        }
    }

    fun ensureCompatible(plugin: JavaPlugin, minimumVersion: String): Boolean {
        val library = plugin.server.servicesManager.load(PnLibrary::class.java)
        if (library == null) {
            showFailure(plugin, IllegalStateException("pnLibrary загружена, но не зарегистрировала API"))
            return false
        }
        if (isAtLeastVersion(library.version, minimumVersion)) return true
        showOutdated(plugin, library.version, minimumVersion)
        return false
    }

    private fun install(plugin: JavaPlugin, minimumVersion: String) {
        val stable = readOrNull(STABLE_API)
        val stableVersion = stable?.let(::releaseVersion)
        val useStable = stableVersion != null && isAtLeastVersion(stableVersion, minimumVersion)
        val json = if (useStable) requireNotNull(stable) else read(TAG_API + "v$minimumVersion").also {
            plugin.logger.warning("Стабильная pnLibrary не подходит; выбран совместимый релиз v$minimumVersion")
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
        showInstallStart(plugin, asset, useStable)
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
            showInstallSuccess(plugin, loaded.description.version, Files.size(target))
        } finally { Files.deleteIfExists(temp) }
    }

    private fun showInstallStart(plugin: JavaPlugin, asset: ReleaseAsset, stable: Boolean) {
        val console = plugin.server.consoleSender
        val channel = if (stable) "STABLE • стабильный канал" else "PRERELEASE • предварительный канал"
        console.sendMessage("")
        console.sendMessage("${ChatColor.GOLD}          ━━━━━━━━━━━ УСТАНОВКА PNLIBRARY ━━━━━━━━━━━")
        console.sendMessage("${ChatColor.YELLOW} /\\_/\\")
        console.sendMessage("${ChatColor.YELLOW}( o.o )     ${ChatColor.WHITE}pnClans ${ChatColor.DARK_GRAY}› ${ChatColor.YELLOW}подготовка зависимости")
        console.sendMessage("${ChatColor.YELLOW} > ^ <      ${ChatColor.GRAY}pnLibrary не найдена на ядре")
        console.sendMessage("")
        console.sendMessage("${ChatColor.DARK_GRAY}            ┌ ${ChatColor.WHITE}Источник     ${ChatColor.GRAY}GitHub Releases")
        console.sendMessage("${ChatColor.DARK_GRAY}            ├ ${ChatColor.WHITE}Канал        ${ChatColor.YELLOW}$channel")
        console.sendMessage("${ChatColor.DARK_GRAY}            ├ ${ChatColor.WHITE}Платформа    ${ChatColor.YELLOW}Bukkit / Paper")
        console.sendMessage("${ChatColor.DARK_GRAY}            ├ ${ChatColor.WHITE}Файл         ${ChatColor.GRAY}${asset.url.substringAfterLast('/')}")
        console.sendMessage("${ChatColor.DARK_GRAY}            └ ${ChatColor.WHITE}Проверка     ${ChatColor.YELLOW}SHA-256 от GitHub")
        console.sendMessage("")
        console.sendMessage("${ChatColor.YELLOW}          ◆ Скачиваю и проверяю библиотеку…")
        console.sendMessage("${ChatColor.GOLD}          ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        console.sendMessage("")
    }

    private fun showInstallSuccess(plugin: JavaPlugin, version: String, bytes: Long) {
        val console = plugin.server.consoleSender
        val sizeMiB = String.format(Locale.US, "%.2f МБ", bytes / 1024.0 / 1024.0)
        console.sendMessage("")
        console.sendMessage("${ChatColor.DARK_GREEN}          ━━━━━━━━━━━ PNLIBRARY ГОТОВА ━━━━━━━━━━━")
        console.sendMessage("${ChatColor.GREEN} /\\_/\\")
        console.sendMessage("${ChatColor.GREEN}( ^.^ )     ${ChatColor.WHITE}pnLibrary ${ChatColor.DARK_GRAY}› ${ChatColor.GREEN}установлена")
        console.sendMessage("${ChatColor.GREEN} > ^ <      ${ChatColor.GRAY}Общая система pnFolder подключена")
        console.sendMessage("")
        console.sendMessage("${ChatColor.DARK_GRAY}            ┌ ${ChatColor.WHITE}Версия       ${ChatColor.GREEN}$version")
        console.sendMessage("${ChatColor.DARK_GRAY}            ├ ${ChatColor.WHITE}Размер       ${ChatColor.GRAY}$sizeMiB")
        console.sendMessage("${ChatColor.DARK_GRAY}            ├ ${ChatColor.WHITE}Целостность  ${ChatColor.GREEN}[ OK ]")
        console.sendMessage("${ChatColor.DARK_GRAY}            └ ${ChatColor.WHITE}Состояние    ${ChatColor.GREEN}включена")
        console.sendMessage("")
        console.sendMessage("${ChatColor.GREEN}          ■ pnClans продолжает запуск")
        console.sendMessage("${ChatColor.DARK_GREEN}          ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        console.sendMessage("")
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

    private fun showOutdated(plugin: JavaPlugin, installed: String, required: String) {
        val console = plugin.server.consoleSender
        console.sendMessage("")
        console.sendMessage("${ChatColor.DARK_RED}          ━━━━━━━━━━━ ТРЕБУЕТСЯ ОБНОВЛЕНИЕ ━━━━━━━━━━━")
        console.sendMessage("${ChatColor.RED} /\\_/\\")
        console.sendMessage("${ChatColor.RED}( x.x )     ${ChatColor.WHITE}pnClans ${ChatColor.DARK_GRAY}› ${ChatColor.RED}несовместимая pnLibrary")
        console.sendMessage("${ChatColor.RED} > ^ <      ${ChatColor.GRAY}Запуск плагина остановлен")
        console.sendMessage("")
        console.sendMessage("${ChatColor.DARK_GRAY}            ┌ ${ChatColor.WHITE}Установлена  ${ChatColor.RED}$installed")
        console.sendMessage("${ChatColor.DARK_GRAY}            ├ ${ChatColor.WHITE}Требуется    ${ChatColor.YELLOW}$required или новее")
        console.sendMessage("${ChatColor.DARK_GRAY}            └ ${ChatColor.WHITE}Состояние    ${ChatColor.RED}обновление обязательно")
        console.sendMessage("")
        console.sendMessage("${ChatColor.DARK_GRAY}            ◆ ${ChatColor.WHITE}Как исправить")
        console.sendMessage("${ChatColor.DARK_GRAY}              ├ ${ChatColor.GRAY}Скачайте актуальный Bukkit JAR:")
        console.sendMessage("${ChatColor.DARK_GRAY}              │ ${ChatColor.YELLOW}https://github.com/pnFolder/pnLibrary/releases/latest")
        console.sendMessage("${ChatColor.DARK_GRAY}              └ ${ChatColor.GRAY}Замените pnLibrary в plugins и перезапустите ядро")
        console.sendMessage("")
        console.sendMessage("${ChatColor.RED}          ■ pnClans не был запущен")
        console.sendMessage("${ChatColor.DARK_RED}          ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        console.sendMessage("")
    }

    private fun read(url: String): String = connection(url).run { inputStream.bufferedReader().use { it.readText() }.also { disconnect() } }
    private fun releaseVersion(json: String): String? =
        Regex("\\\"tag_name\\\"\\s*:\\s*\\\"v?([^\\\"]+)\\\"").find(json)?.groupValues?.get(1)
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
    private data class Version(val major: Int, val minor: Int, val patch: Int, val pre: List<String>) : Comparable<Version> {
        override fun compareTo(other: Version): Int {
            compareValuesBy(this, other, Version::major, Version::minor, Version::patch).takeIf { it != 0 }?.let { return it }
            if (pre.isEmpty()) return if (other.pre.isEmpty()) 0 else 1
            if (other.pre.isEmpty()) return -1
            for (index in 0 until maxOf(pre.size, other.pre.size)) {
                val left = pre.getOrNull(index) ?: return -1
                val right = other.pre.getOrNull(index) ?: return 1
                val leftNumber = left.toIntOrNull()
                val rightNumber = right.toIntOrNull()
                val compared = when {
                    leftNumber != null && rightNumber != null -> leftNumber.compareTo(rightNumber)
                    leftNumber != null -> -1
                    rightNumber != null -> 1
                    else -> left.compareTo(right, ignoreCase = true)
                }
                if (compared != 0) return compared
            }
            return 0
        }

        companion object {
            fun parse(raw: String): Version {
                val value = raw.removePrefix("v").substringBefore('+')
                val base = value.substringBefore('-').split('.')
                return Version(
                    base.getOrNull(0)?.toIntOrNull() ?: 0,
                    base.getOrNull(1)?.toIntOrNull() ?: 0,
                    base.getOrNull(2)?.toIntOrNull() ?: 0,
                    value.substringAfter('-', "").split('.').filter(String::isNotBlank),
                )
            }
        }
    }
    private fun isAtLeastVersion(installed: String, required: String): Boolean =
        Version.parse(installed) >= Version.parse(required)
    private fun rawConnection(url: String) = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
        connectTimeout=8_000; readTimeout=30_000; instanceFollowRedirects=true
        setRequestProperty("Accept", "application/vnd.github+json"); setRequestProperty("User-Agent", "pnClans-pnLibrary-Bootstrap")
    }
    private fun connection(url: String) = rawConnection(url).apply {
        require(responseCode in 200..299) { "GitHub вернул HTTP $responseCode" }
    }
}
