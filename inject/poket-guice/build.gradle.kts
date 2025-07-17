dependencies {
    implementation(projects.poket.poketFacade)

    implementation(libs.guice)
    implementation(libs.kotlin.reflect)

    testImplementation(projects.inject.injectTestcommon)
}
