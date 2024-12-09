package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.Pdf
import no.nav.aap.brev.journalføring.OpprettJournalpostRequest.AvsenderMottaker
import no.nav.aap.brev.journalføring.OpprettJournalpostRequest.Bruker
import no.nav.aap.brev.journalføring.OpprettJournalpostRequest.Dokument
import no.nav.aap.brev.journalføring.OpprettJournalpostRequest.Dokument.DokumentVariant
import no.nav.aap.brev.journalføring.OpprettJournalpostRequest.JournalpostType
import no.nav.aap.brev.journalføring.OpprettJournalpostRequest.Sak
import no.nav.aap.brev.kontrakt.Vedlegg
import no.nav.aap.brev.util.HåndterConflictResponseHandler
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.patch
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.put
import no.nav.aap.komponenter.httpklient.httpclient.request.PatchRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PutRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import org.slf4j.LoggerFactory
import java.net.URI

private const val MASKINELL_JOURNALFØRING_ENHET = "9999"

class DokarkivGateway : ArkivGateway {
    private val log = LoggerFactory.getLogger(DokarkivGateway::class.java)


    private val baseUri = URI.create(requiredConfigForKey("integrasjon.dokarkiv.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.dokarkiv.scope"))
    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = HåndterConflictResponseHandler()
    )

    override fun journalførBrev(
        journalpostInfo: JournalpostInfo,
        pdf: Pdf,
        forsøkFerdigstill: Boolean,
    ): OpprettJournalpostResponse {
        val uri = baseUri.resolve("/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=$forsøkFerdigstill")
        val request = lagRequest(journalpostInfo, pdf)
        val httpRequest = PostRequest(
            body = request,
        )
        val response =
            checkNotNull(client.post<OpprettJournalpostRequest, OpprettJournalpostResponse>(uri, httpRequest))

        if (!response.journalpostferdigstilt) {
            log.error("Journalpost ble ikke ferdigstilt. Journalpost må ferdigstilles for å kunne bli distribuert.")
        }

        return response
    }

    override fun ferdigstillJournalpost(journalpostId: JournalpostId) {
        val uri = baseUri.resolve("/rest/journalpostapi/v1/journalpost/$journalpostId/ferdigstill")
        val request = FerdigstillJournalpostRequest(journalfoerendeEnhet = MASKINELL_JOURNALFØRING_ENHET)
        val httpRequest = PatchRequest(
            body = request,
        )
        client.patch<FerdigstillJournalpostRequest, Unit>(uri, httpRequest)
    }

    override fun tilknyttVedlegg(
        journalpostId: JournalpostId,
        vedlegg: Set<Vedlegg>
    ) {
        val uri = baseUri.resolve("/rest/journalpostapi/v1/journalpost/$journalpostId/tilknyttVedlegg")
        val request = TilknyttVedleggRequest(dokument = vedlegg.map {
            TilknyttVedleggRequest.DokumentVedlegg(it.journalpostId, it.dokumentInfoId)
        })
        val httpRequest = PutRequest(
            body = request,
        )
        val response = checkNotNull(client.put<TilknyttVedleggRequest, TilknyttVedleggResponse>(uri, httpRequest))
        if (response.feiledeDokumenter.isNotEmpty()) {
            response.feiledeDokumenter.forEach {
                log.error("Kunne ikke tilknytte vedlegg, kildeJournalpostId: ${it.kildeJournalpostId}, dokumentInfoId: ${it.dokumentInfoId}, arsakKode: ${it.arsakKode}.")
            }
            throw IllegalStateException("Kunne ikke tilknytte ${response.feiledeDokumenter.size} vedlegg, journalpostId: ${journalpostId.id}.")
        }
    }

    override fun ekspediterJournalpost(journalpostId: String) {
        val uri = baseUri.resolve("/rest/journalpostapi/v1/journalpost/$journalpostId/oppdaterDistribusjonsinfo")
        val request = EkspediterJournalpostRequest(settStatusEkspedert = true)

        val httpRequest = PatchRequest(
            body = request
        )

        client.patch<EkspediterJournalpostRequest, Unit>(uri, httpRequest)
    }

    private fun lagRequest(
        journalpostInfo: JournalpostInfo,
        pdf: Pdf,
    ): OpprettJournalpostRequest {
        return OpprettJournalpostRequest(
            avsenderMottaker = AvsenderMottaker(
                id = journalpostInfo.mottakerIdent,
                idType = when (journalpostInfo.mottakerType) {
                    JournalpostInfo.MottakerType.FNR -> AvsenderMottaker.IdType.FNR
                    JournalpostInfo.MottakerType.HPRNR -> AvsenderMottaker.IdType.HPRNR
                },
                navn = journalpostInfo.mottakerNavn
            ),
            behandlingstema = null,
            bruker = Bruker(
                id = journalpostInfo.brukerFnr,
                idType = Bruker.IdType.FNR
            ),
            dokumenter = listOf(
                Dokument(
                    tittel = journalpostInfo.tittel,
                    brevkode = journalpostInfo.brevkode,
                    dokumentVarianter = listOf(
                        DokumentVariant(
                            filtype = "PDFA",
                            fysiskDokument = pdf.bytes,
                            variantformat = "ARKIV",
                        )
                    ),
                )
            ),
            eksternReferanseId = journalpostInfo.eksternReferanseId,
            journalfoerendeEnhet = MASKINELL_JOURNALFØRING_ENHET,
            journalposttype = JournalpostType.UTGAAENDE,
            sak = Sak(
                fagsakId = journalpostInfo.saksnummer.nummer,
                fagsaksystem = "KELVIN",
                sakstype = Sak.Type.FAGSAK
            ),
            tema = "AAP",
            tilleggsopplysninger = emptyList(),
            tittel = journalpostInfo.tittel,
        )
    }
}
