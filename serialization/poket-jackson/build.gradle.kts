dependencies {
    implementation(projects.poket.poketFacade)

    api(platform(libs.jackson.bom))
    api(libs.jackson.databind)
    api(libs.jackson.kotlin)
    api(libs.jackson.jdk8)
    api(libs.jackson.jsr310)

    testImplementation(libs.trekkie.kotest)
    testImplementation(libs.slf4j)
    testImplementation(libs.slf4j.simple)
}
