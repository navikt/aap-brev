package no.nav.aap.brev.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.brev.bestilling.BehandlingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.bestilling.Saksnummer
import no.nav.aap.brev.bestilling.Vedlegg
import no.nav.aap.brev.journalføring.DokumentInfoId
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.kontrakt.BestillBrevRequest
import no.nav.aap.brev.kontrakt.BestillBrevResponse
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.FerdigstillBrevRequest
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.tilgang.authorizedPut
import tilgang.Operasjon
import javax.sql.DataSource


fun NormalOpenAPIRoute.bestillingApi(dataSource: DataSource) {

    val authorizationBodyPathConfig = AuthorizationBodyPathConfig(
        operasjon = Operasjon.SAKSBEHANDLE,
        applicationRole = "bestill-brev",
        applicationsOnly = true
    )

    route("/api") {
        route("/bestill") {
            authorizedPost<Unit, BestillBrevResponse, BestillBrevRequest>(authorizationBodyPathConfig) { _, request ->
                val referanse = dataSource.transaction { connection ->
                    BrevbestillingService.konstruer(connection).opprettBestilling(
                        saksnummer = Saksnummer(request.saksnummer),
                        behandlingReferanse = BehandlingReferanse(request.behandlingReferanse),
                        unikReferanse = request.unikReferanse,
                        brevtype = request.brevtype,
                        språk = request.sprak,
                        vedlegg = request.vedlegg.map {
                            Vedlegg(
                                JournalpostId(it.journalpostId),
                                DokumentInfoId(it.dokumentInfoId)
                            )
                        }.toSet(),
                    )
                }
                respond(BestillBrevResponse(referanse.referanse), HttpStatusCode.Created)
            }
        }
        route("/bestilling") {
            route("/{referanse}") {
                authorizedGet<BrevbestillingReferansePathParam, BrevbestillingResponse>(
                    AuthorizationParamPathConfig(
                        applicationRole = "hent-brev",
                        applicationsOnly = true
                    )
                ) {
                    val brevbestilling = dataSource.transaction { connection ->
                        BrevbestillingService.konstruer(connection).hent(it.brevbestillingReferanse)
                    }
                    respond(brevbestilling.tilResponse())
                }
                route("/oppdater") {
                    authorizedPut<BrevbestillingReferansePathParam, Unit, Brev>(authorizationBodyPathConfig) { referanse, brev ->
                        dataSource.transaction { connection ->
                            BrevbestillingService.konstruer(connection)
                                .oppdaterBrev(referanse.brevbestillingReferanse, brev)
                        }
                        respondWithStatus(HttpStatusCode.NoContent)
                    }
                }
            }
        }
        route("/ferdigstill") {
            authorizedPost<Unit, String, FerdigstillBrevRequest>(authorizationBodyPathConfig) { _, request ->
                dataSource.transaction { connection ->
                    BrevbestillingService.konstruer(connection).ferdigstill(BrevbestillingReferanse(request.referanse))
                }
                respond("{}", HttpStatusCode.Accepted)
            }
        }
    }
}
