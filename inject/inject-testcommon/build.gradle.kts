plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(1_8)
}

dependencies {
    api(projects.poket.poketFacade)
    api(libs.kotlin.reflect)
    api(libs.trekkie.kotest)
    implementation(libs.micrometer.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
