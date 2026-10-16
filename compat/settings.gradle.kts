pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        val agp = providers.gradleProperty("agp").getOrElse("8.9.1")
        val kotlin = providers.gradleProperty("kotlin").getOrElse("2.1.0")
        id("com.android.application") version agp
        id("org.jetbrains.kotlin.android") version kotlin
        id("org.jetbrains.kotlin.plugin.compose") version kotlin
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "psdk-compat"
include(":app")
