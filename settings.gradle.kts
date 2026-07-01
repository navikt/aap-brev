rootProject.name = "brev"

include(
    "app",
    "dbflyway",
    "kontrakt",
    "lib-test",
)

dependencyResolutionManagement {
    // Felles for alle gradle prosjekter i repoet
    @Suppress("UnstableApiUsage")
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
        mavenCentral()
        mavenLocal()
    }
}
