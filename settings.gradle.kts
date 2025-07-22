rootProject.name = "poket"


enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(
    ":poket-facade",

    ":backends:poket-caffeine",
    ":backends:poket-lettuce",
    ":backends:poket-redisson",

    ":inject:poket-guice",
    ":inject:poket-koin",
    ":inject:inject-testcommon",

    ":serialization:poket-jackson",
    ":serialization:poket-snakeyaml",
)
