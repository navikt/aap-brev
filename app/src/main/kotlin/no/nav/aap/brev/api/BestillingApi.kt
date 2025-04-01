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
import no.nav.aap.brev.bestilling.SorterbarSignatur
import no.nav.aap.brev.bestilling.UnikReferanse
import no.nav.aap.brev.bestilling.Vedlegg
import no.nav.aap.brev.journalføring.DokumentInfoId
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.journalføring.SignaturService
import no.nav.aap.brev.kontrakt.AvbrytBrevbestillingRequest
import no.nav.aap.brev.kontrakt.BestillBrevRequest
import no.nav.aap.brev.kontrakt.BestillBrevResponse
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.FerdigstillBrevRequest
import no.nav.aap.brev.kontrakt.HentSignaturerRequest
import no.nav.aap.brev.kontrakt.HentSignaturerResponse
import no.nav.aap.brev.person.PdlGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.tilgang.authorizedPut
import org.slf4j.MDC
import no.nav.aap.tilgang.Operasjon
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
                MDC.putCloseable(MDCNøkler.SAKSNUMMER.key, request.saksnummer).use {
                    MDC.putCloseable(MDCNøkler.BEHANDLING_REFERANSE.key, request.behandlingReferanse.toString()).use {
                        val bestillingResultat = dataSource.transaction { connection ->
                            BrevbestillingService.konstruer(connection).opprettBestilling(
                                saksnummer = Saksnummer(request.saksnummer),
                                brukerIdent = request.brukerIdent,
                                behandlingReferanse = BehandlingReferanse(request.behandlingReferanse),
                                unikReferanse = UnikReferanse(request.unikReferanse),
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
                        val httpStatusCode = if (bestillingResultat.alleredeOpprettet) {
                            HttpStatusCode.Conflict
                        } else {
                            HttpStatusCode.Created
                        }

                        respond(BestillBrevResponse(bestillingResultat.referanse.referanse), httpStatusCode)
                    }
                }
            }
        }
        route("/bestilling") {
            route("/{referanse}") {
                authorizedGet<BrevbestillingReferansePathParam, BrevbestillingResponse>(
                    AuthorizationParamPathConfig(
                        applicationRole = "hent-brev",
                        applicationsOnly = true
                    )
                ) { brevbestillingReferanse ->
                    MDC.putCloseable(
                        MDCNøkler.BESTILLING_REFERANSE.key,
                        brevbestillingReferanse.brevbestillingReferanse.referanse.toString()
                    ).use {

                        val brevbestilling = dataSource.transaction { connection ->
                            BrevbestillingService.konstruer(connection)
                                .hent(brevbestillingReferanse.brevbestillingReferanse)
                        }
                        respond(brevbestilling.tilResponse())
                    }
                }
                route("/oppdater") {
                    authorizedPut<BrevbestillingReferansePathParam, Unit, Brev>(authorizationBodyPathConfig) { referanse, brev ->
                        MDC.putCloseable(MDCNøkler.BESTILLING_REFERANSE.key, referanse.referanse.toString()).use {
                            dataSource.transaction { connection ->
                                BrevbestillingService.konstruer(connection)
                                    .oppdaterBrev(referanse.brevbestillingReferanse, brev)
                            }
                            respondWithStatus(HttpStatusCode.NoContent)
                        }
                    }
                }
            }
        }
        route("/ferdigstill") {
            authorizedPost<Unit, String, FerdigstillBrevRequest>(authorizationBodyPathConfig) { _, request ->
                MDC.putCloseable(MDCNøkler.BESTILLING_REFERANSE.key, request.referanse.toString()).use {
                    dataSource.transaction { connection ->
                        BrevbestillingService.konstruer(connection)
                            .ferdigstill(
                                referanse = BrevbestillingReferanse(request.referanse),
                                signaturer = request.signaturer
                            )
                    }
                    respond("{}", HttpStatusCode.Accepted)
                }
            }
        }
        route("/avbryt") {
            authorizedPost<Unit, String, AvbrytBrevbestillingRequest>(authorizationBodyPathConfig) { _, request ->
                MDC.putCloseable(MDCNøkler.BESTILLING_REFERANSE.key, request.referanse.toString()).use {
                    dataSource.transaction { connection ->
                        BrevbestillingService.konstruer(connection)
                            .avbryt(BrevbestillingReferanse(request.referanse))
                    }
                    respond("{}", HttpStatusCode.Accepted)
                }
            }
        }
        route("/forhandsvis-signaturer") {
            authorizedPost<Unit, HentSignaturerResponse, HentSignaturerRequest>(authorizationBodyPathConfig) { _, request ->
                val personinfoGateway = PdlGateway()
                val personinfo = personinfoGateway.hentPersoninfo(request.brukerIdent)
                val signaturService = SignaturService.konstruer()
                val signaturer = signaturService.signaturer(
                    sorterbareSignaturer = request.signaturGrunnlag.mapIndexed { index, signatur -> SorterbarSignatur(signatur.navIdent, index) },
                    request.brevtype,
                    personinfo
                )
                respond(HentSignaturerResponse(signaturer))
            }
        }
    }
}
