import java.text.SimpleDateFormat
import java.util.*

import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("com.gradleup.shadow") version "9.3.0"
    kotlin("jvm") version "2.3.20"
}

group = "org.winlogon.template"

val minecraftVersion = libs.versions.minecraft.get()

// Plugins declare an api-version of "major.minor" (e.g. "26.1"); patches don't change the API.
val apiVersion = minecraftVersion.substringBeforeLast(".")

// The catalog cannot interpolate a version into a module name, so fail the build on drift.
val mockBukkitModule = libs.mockbukkit.get().module.toString()
check(minecraftVersion in mockBukkitModule) {
    "MockBukkit module '$mockBukkitModule' does not target Minecraft $minecraftVersion; " +
        "update it in gradle/libs.versions.toml"
}

fun getTime(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date()).toString()
}

val shortVersion: String? = if (project.hasProperty("ver")) {
    val ver = project.property("ver").toString()
    if (ver.startsWith("v")) {
        ver.substring(1).uppercase()
    } else {
        ver.uppercase()
    }
} else {
    null
}

val version: String = when {
    shortVersion.isNullOrEmpty() -> "${getTime()}-SNAPSHOT"
    shortVersion.contains("-RC-") -> shortVersion.substringBefore("-RC-") + "-SNAPSHOT"
    else -> shortVersion
}

val pluginName = rootProject.name
val pluginVersion = version
val pluginPackage = project.group.toString()
val projectName = rootProject.name

// Paper 26.x is compiled against Java 25.
kotlin {
    jvmToolchain(25)
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
        content {
            includeModule("io.papermc.paper", "paper-api")
            includeModule("net.md-5", "bungeecord-chat")
        }
    }
    maven {
        name = "minecraft"
        url = uri("https://libraries.minecraft.net")
        content {
            includeModule("com.mojang", "brigadier")
        }
    }
    maven {
        name = "winlogon-code"
        url = uri("https://maven.winlogon.org/releases")
    }
    mavenCentral()
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.retrohue)
    compileOnly(libs.asynccraftr)

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    runtimeOnly(libs.sqlite.jdbc)

    // MockBukkit keeps paper-api compileOnly, so the test classpath needs its own copy.
    testImplementation(libs.paper.api)
    // Loader.kt resolves these from Maven at server start; tests instantiate the plugin in-process.
    testImplementation(libs.retrohue)
    testImplementation(libs.asynccraftr)

    testImplementation(libs.mockbukkit)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = true
    }
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    filesMatching("**/paper-plugin.yml") {
        expand(
            "NAME" to pluginName,
            "VERSION" to pluginVersion,
            "PACKAGE" to pluginPackage,
            "API_VERSION" to apiVersion
        )
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    minimize()
}

// Disable jar and replace with shadowJar
tasks.jar {
    enabled = false
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

// Utility tasks
tasks.register("printProjectName") {
    doLast {
        println(projectName)
    }
}

var shadowJarTask = tasks.shadowJar.get()
tasks.register("release") {
    dependsOn(tasks.build)
    doLast {
        if (!version.endsWith("-SNAPSHOT")) {
            shadowJarTask.archiveFile.get().asFile.renameTo(
                file("${layout.buildDirectory.get()}/libs/${rootProject.name}.jar")
            )
        }
    }
}
