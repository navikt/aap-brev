package no.nav.aap.brev.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import no.nav.aap.brev.distribusjon.DistribusjonService
import no.nav.aap.brev.kontrakt.KanDistribuereBrevReponse
import no.nav.aap.brev.kontrakt.KanDistribuereBrevRequest
import no.nav.aap.brev.kontrakt.MottakerDistStatus
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import javax.sql.DataSource

fun NormalOpenAPIRoute.distribusjonApi(dataSource: DataSource) {
    val authorizationBodyPathConfig = AuthorizationBodyPathConfig(
        operasjon = Operasjon.SAKSBEHANDLE,
        applicationRole = "hent-distribusjoninfo",
        applicationsOnly = true
    )

    route("/api/distribusjon") {
        route("/kan-distribuere-brev") {
            authorizedPost<Unit, KanDistribuereBrevReponse, KanDistribuereBrevRequest>(
                authorizationBodyPathConfig
            ) { _, request ->
                val mottakereDistStatus = mutableListOf<MottakerDistStatus>()
                request.mottakere.forEach { mottaker ->
                    val personident = mottaker.ident

                    if (personident != null) {
                        val kanDistribuereBrev = dataSource.transaction { connection ->
                            DistribusjonService.konstruer(connection).kanBrevDistribueresTilBruker(personident)
                        }
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
