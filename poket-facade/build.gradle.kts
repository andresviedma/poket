dependencies {
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.datetime)

    implementation(platform(libs.micrometer.bom))
    api(libs.micrometer.core)
    implementation(libs.kotlin.logging)

    testImplementation(libs.trekkie.kotest)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.junit5)
    testImplementation(libs.slf4j)

    testImplementation(libs.jackson.kotlin)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
