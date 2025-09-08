package no.nav.aap.brev.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import no.nav.aap.brev.distribusjon.Distribusjonskanal
import no.nav.aap.brev.distribusjon.DokdistkanalGateway
import no.nav.aap.brev.distribusjon.RegoppslagGateway
import no.nav.aap.brev.kontrakt.KanDistribuereBrevReponse
import no.nav.aap.brev.kontrakt.KanDistribuereBrevRequest
import no.nav.aap.brev.kontrakt.MottakerDistStatus
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost

fun NormalOpenAPIRoute.distribusjonApi() {
    val authorizationBodyPathConfig = AuthorizationBodyPathConfig(
        operasjon = Operasjon.SAKSBEHANDLE,
        applicationRole = "hent-distribusjoninfo",
        applicationsOnly = true
    )

    val regoppslagGateway = RegoppslagGateway()
    val dokdistkanalGateway = DokdistkanalGateway()

    route("/api/distribusjon") {
        route("/kan-distribuere-brev") {
            authorizedPost<Unit, KanDistribuereBrevReponse, KanDistribuereBrevRequest>(
                authorizationBodyPathConfig
            ) { _, request ->
                val mottakereDistStatus = mutableListOf<MottakerDistStatus>()
                request.mottakere.forEach { mottaker ->
                    val personident = mottaker.ident

                    if (personident != null) {
                        val distribusjonskanal = dokdistkanalGateway.bestemDistribusjonskanal(personident)
                        val mottakerPostadresse = regoppslagGateway.hentPostadresse(personident)
                        val kanDistribuereBrev = (distribusjonskanal != Distribusjonskanal.PRINT) || mottakerPostadresse != null
                        mottakereDistStatus.add(MottakerDistStatus(mottaker, kanDistribuereBrev))
                    }
                }

                if (mottakereDistStatus.isEmpty()) {
                    respondWithStatus(HttpStatusCode.NoContent)
                } else {
                    respond(KanDistribuereBrevReponse(mottakereDistStatus), HttpStatusCode.OK)
                }
            }
        }
    }
}
