plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.fvory.psdk.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.fvory.psdk.coordinates"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "coordinate-demo-4"
    }

    sourceSets.getByName("androidTest").assets.srcDir(
        layout.buildDirectory
            .dir("testPdfFixtures")
            .get()
            .asFile,
    )

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":psdk-core"))
    implementation(libs.pdfium.android)
    // Debug only: lets the sample compare the optional Pdfium engine with PdfRenderer
    debugImplementation(project(":psdk-pdfium"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.compose.ui.tooling)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
