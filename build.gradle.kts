import java.text.SimpleDateFormat
import java.util.*

import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlin.jvm)
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

// Hosts the winlogon libraries that are not published to Maven Central.
val winlogonCodeRepository = "https://maven.winlogon.org/releases"

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
        url = uri(winlogonCodeRepository)
    }
    mavenCentral()
}

// The artifacts that the Paper plugin loader resolves at server start and hands to the plugin's class path.
// The version catalog is the single source of truth for their versions.
val loaderLibraries = listOf(
    libs.kotlin.stdlib,
    libs.exposed.core,
    libs.exposed.dao,
    libs.exposed.jdbc,
    libs.sqlite.jdbc,
    libs.retrohue,
    libs.asynccraftr,
)

dependencies {
    // The server provides the API and the loader-provided libraries at runtime.
    compileOnly(libs.paper.api)
    compileOnly(libs.kotlin.stdlib)
    loaderLibraries.forEach {
        compileOnly(it)
    }

    // The plugin loader only resolves libraries on a running server; tests instantiate the plugin in-process.
    testImplementation(libs.kotlin.stdlib)
    testImplementation(libs.paper.api)
    loaderLibraries.forEach {
        testImplementation(it)
    }
    testImplementation(libs.mockbukkit)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// The loader reads the coordinates it has to resolve from a generated descriptor, so that the versions it
// resolves at server start always match the ones the plugin was compiled against.
val generatedResources = layout.buildDirectory.dir("generated/resources")

val generateLoaderLibraries by tasks.registering {
    val outputDirectory = generatedResources
    val coordinates = loaderLibraries.map { library ->
        val dependency = library.get()
        "${dependency.module}:${dependency.versionConstraint.requiredVersion}"
    }

    // Custom repositories aside Maven Central
    val repositories = listOf("winlogon-code" to winlogonCodeRepository)
    inputs.property("coordinates", coordinates)
    inputs.property("repositories", repositories)

    outputs.dir(outputDirectory)

    doLast {
        val descriptor = outputDirectory.get().asFile.resolve("loader-libraries.properties")
        descriptor.parentFile.mkdirs()
        descriptor.writeText(
            buildString {
                coordinates.forEachIndexed { index, coordinate ->
                    appendLine("loader.dependency.$index=$coordinate")
                }
                repositories.forEachIndexed { index, (id, url) ->
                    appendLine("loader.repository.$index.id=$id")
                    appendLine("loader.repository.$index.type=default")
                    appendLine("loader.repository.$index.url=$url")
                }
            }
        )
    }
}

sourceSets.main {
    resources.srcDir(generatedResources)
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
    dependsOn(generateLoaderLibraries)
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
