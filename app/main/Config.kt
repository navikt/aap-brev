private fun getEnvVar(envar: String) = System.getenv(envar) ?: error("missing envvar $envar")

data class Config(
  val sanity: SanityConfig = SanityConfig(),
)

data class SanityConfig(
  val token: String = getEnvVar("SANITY_API_TOKEN"),
  val host: String = "https://948n95rd.api.sanity.io",
)
