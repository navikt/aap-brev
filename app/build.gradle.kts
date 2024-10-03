val ktorVersion = "2.3.12"
val komponenterVersjon = "0.0.84"
val tilgangVersjon = "0.0.11"

plugins {
    id("io.ktor.plugin")
}

application {
    mainClass.set("no.nav.aap.brev.AppKt")
}

dependencies {
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
    implementation("io.micrometer:micrometer-registry-prometheus:1.13.4")
    implementation("ch.qos.logback:logback-classic:1.5.8")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor-api:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:server:$komponenterVersjon")

    implementation("no.nav.aap.tilgang:plugin:$tilgangVersjon")

    implementation(project(":dbflyway"))

    implementation("com.zaxxer:HikariCP:6.0.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.18.0")
    implementation("org.postgresql:postgresql:42.7.4")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.testcontainers:postgresql:1.20.1")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation(kotlin("test"))
    testImplementation(project(":lib-test"))
}
