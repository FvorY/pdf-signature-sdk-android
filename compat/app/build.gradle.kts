plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val sdkVersion = providers.gradleProperty("sdkVersion").getOrElse("0.0.0")
// Many apps (including Flutter templates) still compile Kotlin for JVM 1.8 or 11
val jvm = providers.gradleProperty("jvmTarget").getOrElse("17")
val withPdfium = providers.gradleProperty("pdfium").getOrElse("false").toBoolean()

android {
    namespace = "demo.compat"
    compileSdk = providers.gradleProperty("compileSdk").getOrElse("36").toInt()
    defaultConfig {
        applicationId = "demo.compat"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(jvm)
        targetCompatibility = JavaVersion.toVersion(jvm)
    }
    kotlinOptions { jvmTarget = jvm }
    buildFeatures { compose = true }
    if (providers.gradleProperty("disableLifecycleLint").isPresent) {
        // Lint in AGP 8.6–8.8 crashes on this lifecycle 2.10 check (documented workaround)
        lint { disable += "NullSafeMutableLiveData" }
    }
}

dependencies {
    implementation("io.github.fvory.psdk:psdk-core:$sdkVersion")
    if (withPdfium) implementation("io.github.fvory.psdk:psdk-pdfium:$sdkVersion")
    implementation("androidx.appcompat:appcompat:1.7.0")
    // An older Compose BOM than the SDK's, as many apps have
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
}
