import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("aap.conventions")
    `maven-publish`
    `java-library`
}

group = "no.nav.aap.brev"

java {
    withSourcesJar()
}

kotlin {
    explicitApi = ExplicitApiMode.Warning
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
    api(kelvinLibs.jackson.datatype.jsr310)

    testImplementation(project(":lib-test"))
    testImplementation(libs.json)
    testImplementation(kelvinLibs.bundles.junit)
}
