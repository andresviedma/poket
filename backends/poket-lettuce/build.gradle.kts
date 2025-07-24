dependencies {
    api(projects.poket.poketFacade)
    implementation(libs.lettuce)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.micrometer.core)
    implementation(libs.kotlin.logging)

    testImplementation(libs.trekkie.kotest)
    testImplementation(libs.testcontainers)
    testImplementation(projects.poket.serialization.poketJackson)
}
