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
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    explicitApi()
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}

dependencies {
    api(project(":psdk-core"))
    implementation("io.legere:pdfiumandroid:2.0.1")
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
