import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val ktorVersion = "3.3.1"
val komponenterVersjon = "1.0.430"
val tilgangVersjon = "1.0.151"
val junitVersjon = "5.13.1"

plugins {
    id("brev.conventions")
    id("io.ktor.plugin") version "3.3.1"
}

application {
    mainClass.set("no.nav.aap.brev.AppKt")
}

tasks {
    withType<ShadowJar> {
        duplicatesStrategy = DuplicatesStrategy.WARN
        mergeServiceFiles()
    }
}

tasks.register<JavaExec>("runTestApp") {
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("no.nav.aap.brev.TestAppKt")
}

tasks.register<JavaExec>("genererOpenApi") {
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("no.nav.aap.brev.GenererOpenApiJsonKt")
    workingDir = rootDir
}

dependencies {
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.1")
    implementation("io.micrometer:micrometer-registry-prometheus:1.15.5")
    implementation("ch.qos.logback:logback-classic:1.5.20")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")

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

    implementation("no.nav:ktor-openapi-generator:1.0.131")

    implementation(project(":dbflyway"))
    implementation(project(":kontrakt"))

    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.flywaydb:flyway-database-postgresql:11.15.0")
    implementation("org.postgresql:postgresql:42.7.8")

    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersjon")
    testImplementation("org.assertj:assertj-core:3.27.6")
    testImplementation("org.testcontainers:postgresql:1.21.3")
    constraints {
        implementation("org.apache.commons:commons-compress:1.28.0") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation(project(":lib-test"))
    testImplementation("io.mockk:mockk:1.14.6")
}
