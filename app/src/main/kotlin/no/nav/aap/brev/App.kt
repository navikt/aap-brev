package no.nav.aap.brev

import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.put
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
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
import no.nav.aap.brev.api.BrevbestillingReferansePathParam
import no.nav.aap.brev.api.ErrorRespons
import no.nav.aap.brev.domene.Brev
import no.nav.aap.brev.domene.Brevbestilling
import no.nav.aap.brev.exception.BestillingForBehandlingEksistererException
import no.nav.aap.brev.prosessering.ProsesserBrevbestillingJobbUtfører
import no.nav.aap.komponenter.commonKtorModule
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.motor.Motor
import no.nav.aap.motor.api.motorApi
import no.nav.aap.motor.mdc.NoExtraLogInfoProvider
import no.nav.aap.motor.retry.RetryService
import no.nav.aap.tilgang.authorizedGetWithApprovedList
import no.nav.aap.tilgang.authorizedPostWithApprovedList
import no.nav.aap.tilgang.installerTilgangPluginWithApprovedList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLog")
private val LOGGER = LoggerFactory.getLogger(App::class.java)
const val AZURE = "azure"

class App

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e ->
        LOGGER.error("Uhåndtert feil. Se sikker logg for detaljer.")
        SECURE_LOGGER.error("Uhåndtert feil", e) }

    embeddedServer(Netty, port = 8080) {
        server(
            DbConfig(),
        )
    }.start(wait = true)
}

internal fun Application.server(
    dbConfig: DbConfig,
) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    commonKtorModule(prometheus, AzureConfig(), "AAP - Brev")

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is BestillingForBehandlingEksistererException -> {
                    call.respond(HttpStatusCode.BadRequest, ErrorRespons(cause.message))
                }

                else -> {
                    LoggerFactory.getLogger(App::class.java)
                        .warn("Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                    call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
                }
            }

        }
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    val dataSource = initDatasource(dbConfig)
    Migrering.migrate(dataSource)

    val motor = module(dataSource)

    val behandlingsflytAzp = requiredConfigForKey("integrasjon.behandlingsflyt.azp")

    routing {
        authenticate(AZURE) {
            apiRouting {
                route("/api") {
                    route("/bestill") {
                        authorizedPostWithApprovedList<Unit, BestillBrevResponse, BestillBrevRequest>(
                            behandlingsflytAzp
                        ) { _, request ->
                            val referanse = dataSource.transaction { connection ->
                                BrevbestillingService.konstruer(connection).behandleBrevbestilling(
                                    behandlingReferanse = request.behandlingReferanse,
                                    brevtype = request.brevtype,
                                    språk = request.sprak,
                                )
                            }
                            respond(BestillBrevResponse(referanse), HttpStatusCode.Created)
                        }
                    }
                    route("/bestilling") {
                        route("/{referanse}") {
                            authorizedGetWithApprovedList<BrevbestillingReferansePathParam, Brevbestilling>(
                                behandlingsflytAzp
                            ) {
                                val brevbestilling = dataSource.transaction { connection ->
                                    BrevbestillingService.konstruer(connection).hent(it.brevbestillingReferanse)
                                }
                                respond(brevbestilling)
                            }

                            put<BrevbestillingReferansePathParam, Unit, Brev>() { referanse, brev ->
                                installerTilgangPluginWithApprovedList(listOf(behandlingsflytAzp))
                                dataSource.transaction { connection ->
                                    BrevbestillingService.konstruer(connection)
                                        .oppdaterBrev(referanse.brevbestillingReferanse, brev)
                                }
                                respondWithStatus(HttpStatusCode.NoContent)
                            }
                        }
                    }
                }
                motorApi(dataSource)
            }
        }
        actuator(prometheus, motor)
    }
}

private fun Application.module(dataSource: DataSource): Motor {
    val motor = Motor(
        dataSource = dataSource,
        antallKammer = 2,
        logInfoProvider = NoExtraLogInfoProvider,
        jobber = listOf(ProsesserBrevbestillingJobbUtfører),
    )

    dataSource.transaction { dbConnection ->
        RetryService(dbConnection).enable()
    }

    environment.monitor.subscribe(ApplicationStarted) {
        motor.start()
    }
    environment.monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        motor.stop()
        // Release resources and unsubscribe from events
        application.environment.monitor.unsubscribe(ApplicationStarted) {}
        application.environment.monitor.unsubscribe(ApplicationStopped) {}
    }

    return motor
}

private fun Routing.actuator(prometheus: PrometheusMeterRegistry, motor: Motor) {
    route("/actuator") {
        get("/metrics") {
            call.respond(prometheus.scrape())
        }

        get("/live") {
            val status = HttpStatusCode.OK
            call.respond(status, "Oppe!")
        }

        get("/ready") {
            if (motor.kjører()) {
                val status = HttpStatusCode.OK
                call.respond(status, "Oppe!")
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable, "Kjører ikke")
            }
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
