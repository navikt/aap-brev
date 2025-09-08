package no.nav.aap.brev.test.fakes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.nav.aap.brev.distribusjon.BestemDistribusjonskanalResponse
import no.nav.aap.brev.distribusjon.Distribusjonskanal

private var distkanalBrukerId = String()

fun brukerForDistkanal(
    brukerId: String
) {
    distkanalBrukerId = brukerId
}

fun Application.dokdistkanalFake() {
    val BRUKER_MED_PRINT_DISTKANAL = "brukerMedPrintDistkanal"
    val BRUKER_UTEN_DISTKANAL = "brukerUtenDistkanal"

    applicationFakeFelles("dokdistkanal")
    routing {
        post("/rest/bestemDistribusjonskanal") {
            val brukerHarKanal = distkanalBrukerId != BRUKER_UTEN_DISTKANAL

            val response = if (distkanalBrukerId == BRUKER_MED_PRINT_DISTKANAL) {
                Distribusjonskanal.PRINT
            } else if (!brukerHarKanal) {
                null
            } else {
                Distribusjonskanal.SDP
            }

            val status = if (brukerHarKanal) HttpStatusCode.OK else HttpStatusCode.NoContent
            call.respond(status, BestemDistribusjonskanalResponse(distribusjonskanal = response))
        }
    }
}
