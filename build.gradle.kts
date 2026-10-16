import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
}

allprojects {
    group = "io.github.zakaria5729"
    version = "0.1.0"
}

val libraryModules = mapOf(
    "phonetics" to "Sound-alike spelling variants and match percentages for Bangla and English, for every Kotlin target.",
    "phonetics-compose" to "Compose helpers for kotlin-phonetics: remembered instances and state that follows the input.",
)

subprojects {
    val description = libraryModules[name] ?: return@subprojects
    apply(plugin = "maven-publish")
    extensions.configure<PublishingExtension> {
        publications.withType<MavenPublication>().configureEach {
            pom {
                this.name.set(project.name)
                this.description.set(description)
                url.set("https://github.com/zakaria5729/kotlin-phonetics")
                licenses {
                    license {
                        this.name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("zakaria5729")
                        this.name.set("Zakaria Hossain")
                    }
                }
                scm {
                    url.set("https://github.com/zakaria5729/kotlin-phonetics")
                    connection.set("scm:git:https://github.com/zakaria5729/kotlin-phonetics.git")
                }
            }
        }
    }
}
