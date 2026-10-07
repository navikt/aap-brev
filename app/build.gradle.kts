import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("aap.conventions")
    alias(kelvinLibs.plugins.ktor)
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
    description = "Kjør TestApp"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("no.nav.aap.brev.TestAppKt")
}

tasks.register<JavaExec>("genererOpenApi") {
    description = "Generer openapi.json"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("no.nav.aap.brev.GenererOpenApiJsonKt")
    workingDir = rootDir
}

dependencies {
    implementation(kelvinLibs.jackson.datatype.jsr310)
    implementation(kelvinLibs.micrometer.prometheus)
    implementation(kelvinLibs.logback.classic)
    implementation(kelvinLibs.logstash.logback.encoder)

    implementation(libs.dbconnect)
    implementation(libs.dbmigrering)
    implementation(libs.httpklient)
    implementation(libs.json)
    implementation(libs.infrastructure)
    implementation(libs.motor)
    implementation(libs.motorApi)
    implementation(libs.server)

    implementation(libs.tilgangPlugin)
    implementation(libs.tilgangPluginKontrakt)

    implementation(libs.ktorOpenApiGenerator)

    implementation(project(":dbflyway"))
    implementation(project(":kontrakt"))

    implementation(kelvinLibs.hikaricp)

    api(libs.gateway)
    implementation(kelvinLibs.unleash.client.java)

    testImplementation(libs.dbtest)
    testImplementation(kelvinLibs.bundles.junit)
    testImplementation(kelvinLibs.assertj.core)
    testImplementation(kelvinLibs.testcontainers.postgresql)

    testImplementation(project(":lib-test"))
    testImplementation(kelvinLibs.mockk)
}
