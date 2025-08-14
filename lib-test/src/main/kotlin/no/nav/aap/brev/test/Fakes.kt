package no.nav.aap.brev.no.nav.aap.brev.test

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import no.nav.aap.brev.test.fakes.azureFake
import no.nav.aap.brev.test.fakes.brevSanityProxyFake
import no.nav.aap.brev.test.fakes.dokarkivFake
import no.nav.aap.brev.test.fakes.dokdistfordelingFake
import no.nav.aap.brev.test.fakes.nomFake
import no.nav.aap.brev.test.fakes.norgFake
import no.nav.aap.brev.test.fakes.pdfGenFake
import no.nav.aap.brev.test.fakes.pdlFake
import no.nav.aap.brev.test.fakes.safFake
import no.nav.aap.brev.test.fakes.tilgangFake
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

object Fakes : AutoCloseable {

    private val log: Logger = LoggerFactory.getLogger(Fakes::class.java)

    private val started = AtomicBoolean(false)
    private val servers = mutableListOf<EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>>()

    fun start(azurePort: Int = 0) {
        if (started.get()) {
            return
        }
        val azure = embeddedServer(Netty, port = azurePort, module = { azureFake() }).start()
        val tilgang = embeddedServer(Netty, port = 0, module = { tilgangFake() }).apply { start() }
        val brevSanityProxy = embeddedServer(Netty, port = 0, module = { brevSanityProxyFake() }).apply { start() }
        val pdfGen = embeddedServer(Netty, port = 0, module = { pdfGenFake() }).apply { start() }
        val dokarkiv = embeddedServer(Netty, port = 0, module = { dokarkivFake() }).apply { start() }
        val dokdistfordeling = embeddedServer(Netty, port = 0, module = { dokdistfordelingFake() }).apply { start() }
        val nom = embeddedServer(Netty, port = 0, module = { nomFake() }).apply { start() }
        val pdl = embeddedServer(Netty, port = 0, module = { pdlFake() }).apply { start() }
        val saf = embeddedServer(Netty, port = 0, module = { safFake() }).apply { start() }
        val norg = embeddedServer(Netty, port = 0, module = { norgFake() }).apply { start() }
        servers.addAll(
            listOf(
                azure,
                tilgang,
                brevSanityProxy,
                pdfGen,
                dokarkiv,
                dokdistfordeling,
                saf,
            )
        )
        Thread.currentThread().setUncaughtExceptionHandler { _, e -> log.error("Uhåndtert feil", e) }
        // Azure
        System.setProperty("azure.openid.config.token.endpoint", "http://localhost:${azure.port()}/token")
        System.setProperty("azure.app.client.id", "brev")
        System.setProperty("azure.app.client.secret", "")
        System.setProperty("azure.openid.config.jwks.uri", "http://localhost:${azure.port()}/jwks")
        System.setProperty("azure.openid.config.issuer", "brev")

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

        // Dokdistfordeling
        System.setProperty("integrasjon.dokdistfordeling.url", "http://localhost:${dokdistfordeling.port()}")
        System.setProperty("integrasjon.dokdistfordeling.scope", "scope")

        // Norg
        System.setProperty("integrasjon.norg.url", "http://localhost:${norg.port()}")

        // NOM
        System.setProperty("integrasjon.nom.url", "http://localhost:${nom.port()}/graphql")
        System.setProperty("integrasjon.nom.scope", "scope")

        // PDL
        System.setProperty("integrasjon.pdl.url", "http://localhost:${pdl.port()}/graphql")
        System.setProperty("integrasjon.pdl.scope", "scope")

        // Saf
        System.setProperty("integrasjon.saf.url.graphql", "http://localhost:${saf.port()}/graphql")
        System.setProperty("integrasjon.saf.scope", "scope")
    }


    override fun close() {
        servers.forEach { it.stop(0L, 0L) }
    }

    private fun EmbeddedServer<*, *>.port(): Int =
        runBlocking { this@port.engine.resolvedConnectors() }
            .first { it.type == ConnectorType.HTTP }
            .port
}
