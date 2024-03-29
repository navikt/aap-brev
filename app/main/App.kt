import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLog")
val PUBLIC_LOGGER: Logger = LoggerFactory.getLogger("brev")

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e ->
        SECURE_LOGGER.error("Uhåndtert feil", e)
    }
    embeddedServer(Netty, port = 8080, module = Application::server).start(wait = true)
}

fun Application.server(config: Config = Config()) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val sanity = SanityClient(config.sanity)

    Hikari.init(config.postgres)

    install(MicrometerMetrics) { registry = prometheus }

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    routing {
        route("/draft") {
            post("/{id}") {
                val id = call.parameters["id"]?.let(UUID::fromString) ?: UUID.randomUUID()

                DraftRepo.insert(
                    id = id,
                    raw = call.receive()
                )

                call.respond(HttpStatusCode.Created, id)
            }

            put("/{id}") {
                val id = call.parameters["id"]?.let(UUID::fromString)
                    ?: return@put call.respond(HttpStatusCode.BadRequest)

                DraftRepo.update(
                    id = id,
                    raw = call.receive()
                )

                call.respond(HttpStatusCode.OK)
            }

            get("/{id}") {
                val id = call.parameters["id"]?.let(UUID::fromString)
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

                DraftRepo.selectById(id)?.let { call.respond(it.raw) }
                    ?: call.respond(HttpStatusCode.NotFound)
            }

            get("/all") {
                call.respond(DraftRepo.selectAll().map { it.raw })
            }
        }

        route("/brev") {
            get("/{id}") {
                val id = requireNotNull(call.parameters["id"]) { "parameter 'id' mangler." }

                sanity.brevmal(id)
                    .onSuccess { call.respond(HttpStatusCode.OK, Domain.from(it)) }
                    .onFailure { call.respond(HttpStatusCode.NotFound) }
            }

            get {
                sanity.brevmaler()
                    .onSuccess { call.respond(HttpStatusCode.OK, it.map(Domain::from)) }
                    .onFailure { call.respond(HttpStatusCode.NotFound) }
            }
        }

        route("/actuator") {
            get("/metrics") {
                call.respond(prometheus.scrape())
            }
            get("/live") {
                call.respond(HttpStatusCode.OK, "live")
            }
            get("/ready") {
                call.respond(HttpStatusCode.OK, "ready")
                call.respond(HttpStatusCode(503, "Service Unavailable"))
            }
        }
    }
}

