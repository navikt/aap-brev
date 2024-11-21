package no.nav.aap.brev.test.fakes

import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.pdfGenFake() {
    applicationFakeFelles("brev-sanity-proxy")
    routing {
        post("/api/v1/genpdf/aap-saksbehandling-pdfgen/brev") {
            call.respond(ByteArray(0))
        }
    }
}
