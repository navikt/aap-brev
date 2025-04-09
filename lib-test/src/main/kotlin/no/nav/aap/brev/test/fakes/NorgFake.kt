package no.nav.aap.brev.test.fakes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.brev.organisasjon.NorgEnhet

fun Application.norgFake() {
    applicationFakeFelles("norg")
    routing {
        get("/norg2/api/v1/enhet") {
            call.respond(listOf(NorgEnhet("1234", "Navn", "LOKAL")))
        }
        get("/norg2/api/v1/enhet/{enhetNr}/overordnet") {
            call.respond(listOf(NorgEnhet("4321", "Fylkeskontornavn", "FYLKE")))
        }
    }
}