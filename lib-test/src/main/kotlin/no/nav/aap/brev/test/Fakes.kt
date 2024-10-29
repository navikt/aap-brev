package no.nav.aap.brev.no.nav.aap.brev.test

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import no.nav.aap.brev.api.ErrorResponse
import no.nav.aap.brev.kontrakt.Blokk
import no.nav.aap.brev.kontrakt.BlokkInnhold.FormattertTekst
import no.nav.aap.brev.kontrakt.BlokkType
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Formattering
import no.nav.aap.brev.kontrakt.Innhold
import no.nav.aap.brev.kontrakt.Tekstbolk
import no.nav.aap.brev.test.AZURE_JWKS
import no.nav.aap.brev.test.AzureTokenGen
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tilgang.BehandlingTilgangRequest
import tilgang.JournalpostTilgangRequest
import tilgang.SakTilgangRequest
import tilgang.TilgangResponse

class Fakes(azurePort: Int = 0) : AutoCloseable {
    private val log: Logger = LoggerFactory.getLogger(Fakes::class.java)
    private val azure = embeddedServer(Netty, port = azurePort, module = { azureFake() }).start()
    private val behandlingsflyt = embeddedServer(Netty, port = 0, module = { behandlingsflytFake() }).apply { start() }
    private val tilgang = embeddedServer(Netty, port = 0, module = { tilgangFake() }).apply { start() }
    private val brevSanityProxy = embeddedServer(Netty, port = 0, module = { brevSanityProxyFake() }).apply { start() }

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
    }

    override fun close() {
        azure.stop(0L, 0L)
        behandlingsflyt.stop(0L, 0L)
        tilgang.stop(0L, 0L)
        brevSanityProxy.stop(0L, 0L)
    }

    private fun EmbeddedServer<*, *>.port(): Int =
        runBlocking { this@port.engine.resolvedConnectors() }
            .first { it.type == ConnectorType.HTTP }
            .port

    private fun Application.azureFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@azureFake.log.info("AZURE :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorResponse(cause.message))
            }
        }
        routing {
            post("/token") {
                val token = AzureTokenGen("brev", "brev").generate()
                call.respond(TestToken(access_token = token))
            }
            get("/jwks") {
                call.respond(AZURE_JWKS)
            }
        }
    }

    private fun Application.behandlingsflytFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@behandlingsflytFake.log.info(
                    "BEHANDLINGSFLYT :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorResponse(cause.message))
            }
        }
        routing {
            post("/api/brev/løs-bestilling") {
                call.respond(HttpStatusCode.Accepted, "{}")
            }
        }
    }

    private fun Application.tilgangFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@tilgangFake.log.info(
                    "TILGANG :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorResponse(cause.message))
            }
        }
        routing {
            post("/tilgang/sak") {
                call.receive<SakTilgangRequest>()
                call.respond(TilgangResponse(true))
            }
        }
        routing {
            post("/tilgang/behandling") {
                call.receive<BehandlingTilgangRequest>()
                call.respond(TilgangResponse(true))
            }
        }
        routing {
            post("/tilgang/journalpost") {
                call.receive<JournalpostTilgangRequest>()
                call.respond(TilgangResponse(true))
            }
        }
    }

    fun Application.brevSanityProxyFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@brevSanityProxyFake.log.info(
                    "BREV_SANITY_PROXY :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorResponse(cause.message))
            }
        }
        routing {
            get("/api/mal") {
                call.respond(
                    Brev(
                        overskrift = "Overskrift - Brev",
                        tekstbolker = listOf(
                            Tekstbolk(
                                overskrift = "Overskrift - Tekstbolk", innhold = listOf(
                                    Innhold(
                                        overskrift = "Overskrift - Innhold",
                                        blokker = listOf(
                                            Blokk(
                                                innhold = listOf(
                                                    FormattertTekst(
                                                        tekst = "Formattert",
                                                        formattering = listOf(
                                                            Formattering.UNDERSTREK,
                                                            Formattering.KURSIV,
                                                            Formattering.FET
                                                        )
                                                    )
                                                ),
                                                type = BlokkType.AVSNITT
                                            )
                                        ),
                                        kanRedigeres = true,
                                        erFullstendig = false
                                    )
                                )
                            )
                        )
                    )
                )
            }
        }
    }


    internal data class TestToken(
        val access_token: String,
        val refresh_token: String = "very.secure.token",
        val id_token: String = "very.secure.token",
        val token_type: String = "token-type",
        val scope: String? = null,
        val expires_in: Int = 3599,
    )
}