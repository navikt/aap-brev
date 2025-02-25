import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val ktorVersion = "3.1.0"
val komponenterVersjon = "1.0.152"
val behandlingsflytVersjon = "0.0.162"
val tilgangVersjon = "1.0.7"
val junitVersjon = "5.11.4"

plugins {
    id("brev.conventions")
    id("io.ktor.plugin") version "3.1.1"
}

application {
    mainClass.set("no.nav.aap.brev.AppKt")
}

tasks {
    withType<ShadowJar> {
        mergeServiceFiles()
    }
}

tasks.register<JavaExec>("runTestApp") {
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("no.nav.aap.brev.TestAppKt")
}

dependencies {
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.4")
    implementation("ch.qos.logback:logback-classic:1.5.16")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    implementation("no.nav.aap.behandlingsflyt:kontrakt:$behandlingsflytVersjon")

    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:json:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor-api:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:server:$komponenterVersjon")

    implementation("no.nav.aap.tilgang:plugin:$tilgangVersjon")
    implementation("no.nav.aap.tilgang:plugin-kontrakt:$tilgangVersjon")

    implementation("no.nav:ktor-openapi-generator:1.0.81")

    implementation(project(":dbflyway"))
    implementation(project(":kontrakt"))

    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.flywaydb:flyway-database-postgresql:11.3.3")
    implementation("org.postgresql:postgresql:42.7.5")

    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersjon")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.testcontainers:postgresql:1.20.5")
    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation(project(":lib-test"))
}
