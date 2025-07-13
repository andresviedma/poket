dependencies {
    api(projects.poket.poketFacade)
    api(libs.kotlin.reflect)
    api(libs.trekkie.kotest)
    implementation(libs.micrometer.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
