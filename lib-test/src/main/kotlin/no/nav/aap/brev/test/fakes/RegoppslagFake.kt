package no.nav.aap.brev.test.fakes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.nav.aap.brev.distribusjon.HentPostadresseRequest
import no.nav.aap.brev.distribusjon.HentPostadresseResponse
import no.nav.aap.komponenter.json.DefaultJsonMapper

fun Application.regoppslagFake() {
    val BRUKER_UTEN_POSTADRESSE = "brukerUtenPostadresse"

    applicationFakeFelles("regoppslag")
    routing {
        post("/rest/hentPostadresse") {
            val request = DefaultJsonMapper.fromJson< HentPostadresseRequest>(call.receiveText())
            val brukerHarAdresse = request.ident != BRUKER_UTEN_POSTADRESSE

            if (brukerHarAdresse) {
                call.respond(
                    HttpStatusCode.OK, HentPostadresseResponse(
                        adresseKilde = "OPPSLAG_REGISTER",
                        type = "BOSTEDSADRESSE",
                        adresselinje1 = "Testgaten 123",
                        adresselinje2 = null,
                        adresselinje3 = null,
                        postnummer = "1234",
                        poststed = "Testbyen",
                        landkode = "NOR",
                        land = "Norge"
                    )
                )
            } else {
                call.respond(HttpStatusCode.NoContent, null)
            }
        }
    }
}
