import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("aap.conventions")
    alias(libs.plugins.ktor)
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
    implementation(libs.ktorServerCors)
    implementation(libs.ktorServerStatusPages)

    implementation(libs.jacksonDatatypeJsr310)
    implementation(libs.micrometerRegistryPrometheus)
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)

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

    implementation(libs.hikariCp)
    implementation(libs.flywayDatabasePostgresql)
    implementation(libs.postgresql)

    testImplementation(libs.dbtest)
    testRuntimeOnly(libs.junitJupiterEngine)
    testImplementation(libs.junitApi)
    testImplementation(libs.junitJupiterParams)
    testImplementation(libs.assertJ)
    testImplementation(libs.testcontainersPostgres)

    testImplementation(project(":lib-test"))
    testImplementation(libs.mockk)
}
