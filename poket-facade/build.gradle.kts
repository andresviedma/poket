dependencies {
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.datetime)

    implementation(platform(libs.micrometer.bom))
    implementation(libs.micrometer.core)
    api(libs.kotlin.logging)
    api(libs.slf4j)

    testImplementation(libs.trekkie.kotest)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.junit5)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
