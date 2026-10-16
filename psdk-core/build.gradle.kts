import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

// JitPack sets VERSION to the git tag; local builds use VERSION_NAME from gradle.properties.
val sdkVersion = System.getenv("VERSION") ?: project.property("VERSION_NAME").toString()

android {
    namespace = "io.github.fvory.psdk"
    compileSdk = 36

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        minSdk = 24
        // The code only needs API 35 to compile; AGP would otherwise require compileSdk 36 in apps.
        aarMetadata { minCompileSdk = 35 }
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "SDK_VERSION", "\"$sdkVersion\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // Dependency versions are held back on purpose so older apps can still use the SDK.
        disable += setOf("GradleDependency", "NewerVersionAvailable")
    }

    compileOptions {
        // Java 8 bytecode, like AndroidX, so apps with jvmTarget 1.8 or 11 can inline SDK functions
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    testOptions {
        // android.util.Log and similar return defaults instead of throwing in JVM unit tests.
        unitTests.isReturnDefaultValues = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    explicitApi()
    // Publish a kotlin-stdlib dependency that apps on Kotlin 2.1 can still read.
    coreLibrariesVersion = "2.1.0"
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        // Keep the published metadata readable by apps on older Kotlin compilers.
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}

dependencies {
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    api(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            // JitPack sets GROUP; local builds use gradle.properties.
            groupId = System.getenv("GROUP") ?: project.property("GROUP").toString()
            artifactId = "psdk-core"
            version = sdkVersion
            afterEvaluate { from(components["release"]) }
            pom {
                name.set("PdfSignatureSDK core")
                description.set("Android PDF viewer for placing signatures and E-Materai.")
                url.set("https://github.com/FvorY/pdf-signature-sdk-android")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/license/mit")
                    }
                }
            }
        }
    }
}
