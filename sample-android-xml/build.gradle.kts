plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "io.github.zakaria5729.phonetics.sample.xml"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.zakaria5729.phonetics.sample.xml"
        minSdk = 21
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
    implementation(project(":phonetics"))
}
