plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.dokka)
}

kotlin {
    jvmToolchain(1_8)
}

dependencies {
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(platform(libs.micrometer.bom))

    api(libs.slf4j)
    implementation(libs.micrometer.core)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
