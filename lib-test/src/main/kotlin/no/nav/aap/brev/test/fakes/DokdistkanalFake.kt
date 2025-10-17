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

private var brukerIdTilDistkanal = mutableMapOf<String, Distribusjonskanal?>()

fun brukerForDistkanal(
    brukerId: String?,
    kanal: Distribusjonskanal
) {
    if (brukerId != null) {
        brukerIdTilDistkanal[brukerId] = kanal
    }
}

fun Application.dokdistkanalFake() {
    applicationFakeFelles("dokdistkanal")
    routing {
        post("/rest/bestemDistribusjonskanal") {
            val request = DefaultJsonMapper.fromJson<BestemDistribusjonskanalRequest>(call.receiveText())
            val kanal = brukerIdTilDistkanal[request.brukerId]
            val status = if (kanal != null) HttpStatusCode.OK else HttpStatusCode.NoContent
            call.respond(status, BestemDistribusjonskanalResponse(distribusjonskanal = kanal))
        }
    }
}
