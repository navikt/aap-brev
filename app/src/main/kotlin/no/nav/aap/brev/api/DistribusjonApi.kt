package no.nav.aap.brev.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import no.nav.aap.brev.distribusjon.BestemDistribusjonskanalRequest
import no.nav.aap.brev.distribusjon.Distribusjonskanal
import no.nav.aap.brev.distribusjon.DokdistkanalGateway
import no.nav.aap.brev.distribusjon.HentPostadresseRequest
import no.nav.aap.brev.distribusjon.Postadresse
import no.nav.aap.brev.distribusjon.RegoppslagGateway
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost

fun NormalOpenAPIRoute.distribusjonApi() {
    val authorizationBodyPathConfig = AuthorizationBodyPathConfig(
        operasjon = Operasjon.SAKSBEHANDLE,
        applicationRole = "distribusjon",
        applicationsOnly = true
    )

    val regoppslagGateway = RegoppslagGateway()
    val dokdistkanalGateway = DokdistkanalGateway()

    route("/hent-postadresse") {
        authorizedPost<Unit, Postadresse, HentPostadresseRequest>(
            authorizationBodyPathConfig
        ) { _, request ->
            val regoppslagResponse = regoppslagGateway.hentPostadresse(
                personident = request.ident,
            )

            if (regoppslagResponse == null) {
                respondWithStatus(HttpStatusCode.NoContent)
            } else {
                respond(regoppslagResponse, HttpStatusCode.OK)
            }
        }
    }

    route("/hent-distribusjonskanal") {
        authorizedPost<Unit, Distribusjonskanal, BestemDistribusjonskanalRequest>(
            authorizationBodyPathConfig
        ) { _, request ->
            val dokdistkanalResponse = dokdistkanalGateway.bestemDistribusjonskanal(
                personident = request.brukerId,
            )

            if (dokdistkanalResponse == null) {
                respondWithStatus(HttpStatusCode.NoContent)
            } else {
                respond(dokdistkanalResponse, HttpStatusCode.OK)
            }
        }
    }
}
