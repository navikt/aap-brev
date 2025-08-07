package no.nav.aap.brev

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.brev.kontrakt.BestillBrevV2Request
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.test.randomBrukerIdent
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
import java.util.*

class AppTest {

    companion object {
        private val postgres = postgreSQLContainer()

        init {
            Fakes.start()
        }

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
        }.start()

        @JvmStatic
        @AfterAll
        fun afterAll() {
            server.stop()
            postgres.close()
        }
    }

    @Test
    fun `bestiller brev`() {
        assertDoesNotThrow {
            restClient.post<_, Unit>(
                uri = URI.create("http://localhost:8080/").resolve("/api/v2/bestill"),
                request = PostRequest(
                    body = BestillBrevV2Request(
                        saksnummer = "SAK123",
                        brukerIdent = randomBrukerIdent(),
                        behandlingReferanse = UUID.randomUUID(),
                        brevtype = Brevtype.INNVILGELSE,
                        unikReferanse = UUID.randomUUID().toString(),
                        sprak = Språk.NB,
                        faktagrunnlag = emptySet(),
                        ferdigstillAutomatisk = false,
                        vedlegg = emptySet()
                    )
                )
            )
        }
    }
}

internal fun postgreSQLContainer(): PostgreSQLContainer<Nothing> {
    val postgres = PostgreSQLContainer<Nothing>("postgres:16")
    postgres.waitingFor(HostPortWaitStrategy().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS)))
    postgres.start()
    return postgres
}