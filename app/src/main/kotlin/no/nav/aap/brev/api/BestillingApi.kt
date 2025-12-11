package no.nav.aap.brev.api

import no.nav.aap.brev.kontrakt.BrevdataDto
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.brev.bestilling.BehandlingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.bestilling.PdfService
import no.nav.aap.brev.bestilling.Saksnummer
import no.nav.aap.brev.bestilling.UnikReferanse
import no.nav.aap.brev.bestilling.Vedlegg
import no.nav.aap.brev.bestilling.tilSorterbareSignaturer
import no.nav.aap.brev.innhold.BrevinnholdService
import no.nav.aap.brev.journalføring.DokumentInfoId
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.journalføring.SignaturService
import no.nav.aap.brev.kontrakt.AvbrytBrevbestillingRequest
import no.nav.aap.brev.kontrakt.BestillBrevResponse
import no.nav.aap.brev.kontrakt.BestillBrevV2Request
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.FerdigstillBrevRequest
import no.nav.aap.brev.kontrakt.ForhandsvisBrevRequest
import no.nav.aap.brev.kontrakt.GjenopptaBrevbestillingRequest
import no.nav.aap.brev.kontrakt.HentSignaturerRequest
import no.nav.aap.brev.kontrakt.HentSignaturerResponse
import no.nav.aap.brev.kontrakt.OppdaterBrevmalRequest
import no.nav.aap.brev.person.PdlGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.tilgang.authorizedPut
import org.slf4j.MDC
import javax.sql.DataSource

