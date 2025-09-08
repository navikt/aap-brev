package no.nav.aap.brev.test.fakes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.nav.aap.brev.distribusjon.BestemDistribusjonskanalRequest
import no.nav.aap.brev.distribusjon.BestemDistribusjonskanalResponse
import no.nav.aap.brev.distribusjon.Distribusjonskanal
import no.nav.aap.komponenter.json.DefaultJsonMapper

fun Application.dokdistkanalFake() {
    val BRUKER_MED_PRINT_KANAL = "brukerMedPrintKanal"
    val BRUKER_UTEN_KANAL = "brukerUtenKanal"

    applicationFakeFelles("dokdistkanal")
    routing {
        post("/rest/bestemDistribusjonskanal") {
            val request = DefaultJsonMapper.fromJson<BestemDistribusjonskanalRequest>(call.receiveText())
            val brukerHarKanal = request.brukerId != BRUKER_UTEN_KANAL

            val response = if (request.brukerId == BRUKER_MED_PRINT_KANAL) {
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
