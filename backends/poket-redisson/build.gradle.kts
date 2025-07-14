dependencies {
    api(projects.poket.poketFacade)
    implementation(projects.poket.serialization.poketJackson)
    implementation(libs.redisson)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.micrometer.core)
    implementation(libs.kotlin.logging)

    testImplementation(libs.trekkie.kotest)
    testImplementation(libs.testcontainers)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
