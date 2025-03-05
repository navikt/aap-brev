package no.nav.aap.brev

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.komponenter.config.configForKey
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.time.Duration
import java.time.temporal.ChronoUnit

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
    embeddedServer(Netty, port = 8082) {
        server(
            dbConfig = dbConfig,
        )
        module()
    }.start(wait = true)
}

private fun postgreSQLContainer(): PostgreSQLContainer<Nothing> {
    val postgres = PostgreSQLContainer<Nothing>("postgres:16")
    postgres.waitingFor(HostPortWaitStrategy().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS)))
    postgres.start()
    return postgres
}

private fun Application.module() {
    // Setter opp virtuell sandkasse lokalt
    monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        // Release resources and unsubscribe from events
        application.monitor.unsubscribe(ApplicationStopped) {}
    }
}