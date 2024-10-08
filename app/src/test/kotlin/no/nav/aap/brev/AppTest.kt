package no.nav.aap.brev

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.brev.kontrakt.BestillBrevRequest
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.net.URI
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.UUID

class AppTest {

    companion object {
        private val postgres = postgreSQLContainer()
        private val fakes = Fakes()
        private val dbConfig = DbConfig(
            jdbcUrl = "${postgres.jdbcUrl}&user=${postgres.username}&password=${postgres.password}",
        )
        private val restClient = RestClient(
            config = ClientConfig(scope = "brev"),
            tokenProvider = ClientCredentialsTokenProvider,
            responseHandler = DefaultResponseHandler()
        )

        // Starter server
        private val server = embeddedServer(Netty, port = 8080) {
            server(
                dbConfig = dbConfig,
            )
            module(fakes)
        }.start()

        @JvmStatic
        @AfterAll
        fun afterAll() {
            server.stop()
            fakes.close()
            postgres.close()
        }
    }

    @Test
    fun `bestiller brev`() {
        assertDoesNotThrow {
            restClient.post<_, Unit>(
                uri = URI.create("http://localhost:8080/").resolve("/api/bestill"),
                request = PostRequest(
                    body = BestillBrevRequest(UUID.randomUUID(), Brevtype.INNVILGELSE, Språk.nb)
                )
            )
        }
    }
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