fun NormalOpenAPIRoute.bestillingApi(dataSource: DataSource) {

    val authorizationBodyPathConfig = AuthorizationBodyPathConfig(
        operasjon = Operasjon.SAKSBEHANDLE,
        applicationRole = "bestill-brev",
        applicationsOnly = true
    )

    route("/api") {
        route("/v2/bestill") {
            authorizedPost<Unit, BestillBrevResponse, BestillBrevV2Request>(authorizationBodyPathConfig) { _, request ->
                MDC.putCloseable(MDCNøkler.SAKSNUMMER.key, request.saksnummer).use {
                    MDC.putCloseable(MDCNøkler.BEHANDLING_REFERANSE.key, request.behandlingReferanse.toString())
                        .use {
                            val bestillingResultat = dataSource.transaction { connection ->
                                BrevbestillingService.konstruer(connection).opprettBestillingV2(
                                    saksnummer = Saksnummer(request.saksnummer),
                                    brukerIdent = request.brukerIdent,
                                    behandlingReferanse = BehandlingReferanse(request.behandlingReferanse),
                                    unikReferanse = UnikReferanse(request.unikReferanse),
                                    brevtype = request.brevtype,
                                    språk = request.sprak,
                                    faktagrunnlag = request.faktagrunnlag,
                                    vedlegg = request.vedlegg.map {
                                        Vedlegg(
                                            JournalpostId(it.journalpostId),
                                            DokumentInfoId(it.dokumentInfoId)
                                        )
                                    }.toSet(),
                                    ferdigstillAutomatisk = request.ferdigstillAutomatisk,
                                )
                            }
                            val httpStatusCode = if (bestillingResultat.alleredeOpprettet) {
                                HttpStatusCode.Conflict
                            } else {
                                HttpStatusCode.Created
                            }

                            respond(
                                BestillBrevResponse(
                                    bestillingResultat.brevbestilling.referanse.referanse,
                                ), httpStatusCode
                            )
                        }
                }
            }
        }
        route("/v3/bestill") {
            authorizedPost<Unit, BestillBrevResponse, BestillBrevV2Request>(authorizationBodyPathConfig) { _, request ->
                MDC.putCloseable(MDCNøkler.SAKSNUMMER.key, request.saksnummer).use {
                    MDC.putCloseable(MDCNøkler.BEHANDLING_REFERANSE.key, request.behandlingReferanse.toString())
                        .use {
                            if (Miljø.erProd()) {
                                respondWithStatus(HttpStatusCode.NotImplemented)
                                return@authorizedPost
                            }
                            val bestillingResultat = dataSource.transaction { connection ->
                                BrevbestillingService.konstruer(connection).opprettBestillingV3(
                                    saksnummer = Saksnummer(request.saksnummer),
                                    brukerIdent = request.brukerIdent,
                                    behandlingReferanse = BehandlingReferanse(request.behandlingReferanse),
                                    unikReferanse = UnikReferanse(request.unikReferanse),
                                    brevtype = request.brevtype,
                                    språk = request.sprak,
                                    faktagrunnlag = request.faktagrunnlag,
                                    vedlegg = request.vedlegg.map {
                                        Vedlegg(
                                            JournalpostId(it.journalpostId),
                                            DokumentInfoId(it.dokumentInfoId)
                                        )
                                    }.toSet(),
                                    ferdigstillAutomatisk = request.ferdigstillAutomatisk,
                                )
                            }
                            val httpStatusCode = if (bestillingResultat.alleredeOpprettet) {
                                HttpStatusCode.Conflict
                            } else {
                                HttpStatusCode.Created
                            }

                            respond(
                                BestillBrevResponse(
                                    bestillingResultat.brevbestilling.referanse.referanse,
                                ), httpStatusCode
                            )
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
                route("/v3/oppdater") {
                    authorizedPut<BrevbestillingReferansePathParam, Unit, BrevdataDto>(authorizationBodyPathConfig) { referanse, brevdata ->
                        MDC.putCloseable(MDCNøkler.BESTILLING_REFERANSE.key, referanse.referanse.toString()).use {
                            if (Miljø.erProd()) {
                                respondWithStatus(HttpStatusCode.NotImplemented)
                                return@authorizedPut
                            }
                            dataSource.transaction { connection ->
                                BrevbestillingService.konstruer(connection)
                                    .oppdaterBrevdata(referanse.brevbestillingReferanse, brevdata.tilBrevdata())
                            }
                            respondWithStatus(HttpStatusCode.NoContent)
                        }
                    }
                }
                route("/forhandsvis") {
                    authorizedPost<BrevbestillingReferansePathParam, ByteArray, ForhandsvisBrevRequest>(
                        authorizationBodyPathConfig
                    ) { referanse, request ->
                        val pdf = dataSource.transaction { connection ->
                            PdfService.konstruer(connection)
                                .genererPdfForForhåndsvisning(referanse.brevbestillingReferanse, request.signaturer)

                        }
                        respond(pdf.bytes)
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
                                signaturer = request.signaturer,
                                mottakere = request.mottakere.tilMottakere(request.referanse)
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
                    sorterbareSignaturer = request.signaturGrunnlag.tilSorterbareSignaturer(),
                    brevtype = request.brevtype,
                    personinfo = personinfo
                )
                respond(HentSignaturerResponse(signaturer))
            }
        }
        route("/gjenoppta-bestilling") {
            authorizedPost<Unit, String, GjenopptaBrevbestillingRequest>(authorizationBodyPathConfig) { _, request ->
                MDC.putCloseable(MDCNøkler.BESTILLING_REFERANSE.key, request.referanse.toString()).use {
                    dataSource.transaction { connection ->
                        BrevbestillingService.konstruer(connection)
                            .gjenoppta(BrevbestillingReferanse(request.referanse))
                    }
                    respond("{}", HttpStatusCode.Accepted)
                }
            }
        }
        route("/oppdater-brevmal") {
            authorizedPut<Unit, String, OppdaterBrevmalRequest>(authorizationBodyPathConfig) { _, request ->
                MDC.putCloseable(MDCNøkler.BESTILLING_REFERANSE.key, request.referanse.toString()).use {
                    dataSource.transaction { connection ->
                        BrevinnholdService.konstruer(connection)
                            .hentOgLagreBrevmal(BrevbestillingReferanse(request.referanse))
                    }
                    respondWithStatus(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
