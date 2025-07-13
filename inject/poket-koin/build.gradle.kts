dependencies {
    implementation(projects.poket.poketFacade)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    implementation(libs.kotlin.reflect)

    testImplementation(projects.poket.inject.injectTestcommon)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
