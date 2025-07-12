@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.publishOnCentral)
    alias(libs.plugins.dokka)
    `java-library`
}

repositories {
    mavenCentral()
}

group = "io.github.andresviedma"
val poketVersion: String by project
version = poketVersion

val Provider<PluginDependency>.id: String get() = get().pluginId

allprojects {
    group = rootProject.group
    version = rootProject.version

    with(rootProject.libs.plugins) {
        apply(plugin = kotlin.jvm.id)
        apply(plugin = publishOnCentral.id)
        apply(plugin = dokka.id)
    }

    repositories {
        mavenCentral()
    }

    publishOnCentral {
        val repoOwner = "andresviedma"
        projectLongName.set("Trekkie test")
        projectDescription.set("Kotlin testing specification framework inspired by Spock framework")
        scmConnection.set("scm:git:https://github.com/$repoOwner/${rootProject.name}")
        projectUrl.set("https://github.com/$repoOwner/${rootProject.name}")
        licenseName.set("Apache License 2.0")
        licenseUrl.set("https://www.apache.org/licenses/LICENSE-2.0")
    }

    publishing.publications.withType<MavenPublication>().configureEach {
        pom {
            developers {
                developer {
                    id.set("andresviedma")
                    name.set("Andrés Viedma")
                    email.set("andres.viedma@gmail.com")
                }
            }
        }
    }
}
