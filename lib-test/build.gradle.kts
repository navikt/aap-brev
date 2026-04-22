plugins {
    id("aap.conventions")
}

dependencies {
    implementation(project(":app"))
    implementation(project(":kontrakt"))
    implementation(libs.ktorServerAuth)
    implementation(libs.ktorServerAuthJwt)
    implementation(libs.ktorServerCallLogging)
    implementation(libs.ktorServerContentNegotation)
    implementation(libs.ktorServerMetricsMicrometer)
    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerCors)
    implementation(libs.ktorServerStatusPages)
    implementation(libs.ktorSerializationJackson)

    implementation(libs.httpklient)
    implementation(libs.json)
    implementation(libs.tilgangKontrakt)

    implementation(libs.jacksonDatabind)
    implementation(libs.jacksonDatatypeJsr310)

    implementation(libs.joseJwt)
}