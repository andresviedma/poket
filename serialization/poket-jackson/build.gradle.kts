plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(1_8)
}

dependencies {
    implementation(projects.poket.poketFacade)

    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.jdk8)
    implementation(libs.jackson.jsr310)

    /*
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)

    implementation(platform(libs.micrometer.bom))
    implementation(libs.micrometer.core)
    api(libs.kotlin.logging)
    api(libs.slf4j)

     */

    testImplementation(libs.trekkie.kotest)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
