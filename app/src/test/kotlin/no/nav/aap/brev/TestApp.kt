package no.nav.aap.brev

import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.komponenter.config.configForKey
import java.util.concurrent.TimeUnit

fun main() {
    var jdbcUrl = configForKey("JDBC_URL")

    if (jdbcUrl == null) {
        val postgres = postgreSQLContainer()
        println("Bruk følgende konfigurasjon for å koble til databasen:")
        println("jdbcUrl: ${postgres.jdbcUrl}. Password: ${postgres.password}. Username: ${postgres.username}.")
        jdbcUrl = "${postgres.jdbcUrl}&user=${postgres.username}&password=${postgres.password}"
    }

    Fakes.start(azurePort = 8083)

    val dbConfig = DbConfig(
        jdbcUrl = jdbcUrl,
    )

    // Starter server
    embeddedServer(Netty, configure = {
        shutdownGracePeriod = TimeUnit.SECONDS.toMillis(5)
        shutdownTimeout = TimeUnit.SECONDS.toMillis(10)
        connector {
            port = 8082
        }
    }) {
        server(
            dbConfig = dbConfig,
        )
    }.start(wait = true)
}
