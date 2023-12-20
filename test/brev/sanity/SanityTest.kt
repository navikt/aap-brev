package brev.sanity

import brev.Config
import brev.SanityConfig
import brev.server
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SanityTest {

    @Test
    fun `exposes route for finding brevmal by id`() {
        Sanity().use { sanity ->
            val config = Config(SanityConfig("token", sanity.host))
            testApplication {
                application { server(config) }
                val response = httpClient.get("/brev/1") {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                }
                assertEquals(mapOf("result" to "nice"), response.body<Map<String, Any>>())
            }
        }
    }
}

private class Sanity : AutoCloseable {
    private val server = embeddedServer(Netty, port = 0) { fake() }.start()
    val host: String get() = "http://localhost:${server.port()}"

    fun Application.fake() {
        install(ContentNegotiation) {
            jackson {
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                registerModule(JavaTimeModule())
            }
        }
        routing {
            get("/$VERSION/data/query/$DATASET") {
                call.respond(Response(mapOf("result" to "nice")))
            }
        }
    }

    override fun close() = server.stop(0L, 0L)
}

private val ApplicationTestBuilder.httpClient: HttpClient
    get() = createClient {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) { jackson() }
    }

fun NettyApplicationEngine.port() = runBlocking { resolvedConnectors() }.first { it.type == ConnectorType.HTTP }.port