import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.mavenPublish) apply false
}

allprojects {
    group = "io.github.zakaria5729"
    version = "0.1.1"
}

val libraryModules = mapOf(
    "phonetics" to "Sound-alike spelling variants and match percentages for Bangla and English, for every Kotlin target.",
    "phonetics-compose" to "Compose helpers for kotlin-phonetics: remembered instances and state that follows the input.",
)

subprojects {
    val moduleDescription = libraryModules[name] ?: return@subprojects
    apply(plugin = "com.vanniktech.maven.publish")
    extensions.configure<MavenPublishBaseExtension> {
        publishToMavenCentral()
        coordinates(group.toString(), name, version.toString())
        pom {
            this.name.set(project.name)
            description.set(moduleDescription)
            inceptionYear.set("2026")
            url.set("https://github.com/zakaria5729/kotlin_phonetics")
            licenses {
                license {
                    this.name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("zakaria5729")
                    this.name.set("Zakaria Hossain")
                    url.set("https://github.com/zakaria5729")
                }
            }
            scm {
                url.set("https://github.com/zakaria5729/kotlin_phonetics")
                connection.set("scm:git:https://github.com/zakaria5729/kotlin_phonetics.git")
                developerConnection.set("scm:git:ssh://git@github.com/zakaria5729/kotlin_phonetics.git")
            }
        }
    }
}
