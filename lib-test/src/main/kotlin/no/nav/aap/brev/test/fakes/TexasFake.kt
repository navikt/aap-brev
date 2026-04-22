package no.nav.aap.brev.test.fakes

import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.nav.aap.brev.test.AzureTokenGen

fun Application.texasFake() {
    applicationFakeFelles("texas")
    routing {
        post("/token") {
            val token = AzureTokenGen("brev", "brev").generate()
            call.respond(TestToken(access_token = token))
        }
        post("/introspect") {
            call.respond(mapOf("active" to true))
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
