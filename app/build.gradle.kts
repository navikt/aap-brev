import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val ktorVersion = "3.3.3"
val komponenterVersjon = "1.0.936"
val tilgangVersjon = "1.0.178"
val junitVersjon = "5.13.1"

plugins {
    id("aap.conventions")
    id("io.ktor.plugin") version "3.3.3"
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
    implementation("io.micrometer:micrometer-registry-prometheus:1.16.2")
    implementation("ch.qos.logback:logback-classic:1.5.25")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")

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

    implementation("no.nav.aap.kelvin:ktor-openapi-generator:$komponenterVersjon")

    implementation(project(":dbflyway"))
    implementation(project(":kontrakt"))

    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.flywaydb:flyway-database-postgresql:11.20.2")
    implementation("org.postgresql:postgresql:42.7.9")

    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersjon")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.3")

    testImplementation(project(":lib-test"))
    testImplementation("io.mockk:mockk:1.14.7")
}
