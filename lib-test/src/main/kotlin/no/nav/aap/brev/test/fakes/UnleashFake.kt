package no.nav.aap.brev.test.fakes

import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.unleashFake() {
    applicationFakeFelles("unleash")
    routing {
        get("/api/client/features") {
            call.respond(
                mapOf(
                    "version" to 1,
                    "features" to emptyList<Any>(),
                )
            )
        }
    }
}
