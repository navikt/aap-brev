package no.nav.aap.brev.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.brev.arkivoppslag.ArkivoppslagGateway
import no.nav.aap.brev.bestilling.PdfBrev
import no.nav.aap.brev.bestilling.PdfBrev.Blokk
import no.nav.aap.brev.bestilling.PdfBrev.FormattertTekst
import no.nav.aap.brev.bestilling.PdfBrev.Innhold
import no.nav.aap.brev.bestilling.PdfBrev.Mottaker
import no.nav.aap.brev.bestilling.PdfBrev.Mottaker.IdentType
import no.nav.aap.brev.bestilling.PdfBrev.Tekstbolk
import no.nav.aap.brev.bestilling.PdfGateway
import no.nav.aap.brev.bestilling.Saksnummer
import no.nav.aap.brev.journalføring.JournalføringData
import no.nav.aap.brev.journalføring.JournalføringData.MottakerType
import no.nav.aap.brev.journalføring.JournalføringGateway
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.kontrakt.BlokkType
import no.nav.aap.brev.kontrakt.EkspederBehandlerBestillingRequest
import no.nav.aap.brev.kontrakt.HentSignaturDokumentinnhentingRequest
import no.nav.aap.brev.kontrakt.JournalførBehandlerBestillingRequest
import no.nav.aap.brev.kontrakt.JournalførBehandlerBestillingResponse
import no.nav.aap.brev.kontrakt.Signatur
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.organisasjon.AnsattInfoDevGateway
import no.nav.aap.brev.organisasjon.AnsattInfoGateway
import no.nav.aap.brev.organisasjon.NomInfoGateway
import no.nav.aap.brev.organisasjon.NorgGateway
import no.nav.aap.brev.person.PdlGateway
import no.nav.aap.brev.util.TimeUtils.formaterFullLengde
import no.nav.aap.komponenter.httpklient.httpclient.error.BadRequestHttpResponsException
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import org.slf4j.LoggerFactory

fun NormalOpenAPIRoute.dokumentinnhentingApi(
    pdfGateway: PdfGateway,
    journalføringGateway: JournalføringGateway,
    arkivoppslagGateway: ArkivoppslagGateway,
) {

    val log = LoggerFactory.getLogger(this::class.java)

    val authorizationBodyPathConfig = AuthorizationBodyPathConfig(
        operasjon = Operasjon.SAKSBEHANDLE,
        applicationRole = "dokumentinnhenting-api",
        applicationsOnly = true
    )

    route("/api/dokumentinnhenting") {
        route("/journalfor-behandler-bestilling") {
            authorizedPost<Unit, JournalførBehandlerBestillingResponse, JournalførBehandlerBestillingRequest>(
                authorizationBodyPathConfig
            ) { _, request ->

                val signatur = utledSignatur(brukerFnr = request.brukerFnr, navIdent = request.bestillerNavIdent)

                val pdfBrev = mapPdfBrev(request, signatur?.let { listOf(it) } ?: emptyList())
                val pdf = pdfGateway.genererPdf(pdfBrev)
                val journalpostResponse = journalføringGateway.journalførBrev(
                    journalføringData = JournalføringData(
                        brukerFnr = request.brukerFnr,
                        mottakerIdent = request.mottakerHprnr,
                        mottakerNavn = request.mottakerNavn,
                        mottakerType = MottakerType.HPRNR,
                        saksnummer = Saksnummer(request.saksnummer),
                        eksternReferanseId = request.eksternReferanseId.toString(),
                        tittelJournalpost = request.tittel,
                        tittelBrev = request.tittel,
                        brevkode = request.brevkode,
                        overstyrInnsynsregel = request.overstyrInnsynsregel,
                    ),
                    pdf = pdf,
                    forsøkFerdigstill = true,
                )

                respond(
                    JournalførBehandlerBestillingResponse(
                        journalpostId = journalpostResponse.journalpostId.id,
                        journalpostFerdigstilt = journalpostResponse.journalpostferdigstilt,
                        dokumenter = journalpostResponse.dokumenter.map { it.dokumentInfoId }),
                    HttpStatusCode.Created
                )
            }
        }
        route("/ekspeder-journalpost-behandler-bestilling") {
            authorizedPost<Unit, String, EkspederBehandlerBestillingRequest>(
                authorizationBodyPathConfig
            ) { _, request ->
                try {
                    journalføringGateway.ekspediterJournalpost(request.journalpostId, "HELSENETTET")
                } catch (e: BadRequestHttpResponsException) {
                    val journalpost = arkivoppslagGateway.hentJournalpost(JournalpostId(request.journalpostId))
                    if (journalpost.journalstatus == "EKSPEDERT") {
                        log.info("Journalpost ${request.journalpostId} er allerede ekspedert.")
                    } else {
                        throw e
                    }
                }

                respond("{}", HttpStatusCode.OK)
            }
        }

        route("/forhandsvis-signatur") {
            authorizedPost<Unit, Signatur, HentSignaturDokumentinnhentingRequest>(
                authorizationBodyPathConfig
            ) { _, request ->
                val signatur = utledSignatur(brukerFnr = request.brukerFnr, navIdent = request.bestillerNavIdent)

                if (signatur != null) {
                    respond(signatur)
                } else {
                    respondWithStatus(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

private fun utledSignatur(brukerFnr: String, navIdent: String): Signatur? {
    val ansattInfoGateway: AnsattInfoGateway =
        if (Miljø.er() == MiljøKode.DEV) AnsattInfoDevGateway() else NomInfoGateway()
    val personinfoV2Gateway = PdlGateway()
    val enhetGateway = NorgGateway()
    val personinfo = personinfoV2Gateway.hentPersoninfo(brukerFnr)
    return if (personinfo.harStrengtFortroligAdresse) {
        null
    } else {
        val ansattInfo = ansattInfoGateway.hentAnsattInfo(navIdent)
        val enhet = enhetGateway.hentEnheter(listOf(ansattInfo.enhetsnummer)).first()
        Signatur(navn = ansattInfo.navn, enhet = enhet.navn)
    }
}

private fun mapPdfBrev(request: JournalførBehandlerBestillingRequest, signaturer: List<Signatur>): PdfBrev {
    return PdfBrev(
        mottaker = Mottaker(
            navn = request.mottakerNavn,
            ident = request.mottakerHprnr,
            identType = IdentType.HPRNR
        ),
        saksnummer = request.saksnummer,
        dato = request.dato.formaterFullLengde(Språk.NB),
        overskrift = request.tittel,
        tekstbolker = listOf(
            Tekstbolk(
                overskrift = null,
                innhold = request.brevAvsnitt.map {
                    Innhold(
                        overskrift = null,
                        blokker = listOf(
                            Blokk(
                                innhold = listOf(
                                    FormattertTekst(
                                        tekst = it,
                                        formattering = emptyList()
                                    )
                                ),
                                type = BlokkType.AVSNITT
                            )
                        )
                    )
                }
            )
        ),
        signaturer = signaturer
    )
}
