dependencies {
    api(projects.poket.poketFacade)
    implementation(libs.caffeine)

    testImplementation(libs.trekkie.kotest)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
