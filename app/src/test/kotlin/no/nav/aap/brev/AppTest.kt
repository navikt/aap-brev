package no.nav.aap.brev

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.net.URI
import org.assertj.core.api.Assertions.assertThat
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class AppTest {

    companion object {
        private val fakes = Fakes()

        val httpClient = HttpClient.newBuilder().build()
        private val restClient = RestClient(
            config = ClientConfig(scope = "brev"),
            tokenProvider = ClientCredentialsTokenProvider,
            responseHandler = DefaultResponseHandler()
        )

        // Starter server
        private val server = embeddedServer(Netty, port = 0) {
            server(/*dbConfig = dbConfig*/)
            module(fakes)
        }.start()

        @JvmStatic
        @AfterAll
        fun afterAll() {
            server.stop()
            fakes.close()
        }
    }

    @Test
    fun `appen er ready`() {
        val res = httpClient.send(
            HttpRequest.newBuilder(
                URI.create("http://localhost:8080/")
                    .resolve("/actuator/ready"),
            ).GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        assertThat(res.statusCode()).isEqualTo(HttpStatusCode.OK.value)
    }
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