package no.nav.aap.brev

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.brev.api.BestillBrevRequest
import no.nav.aap.brev.api.BestillBrevResponse
import no.nav.aap.brev.domene.BehandlingReferanse
import no.nav.aap.brev.domene.Brevtype
import no.nav.aap.brev.domene.Språk
import no.nav.aap.brev.innhold.BrevinnholdService
import no.nav.aap.brev.innhold.SanityBrevinnholdGateway
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.net.URI
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.test.assertNotNull

class AppTest {

    companion object {
        private val postgres = postgreSQLContainer()
        private val fakes = Fakes(azurePort = 8081)
        private val dbConfig = DbConfig(
            jdbcUrl = "${postgres.jdbcUrl}&user=${postgres.username}&password=${postgres.password}",
        )

        val brevinnholdGateway = SanityBrevinnholdGateway()
        val brevinnholdService = BrevinnholdService(brevinnholdGateway)

        private val restClient = RestClient(
            config = ClientConfig(scope = "brev"),
            tokenProvider = ClientCredentialsTokenProvider,
            responseHandler = DefaultResponseHandler()
        )

        // Starter server
        private val server = embeddedServer(Netty, port = 8080) {
            server(
                dbConfig = dbConfig,
                brevinnholdService = brevinnholdService,
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
        val res: BestillBrevResponse? = restClient.post(
            uri = URI.create("http://localhost:8080/").resolve("/api/bestill"),
            request = PostRequest(
                body = BestillBrevRequest(BehandlingReferanse("123"), Brevtype.INNVILGELSE, Språk.NB)
            ),
            mapper = { body, _ ->
                DefaultJsonMapper.fromJson(body)
            }
        )
        assertNotNull(res)
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