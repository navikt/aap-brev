import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val komponenterVersjon = "1.0.356"
val jupiterVersjon = "5.13.1"

plugins {
    id("brev.conventions")
    `maven-publish`
    `java-library`
}

group = "no.nav.aap.brev"

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
    }
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.name
            version = project.findProperty("version")?.toString() ?: "0.0.0"
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/navikt/aap-brev")
            credentials {
                username = "x-access-token"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

dependencies {
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.0")

    testImplementation("no.nav.aap.kelvin:json:$komponenterVersjon")
    testImplementation("org.assertj:assertj-core:3.27.4")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersjon")
}
