package no.nav.aap.brev.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.brev.bestilling.SaksbehandlingPdfGenGateway
import no.nav.aap.brev.bestilling.Saksnummer
import no.nav.aap.brev.journalføring.DokarkivGateway
import no.nav.aap.brev.journalføring.JournalføringData
import no.nav.aap.brev.journalføring.JournalføringData.MottakerType
import no.nav.aap.brev.kontrakt.BlokkType
import no.nav.aap.brev.kontrakt.EkspederBehandlerBestillingRequest
import no.nav.aap.brev.kontrakt.JournalførBehandlerBestillingRequest
import no.nav.aap.brev.kontrakt.JournalførBehandlerBestillingResponse
import no.nav.aap.brev.bestilling.PdfBrev
import no.nav.aap.brev.bestilling.PdfBrev.Blokk
import no.nav.aap.brev.bestilling.PdfBrev.FormattertTekst
import no.nav.aap.brev.bestilling.PdfBrev.Innhold
import no.nav.aap.brev.bestilling.PdfBrev.Mottaker
import no.nav.aap.brev.bestilling.PdfBrev.Mottaker.IdentType
import no.nav.aap.brev.bestilling.PdfBrev.Tekstbolk
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.authorizedPost
import tilgang.Operasjon

fun NormalOpenAPIRoute.dokumentinnhentingApi() {

    val dokumentinnhentingAzp = requiredConfigForKey("integrasjon.dokumentinnhenting.azp")
    val azureTokenGeneratorAzp = requiredConfigForKey("integrasjon.azure_token_generator.azp")
    // TODO erstatt med custom role når det er på plass i tilgang-plugin
    val approvedApplications = if (Miljø.er() == MiljøKode.DEV) {
        setOf(dokumentinnhentingAzp, azureTokenGeneratorAzp)
    } else {
        setOf(dokumentinnhentingAzp)
    }
    val authorizationBodyPathConfig = AuthorizationBodyPathConfig(
        operasjon = Operasjon.SAKSBEHANDLE,
        approvedApplications = approvedApplications,
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
                    journalføringData = JournalføringData(
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

                arkivGateway.ekspediterJournalpost(request.journalpostId, "HELSENETTET")

                respond("{}", HttpStatusCode.OK)
            }
        }
    }
}

private fun mapPdfBrev(request: JournalførBehandlerBestillingRequest): PdfBrev {
    return PdfBrev(
        mottaker = Mottaker(
            navn = request.mottakerNavn,
            ident = request.mottakerHprnr,
            identType = IdentType.HPRNR
        ),
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
