val ktorVersion = "3.0.3"
val komponenterVersjon = "1.0.91"
val tilgangVersjon = "0.0.56"
val behandlingsflytVersjon = "0.0.49"

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
    implementation("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")
    implementation("no.nav.aap.behandlingsflyt:kontrakt:$behandlingsflytVersjon")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")

    implementation("com.nimbusds:nimbus-jose-jwt:9.47")
}