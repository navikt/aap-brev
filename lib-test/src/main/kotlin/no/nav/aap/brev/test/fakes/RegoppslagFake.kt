package no.nav.aap.brev.test.fakes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.nav.aap.brev.distribusjon.HentPostadresseResponse

private var regoppslagBrukerId = String()

fun brukerForRegoppslag(
    brukerId: String
) {
    regoppslagBrukerId = brukerId
}

fun Application.regoppslagFake() {
    val BRUKER_UTEN_POSTADRESSE = "brukerUtenPostadresse"

    applicationFakeFelles("regoppslag")
    routing {
        post("/rest/postadresse") {
            val brukerHarAdresse = regoppslagBrukerId != BRUKER_UTEN_POSTADRESSE

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
