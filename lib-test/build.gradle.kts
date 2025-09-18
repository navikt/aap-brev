val ktorVersion = "3.2.3"
val komponenterVersjon = "1.0.362"
val tilgangVersjon = "1.0.125"
val jacksonVersjon = "2.20.0"

plugins {
    id("brev.conventions")
}

dependencies {
    implementation(project(":app"))
    implementation(project(":kontrakt"))
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:json:$komponenterVersjon")
    implementation("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")

    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersjon")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersjon")

    implementation("com.nimbusds:nimbus-jose-jwt:10.4.2")
}