import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

plugins {
    id("com.gradleup.shadow") version "8.3.5"
    id("java")
}

group = "org.winlogon.simplewarp"

fun getTime(): String {
    val sdf = SimpleDateFormat("yyMMdd-HHmm")
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}

// Set version to version property if supplied
val shortVersion: String? = if (project.hasProperty("ver")) {
    val ver = project.property("ver") as String
    if (ver.startsWith("v")) {
        ver.substring(1).uppercase()
    } else {
        ver.uppercase()
    }
} else null

// If the tag includes "-RC-" or no tag is supplied, append "-SNAPSHOT"
val version = when {
    shortVersion.isNullOrEmpty() -> "${getTime()}-SNAPSHOT"
    shortVersion.contains("-RC-") -> "${shortVersion.substringBefore("-RC-")}-SNAPSHOT"
    else -> shortVersion
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
        content {
            includeModule("io.papermc.paper", "paper-api")
            includeModule("io.papermc", "paperlib")
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
        name = "jitpack"
        url = uri("https://jitpack.io")
        content {
            includeModule("com.github.walker84837", "JResult")
        }
    }

    mavenCentral()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation("io.papermc:paperlib:1.0.8")
    implementation("com.github.walker84837:JResult:1.1.0")
    testImplementation("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    filesMatching("**/plugin.yml") {
        expand(
            "NAME" to rootProject.name,
            "VERSION" to version,
            "PACKAGE" to project.group.toString()
        )
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("io.papermc.lib", "shadow.io.papermc.paperlib")
    minimize()
}

// Disable jar and replace with shadowJar
tasks.jar {
    enabled = false
}
tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.register("printProjectName") {
    doLast {
        println(rootProject.name)
    }
}

tasks.register("release") {
    dependsOn(tasks.build)

    doLast {
        if (!version.endsWith("-SNAPSHOT")) {
            val shadowJarFile = tasks.shadowJar.get().archiveFile.get().asFile
            val targetFile = File("${layout.buildDirectory.get()}/libs/${rootProject.name}.jar")
            shadowJarFile.renameTo(targetFile)
        }
    }
}
