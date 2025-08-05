import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val ktlint by configurations.creating

plugins {
    kotlin("jvm") version "2.2.0"
    id("com.gradleup.shadow") version "8.3.9"
}

group = "me.quantiom"
version = "1.2.9"
description = "AdvancedVanish"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.alessiodp.com/releases/")
}

val versions =
    mapOf(
        "paperApi" to "1.21.4-R0.1-SNAPSHOT",
        "acfPaper" to "0.5.1-SNAPSHOT",
        "lombok" to "1.18.38",
        "libby" to "1.3.1",
        "kotlinStdlib" to "2.2.0",
        "protocolLib" to "5.3.0",
        "placeholderApi" to "2.11.6",
        "exposed" to "0.61.0",
        "luckperms" to "5.5",
        "ktlint" to "1.7.1",
    )

dependencies {
    api("co.aikar:acf-paper:${versions["acfPaper"]}")
    api("org.projectlombok:lombok:${versions["lombok"]}")
    api("net.byteflux:libby-bukkit:${versions["libby"]}")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:${versions["kotlinStdlib"]}")
    compileOnly("io.papermc.paper:paper-api:${versions["paperApi"]}")
    compileOnly("com.comphenix.protocol:ProtocolLib:${versions["protocolLib"]}")
    compileOnly("me.clip:placeholderapi:${versions["placeholderApi"]}")
    compileOnly("org.jetbrains.exposed:exposed-core:${versions["exposed"]}")
    compileOnly("org.jetbrains.exposed:exposed-dao:${versions["exposed"]}")
    compileOnly("org.jetbrains.exposed:exposed-jdbc:${versions["exposed"]}")
    compileOnly("net.luckperms:api:${versions["luckperms"]}")
    ktlint("com.pinterest.ktlint:ktlint-cli:${versions["ktlint"]}") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}
sourceSets {
    main {
        java {
            srcDirs("src/main/kotlin") // Ensure Gradle is compiling files from this directory
        }
    }
}
tasks {
    named<ProcessResources>("processResources") {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    register<JavaExec>("ktlintCheck") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Check Kotlin code style"
        classpath = configurations.getByName("ktlint")
        mainClass.set("com.pinterest.ktlint.Main")
        args("**/src/**/*.kt", "**.kts", "!**/build/**")
    }

    register<JavaExec>("ktlintFormat") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Format Kotlin code"
        classpath = configurations.getByName("ktlint")
        mainClass.set("com.pinterest.ktlint.Main")
        args("-F", "**/src/**/*.kt", "**.kts", "!**/build/**")
    }
    withType<ShadowJar> {
        archiveClassifier.set("shaded")

        relocate("net.byteflux.libby", "me.quantiom.libs.libby")
        relocate("co.aikar.commands", "me.quantiom.libs.acf")

        minimize {
            exclude(dependency("org.jetbrains.exposed:.*:.*"))
            exclude(dependency("org.jetbrains.kotlin:.*:.*"))
            exclude(dependency("org.jetbrains.kotlinx:.*:.*"))
        }

        exclude(
            "META-INF/*.SF",
            "META-INF/*.DSA",
            "META-INF/*.RSA",
            "META-INF/LICENSE*",
            "META-INF/NOTICE*",
            "META-INF/DEPENDENCIES",
            "META-INF/maven/**",
            "META-INF/versions/**",
            "META-INF/services/javax.*",
            "**/*.html",
            "**/*.txt",
            "**/*.properties",
            "**/*.kotlin_module",
            "**/*.kotlin_metadata",
            "**/*.kotlin_builtins",
        )

        mergeServiceFiles()

        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Built-By" to System.getProperty("user.name"),
            )
        }
    }

    named("build") {
        dependsOn("shadowJar")
    }
}
