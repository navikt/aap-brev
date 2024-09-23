package no.nav.aap.brev

import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.brev.api.BestillBrevRequest
import no.nav.aap.brev.api.BestillBrevResponse
import no.nav.aap.brev.api.ErrorRespons
import no.nav.aap.brev.domene.BrevbestillingReferanse
import no.nav.aap.brev.innhold.BrevinnholdService
import no.nav.aap.brev.innhold.SanityBrevinnholdGateway
import no.nav.aap.komponenter.commonKtorModule
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.tilgang.authorizedPostWithApprovedList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

private val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLog")
const val AZURE = "azure"

class App

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> SECURE_LOGGER.error("Uhåndtert feil", e) }

    val brevinnholdGateway = SanityBrevinnholdGateway()
    val brevinnholdService = BrevinnholdService(brevinnholdGateway)

    embeddedServer(Netty, port = 8080) {
        server(
            DbConfig(),
            brevinnholdService,
        )
    }.start(wait = true)
}

internal fun Application.server(
    dbConfig: DbConfig,
    brevinnholdService: BrevinnholdService,
) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    commonKtorModule(prometheus, AzureConfig(), "AAP - Brev")

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            LoggerFactory.getLogger(App::class.java)
                .warn("Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
            call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
        }
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    val dataSource = initDatasource(dbConfig)
    Migrering.migrate(dataSource)

    val behandlingsflytAzp = requiredConfigForKey("integrasjon.behandlingsflyt.azp")

    routing {
        authenticate(AZURE) {
            apiRouting {
                route("/api") {
                    route("/bestill") {
                        authorizedPostWithApprovedList<Unit, BestillBrevResponse, BestillBrevRequest>(behandlingsflytAzp) { _, request ->
                            brevinnholdService.behandleBrevbestilling(
                                request.behandlingReferanse,
                                request.brevtype,
                                request.language,
                            )
                            respond(
                                response = BestillBrevResponse(BrevbestillingReferanse(UUID.randomUUID())),
                                statusCode = HttpStatusCode.Created
                            )
                        }
                    }
                }
            }
        }
        actuator(prometheus)
    }
}

private fun Routing.actuator(prometheus: PrometheusMeterRegistry) {
    route("/actuator") {
        get("/metrics") {
            call.respond(prometheus.scrape())
        }

        get("/live") {
            val status = HttpStatusCode.OK
            call.respond(status, "Oppe!")
        }

        get("/ready") {
            call.respond(HttpStatusCode.OK, "Oppe!")
        }
    }
}

class DbConfig(
    val jdbcUrl: String = System.getenv("NAIS_DATABASE_BREV_BREV_JDBC_URL"),
)

fun initDatasource(dbConfig: DbConfig) = HikariDataSource(HikariConfig().apply {
    jdbcUrl = dbConfig.jdbcUrl
    maximumPoolSize = 10
    minimumIdle = 1
    driverClassName = "org.postgresql.Driver"
    connectionTestQuery = "SELECT 1"
})
