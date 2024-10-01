package no.nav.aap.brev

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.time.Duration
import java.time.temporal.ChronoUnit

fun main() {
    val postgres = postgreSQLContainer()
    val fakes = Fakes(azurePort = 8081)

    val dbConfig = DbConfig(
        jdbcUrl = "${postgres.jdbcUrl}&user=${postgres.username}&password=${postgres.password}",
    )

    println("Bruk følgende konfigurasjon for å koble til databasen:")
    println("jdbcUrl: ${postgres.jdbcUrl}. Password: ${postgres.password}. Username: ${postgres.username}.")

    // Starter server
    embeddedServer(Netty, port = 8080) {
        server(
            dbConfig = dbConfig,
        )
        module(fakes)
    }.start(wait = true)
}

private fun postgreSQLContainer(): PostgreSQLContainer<Nothing> {
    val postgres = PostgreSQLContainer<Nothing>("postgres:16")
    postgres.waitingFor(HostPortWaitStrategy().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS)))
    postgres.start()
    return postgres
}

private fun Application.module(fakes: Fakes) {
    // Setter opp virtuell sandkasse lokalt
    environment.monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        fakes.close()
        // Release resources and unsubscribe from events
        application.environment.monitor.unsubscribe(ApplicationStopped) {}
    }
}