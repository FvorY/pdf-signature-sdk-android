import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

// JitPack sets VERSION to the git tag; local builds use VERSION_NAME from gradle.properties.
val sdkVersion = System.getenv("VERSION") ?: project.property("VERSION_NAME").toString()

android {
    namespace = "io.github.fvory.psdk.pdfium"
    compileSdk = 36

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        minSdk = 24
        // The code only needs API 35 to compile; AGP would otherwise require compileSdk 36 in apps.
        aarMetadata { minCompileSdk = 35 }
        consumerProguardFiles("consumer-rules.pro")
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
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}

dependencies {
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    api(project(":psdk-core"))
    implementation(libs.pdfium.android)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = System.getenv("GROUP") ?: project.property("GROUP").toString()
            artifactId = "psdk-pdfium"
            version = sdkVersion
            afterEvaluate { from(components["release"]) }
            pom {
                name.set("PdfSignatureSDK Pdfium engine")
                description.set("Optional Pdfium engine: opens password-protected PDFs on Android 14 and lower.")
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
