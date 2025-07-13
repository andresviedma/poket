plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(1_8)
}

dependencies {
    api(projects.poket.poketFacade)
    implementation(libs.caffeine)

    testImplementation(libs.trekkie.kotest)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
