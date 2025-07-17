dependencies {
    implementation(projects.poket.poketFacade)

    implementation(libs.koin.core)
    implementation(libs.kotlin.reflect)

    testImplementation(projects.poket.inject.injectTestcommon)
}
