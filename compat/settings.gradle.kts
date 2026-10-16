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
        // For checking a JitPack build: -PsdkGroup=com.github.FvorY.pdf-signature-sdk-android -PsdkVersion=<tag or commit>
        maven("https://jitpack.io")
    }
}

rootProject.name = "psdk-compat"
include(":app")
