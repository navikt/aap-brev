package no.nav.aap.brev.test.fakes

import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import tilgang.BehandlingTilgangRequest
import tilgang.JournalpostTilgangRequest
import tilgang.SakTilgangRequest
import tilgang.TilgangResponse

fun Application.tilgangFake() {
    applicationFakeFelles("tilgang")
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