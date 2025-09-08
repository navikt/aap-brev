package no.nav.aap.brev

import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
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
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.brev.api.ErrorResponse
import no.nav.aap.brev.api.bestillingApi
import no.nav.aap.brev.api.distribusjonApi
import no.nav.aap.brev.api.dokumentinnhentingApi
import no.nav.aap.brev.exception.ValideringsfeilException
import no.nav.aap.brev.prosessering.ProsesserBrevbestillingJobbUtfører
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.server.AZURE
import no.nav.aap.komponenter.server.commonKtorModule
import no.nav.aap.motor.Motor
import no.nav.aap.motor.api.motorApi
import no.nav.aap.motor.mdc.NoExtraLogInfoProvider
import no.nav.aap.motor.retry.RetryService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

private val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLog")
private val LOGGER = LoggerFactory.getLogger(App::class.java)

class App

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e ->
        SECURE_LOGGER.error("Uhåndtert feil", e)
        LOGGER.error("Uhåndert feil. Se sikker logg for stacktrace")
    }

    val serverPort = System.getenv("HTTP_PORT")?.toInt() ?: 8080
    embeddedServer(Netty, configure = {
        shutdownGracePeriod = TimeUnit.SECONDS.toMillis(5)
        shutdownTimeout = TimeUnit.SECONDS.toMillis(10)
        connector {
            port = serverPort
        }
    }) {
        server(DbConfig())
    }.start(wait = true)
}

internal fun Application.server(dbConfig: DbConfig) {

    commonKtorModule(prometheus, AzureConfig(), InfoModel(title = "AAP - Brev"))

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is ValideringsfeilException -> {
                    LoggerFactory.getLogger(App::class.java)
                        .warn(cause.message, cause)
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message))
                }

                else -> {
                    LoggerFactory.getLogger(App::class.java)
                        .warn("Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                    call.respond(status = HttpStatusCode.InternalServerError, message = ErrorResponse(cause.message))
                }
            }
        }
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    val dataSource = initDatasource(dbConfig, prometheus)
    Migrering.migrate(dataSource)

    val motor = module(dataSource)

    routing {
        authenticate(AZURE) {
            apiRouting {
                bestillingApi(dataSource)
                dokumentinnhentingApi()
                distribusjonApi()
                motorApi(dataSource)
            }
        }
        actuator(prometheus, motor)
    }
}

private fun Application.module(dataSource: DataSource): Motor {
    val motor =
        Motor(
            dataSource = dataSource,
            antallKammer = 2,
            logInfoProvider = NoExtraLogInfoProvider,
            jobber = listOf(ProsesserBrevbestillingJobbUtfører),
            prometheus = prometheus,
        )

    dataSource.transaction { dbConnection ->
        RetryService(dbConnection).enable()
    }

    monitor.subscribe(ApplicationStarted) {
        motor.start()
    }
    monitor.subscribe(ApplicationStopPreparing) { environment ->
        environment.log.info("Forbereder stopp av applikasjon, stopper motor.")
        motor.stop()
    }
    monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet.")
        // Release resources and unsubscribe from events
        application.monitor.unsubscribe(ApplicationStarted) {}
        application.monitor.unsubscribe(ApplicationStopPreparing) {}
        application.monitor.unsubscribe(ApplicationStopped) {}
    }

    return motor
}

private fun Routing.actuator(
    prometheus: PrometheusMeterRegistry,
    motor: Motor
) {
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
    val jdbcUrl: String = System.getenv("NAIS_DATABASE_BREV_BREV_JDBC_URL")
)

fun initDatasource(dbConfig: DbConfig, meterRegistry: MeterRegistry) =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = dbConfig.jdbcUrl
            maximumPoolSize = 10
            minimumIdle = 1
            driverClassName = "org.postgresql.Driver"
            connectionTestQuery = "SELECT 1"
            metricRegistry = meterRegistry
        }
    )