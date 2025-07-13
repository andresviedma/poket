plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(1_8)
}

dependencies {
    implementation(projects.poket.poketFacade)

    implementation(libs.guice)
    implementation(libs.kotlin.reflect)

    testImplementation(projects.inject.injectTestcommon)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
