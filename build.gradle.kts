import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.20-Beta2"
    kotlin("plugin.serialization") version "2.4.20-Beta2"
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

version = "1.3.0-beta.4"

val javaTarget = providers.gradleProperty("javaTarget").getOrElse("25")
    .also { require(it == "21" || it == "25") { "javaTarget must be 21 or 25" } }

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")

    //KGui
    maven("https://jitpack.io")

    //PlaceholderApi
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")

    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.charleskorn.kaml:kaml:0.67.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")
    compileOnly("ru.privatenull:pnlibrary-api:2.0.0-beta.4")
    compileOnly("ru.privatenull:pnlibrary-core:2.0.0-beta.4") { isTransitive = false }
    compileOnly("ru.privatenull:pnlibrary-bukkit:2.0.0-beta.4") { isTransitive = false }

    compileOnly("me.clip:placeholderapi:2.12.3")
    implementation("com.github.retrooper:packetevents-spigot:2.13.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(javaTarget.toInt())
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaTarget))
    }
}

tasks {
    shadowJar {
        archiveFileName.set("pnClans-${project.version}-paper-mc1.21.11-java${javaTarget}.jar")
        // PacketEvents must be isolated when bundled to avoid conflicts with
        // another plugin's embedded or standalone PacketEvents installation.
        relocate("com.github.retrooper", "ua.inventorytype.pnclans.libs")
        relocate("io.github.retrooper", "ua.inventorytype.pnclans.libs")
    }

    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion("1.21.11")
        jvmArgs("-Xms2G", "-Xmx2G")
        pluginJars.from(shadowJar.flatMap { it.archiveFile })
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
