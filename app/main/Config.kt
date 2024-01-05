private fun getEnvVar(envar: String) =
    System.getenv(envar) ?: error("missing envvar $envar")

data class Config(
    val sanity: SanityConfig = SanityConfig(),
    val postgres: PostgresConfig = PostgresConfig(),
)

data class SanityConfig(
    val token: String = getEnvVar("SANITY_API_TOKEN"),
    val host: String = "https://948n95rd.api.sanity.io",
)

data class PostgresConfig(
    val host: String = getEnvVar("NAIS_DATABASE_BREV_BREV_HOST"),
    val port: String = getEnvVar("NAIS_DATABASE_BREV_BREV_PORT"),
    val database: String = getEnvVar("NAIS_DATABASE_BREV_BREV_DATABASE"),
    val username: String = getEnvVar("NAIS_DATABASE_BREV_BREV_USERNAME"),
    val password: String = getEnvVar("NAIS_DATABASE_BREV_BREV_PASSWORD"),
    val url: String = "jdbc:postgresql://${host}:${port}/${database}",
    val driver: String = "org.postgresql.Driver",
    val cluster: String = getEnvVar("NAIS_CLUSTER_NAME")
)
