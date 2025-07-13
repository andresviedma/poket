plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(1_8)
}

dependencies {
    implementation(projects.poket.poketFacade)

    implementation(libs.kotlin.reflect)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.micrometer.core)

    testImplementation(libs.trekkie.kotest)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.junit5)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
