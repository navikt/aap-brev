package no.nav.aap.brev

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.io.BufferedWriter
import java.io.FileWriter
import java.net.URI
import java.nio.charset.StandardCharsets

fun main() {
    Fakes.start(azurePort = 8083)
    val postgres = postgreSQLContainer()

    val dbConfig = DbConfig(
        jdbcUrl = "${postgres.jdbcUrl}&user=${postgres.username}&password=${postgres.password}"
    )

    // Starter server
    val server = embeddedServer(Netty, port = 8082) {
        server(
            dbConfig = dbConfig,
        )
        module()
    }.start()

    val restClient = RestClient(
        config = ClientConfig(scope = "brev"),
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = DefaultResponseHandler()
    )

    val openApiDoc =
        checkNotNull(
            restClient.get<String>(
                URI.create("http://localhost:8082/openapi.json"),
                GetRequest()
            ) { body, _ ->
                String(body.readAllBytes(), StandardCharsets.UTF_8)
            }
        )

    val writer = BufferedWriter(FileWriter("openapi.json"))
    writer.use {
        it.write(openApiDoc)
    }

    server.stop()
    Fakes.close()
}

private fun Application.module() {
    // Setter opp virtuell sandkasse lokalt
    monitor.subscribe(ApplicationStopPreparing) { application ->
        // Release resources and unsubscribe from events
        monitor.unsubscribe(ApplicationStopped) {}
    }
}