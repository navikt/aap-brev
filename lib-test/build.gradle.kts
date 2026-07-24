plugins {
    id("aap.conventions")
}

dependencies {
    implementation(project(":app"))
    implementation(project(":kontrakt"))

    implementation(libs.server)
    implementation(libs.httpklient)
    implementation(libs.json)
    implementation(libs.tilgangKontrakt)

    implementation(libs.jacksonDatabind)
    implementation(libs.jacksonDatatypeJsr310)

    implementation(libs.joseJwt)
}
