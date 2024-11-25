package no.nav.aap.brev.no.nav.aap.brev.test

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import no.nav.aap.brev.test.fakes.azureFake
import no.nav.aap.brev.test.fakes.behandlingsflytFake
import no.nav.aap.brev.test.fakes.brevSanityProxyFake
import no.nav.aap.brev.test.fakes.dokarkivFake
import no.nav.aap.brev.test.fakes.pdfGenFake
import no.nav.aap.brev.test.fakes.tilgangFake
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Fakes(azurePort: Int = 0) : AutoCloseable {
    private val log: Logger = LoggerFactory.getLogger(Fakes::class.java)

    private val azure = embeddedServer(Netty, port = azurePort, module = { azureFake() }).start()
    private val behandlingsflyt = embeddedServer(Netty, port = 0, module = { behandlingsflytFake() }).apply { start() }
    private val tilgang = embeddedServer(Netty, port = 0, module = { tilgangFake() }).apply { start() }
    private val brevSanityProxy = embeddedServer(Netty, port = 0, module = { brevSanityProxyFake() }).apply { start() }
    private val pdfGen = embeddedServer(Netty, port = 0, module = { pdfGenFake() }).apply { start() }
    private val dokarkiv = embeddedServer(Netty, port = 0, module = { dokarkivFake() }).apply { start() }

    init {
        Thread.currentThread().setUncaughtExceptionHandler { _, e -> log.error("Uhåndtert feil", e) }
        // Azure
        System.setProperty("azure.openid.config.token.endpoint", "http://localhost:${azure.port()}/token")
        System.setProperty("azure.app.client.id", "brev")
        System.setProperty("azure.app.client.secret", "")
        System.setProperty("azure.openid.config.jwks.uri", "http://localhost:${azure.port()}/jwks")
        System.setProperty("azure.openid.config.issuer", "brev")

        // Behandlingsflyt
        System.setProperty("integrasjon.behandlingsflyt.url", "http://localhost:${behandlingsflyt.port()}")
        System.setProperty("integrasjon.behandlingsflyt.scope", "scope")
        System.setProperty("integrasjon.behandlingsflyt.azp", "azp")

        // Tilgang
        System.setProperty("integrasjon.tilgang.url", "http://localhost:${tilgang.port()}")
        System.setProperty("integrasjon.tilgang.scope", "scope")
        System.setProperty("integrasjon.tilgang.azp", "azp")

        // Brev sanity proxy
        System.setProperty("integrasjon.brev_sanity_proxy.url", "http://localhost:${brevSanityProxy.port()}")
        System.setProperty("integrasjon.brev_sanity_proxy.scope", "scope")
        System.setProperty("integrasjon.brev_sanity_proxy.azp", "azp")

        // PdfGen
        System.setProperty("integrasjon.saksbehandling_pdfgen.url", "http://localhost:${pdfGen.port()}")
        System.setProperty("integrasjon.saksbehandling_pdfgen.scope", "scope")

        // Dokarkiv
        System.setProperty("integrasjon.dokarkiv.url", "http://localhost:${dokarkiv.port()}")
        System.setProperty("integrasjon.dokarkiv.scope", "scope")
    }

    override fun close() {
        azure.stop(0L, 0L)
        behandlingsflyt.stop(0L, 0L)
        tilgang.stop(0L, 0L)
        brevSanityProxy.stop(0L, 0L)
        pdfGen.stop(0L, 0L)
    }

    private fun EmbeddedServer<*, *>.port(): Int =
        runBlocking { this@port.engine.resolvedConnectors() }
            .first { it.type == ConnectorType.HTTP }
            .port
}
