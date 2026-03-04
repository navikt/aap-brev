package no.nav.aap.brev.test.fakes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.brev.organisasjon.NorgEnhet

fun Application.norgFake() {
    applicationFakeFelles("norg")
    routing {
        get("/norg2/api/v1/enhet") {
            val enheter = call.queryParameters.getAll("enhetsnummerListe")
                ?.map { NorgEnhet(it, "Enhetnavn $it", "LOKAL") }
                ?: emptyList()
            call.respond(enheter)
        }
        get("/norg2/api/v1/enhet/{enhetNr}/overordnet") {
            call.respond(listOf(NorgEnhet("4321", "Fylkeskontornavn", "FYLKE")))
        }
    }
}