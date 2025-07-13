dependencies {
    implementation(projects.poket.poketFacade)

    implementation(platform(libs.guice.bom))
    implementation(libs.guice)
    implementation(libs.kotlin.reflect)

    testImplementation(projects.inject.injectTestcommon)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
