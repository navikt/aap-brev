package no.nav.aap.brev.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.brev.bestilling.SaksbehandlingPdfGenGateway
import no.nav.aap.brev.bestilling.Saksnummer
import no.nav.aap.brev.journalføring.DokarkivGateway
import no.nav.aap.brev.journalføring.JournalpostInfo
import no.nav.aap.brev.journalføring.JournalpostInfo.MottakerType
import no.nav.aap.brev.kontrakt.BlokkType
import no.nav.aap.brev.kontrakt.EkspederBehandlerBestillingRequest
import no.nav.aap.brev.kontrakt.JournalførBehandlerBestillingRequest
import no.nav.aap.brev.kontrakt.JournalførBehandlerBestillingResponse
import no.nav.aap.brev.kontrakt.PdfBrev
import no.nav.aap.brev.kontrakt.PdfBrev.Blokk
import no.nav.aap.brev.kontrakt.PdfBrev.FormattertTekst
import no.nav.aap.brev.kontrakt.PdfBrev.Innhold
import no.nav.aap.brev.kontrakt.PdfBrev.Mottaker
import no.nav.aap.brev.kontrakt.PdfBrev.Tekstbolk
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.authorizedPost
import tilgang.Operasjon

fun NormalOpenAPIRoute.dokumentinnhentingApi() {

    val dokumentinnhentingAzp = requiredConfigForKey("integrasjon.dokumentinnhenting.azp")

    val authorizationBodyPathConfig = AuthorizationBodyPathConfig(
        operasjon = Operasjon.SAKSBEHANDLE,
        approvedApplications = setOf(dokumentinnhentingAzp),
        applicationsOnly = true
    )

    route("/api/dokumentinnhenting") {
        route("/journalfor-behandler-bestilling") {
            authorizedPost<Unit, JournalførBehandlerBestillingResponse, JournalførBehandlerBestillingRequest>(
                authorizationBodyPathConfig
            ) { _, request ->

                val pdfGateway = SaksbehandlingPdfGenGateway()
                val arkivGateway = DokarkivGateway()

                val pdfBrev = mapPdfBrev(request)
                val pdf = pdfGateway.genererPdf(pdfBrev)
                val journalpostResponse = arkivGateway.journalførBrev(
                    journalpostInfo = JournalpostInfo(
                        brukerFnr = request.brukerFnr,
                        mottakerIdent = request.mottakerHprnr,
                        mottakerNavn = request.mottakerNavn,
                        mottakerType = MottakerType.HPRNR,
                        saksnummer = Saksnummer(request.saksnummer),
                        eksternReferanseId = request.eksternReferanseId,
                        tittel = request.tittel,
                        brevkode = request.brevkode,
                        overstyrInnsynsregel = true,
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
                val arkivGateway = DokarkivGateway()

                arkivGateway.ekspediterJournalpost(request.journalpostId)

                respond("{}", HttpStatusCode.OK)
            }
        }
    }
}

private fun mapPdfBrev(request: JournalførBehandlerBestillingRequest): PdfBrev {
    return PdfBrev(
        mottaker = Mottaker(navn = request.mottakerNavn, ident = request.mottakerHprnr),
        saksnummer = request.saksnummer,
        dato = request.dato,
        overskrift = request.tittel,
        tekstbolker = listOf(
            Tekstbolk(
                overskrift = null,
                innhold = listOf(
                    Innhold(
                        overskrift = null,
                        blokker = listOf(
                            Blokk(
                                innhold = request.brevAvsnitt.map {
                                    FormattertTekst(
                                        tekst = it,
                                        formattering = emptyList()
                                    )
                                },
                                type = BlokkType.AVSNITT
                            )
                        )
                    )
                )
            )
        )
    )
}
