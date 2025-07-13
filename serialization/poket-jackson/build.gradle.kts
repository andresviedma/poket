dependencies {
    implementation(projects.poket.poketFacade)

    api(platform(libs.jackson.bom))
    api(libs.jackson.databind)
    api(libs.jackson.kotlin)
    api(libs.jackson.jdk8)
    api(libs.jackson.jsr310)
}
