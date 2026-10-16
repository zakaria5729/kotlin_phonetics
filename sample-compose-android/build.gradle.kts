plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "io.github.zakaria5729.phonetics.sample.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.zakaria5729.phonetics.sample.compose"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":sample-compose"))
    implementation(libs.androidx.activity.compose)
}
