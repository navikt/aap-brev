package no.nav.aap.brev.test.fakes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.safFake() {
    applicationFakeFelles("saf")
    routing {
        post("/graphql") {
            call.respond("{}")
        }
    }
}
