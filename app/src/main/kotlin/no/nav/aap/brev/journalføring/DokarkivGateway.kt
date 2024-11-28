package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.Pdf
import no.nav.aap.brev.journalføring.OpprettJournalpostRequest.AvsenderMottaker
import no.nav.aap.brev.journalføring.OpprettJournalpostRequest.Bruker
import no.nav.aap.brev.journalføring.OpprettJournalpostRequest.Dokument
import no.nav.aap.brev.journalføring.OpprettJournalpostRequest.Dokument.DokumentVariant
import no.nav.aap.brev.journalføring.OpprettJournalpostRequest.JournalpostType
import no.nav.aap.brev.journalføring.OpprettJournalpostRequest.Sak
import no.nav.aap.brev.util.HåndterConflictResponseHandler
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.*

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
    ): JournalpostId {
        val uri = baseUri.resolve("/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true")
        val request = lagRequest(journalpostInfo, pdf)
        val httpRequest = PostRequest(
            body = request,
            additionalHeaders = listOf(
//                Header("Nav-User-Id", navIdent) // TODO vurder om vi skal sette denne
            )
        )
        val response = checkNotNull(client.post<OpprettJournalpostRequest, OpprettJournalpostResponse>(uri, httpRequest))

        if (!response.journalpostferdigstilt) {
            log.error("Journalpost ble ikke ferdigstilt. Journalpost må ferdigstilles for å kunne bli distribuert.")
        }

        return response.journalpostId
    }

    private fun lagRequest(
        journalpostInfo: JournalpostInfo,
        pdf: Pdf,
    ): OpprettJournalpostRequest {
        return OpprettJournalpostRequest(
            avsenderMottaker = AvsenderMottaker(
                id = journalpostInfo.fnr,
                idType = AvsenderMottaker.IdType.FNR,
            ),
            behandlingstema = null,
            bruker = Bruker(
                id = journalpostInfo.fnr,
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
            journalfoerendeEnhet = "9999", // TODO
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

data class OpprettJournalpostRequest(
    val avsenderMottaker: AvsenderMottaker,
    val behandlingstema: String?,
    val bruker: Bruker,
    val dokumenter: List<Dokument>,
    val eksternReferanseId: UUID,
    val journalfoerendeEnhet: String,
    val journalposttype: JournalpostType,
    val sak: Sak,
    val tema: String,
    val tilleggsopplysninger: List<Tilleggsopplysning>,
    val tittel: String,
) {
    data class AvsenderMottaker(
        val id: String,
        val idType: IdType,
        val navn: String? = null
    ) {
        enum class IdType {
            FNR,
            ORGNR,
            HPRNR,
            UTL_ORG,
        }
    }

    data class Dokument(
        val tittel: String,
        val brevkode: String,
        val dokumentVarianter: List<DokumentVariant>
    ) {
        data class DokumentVariant(
            val filtype: String,
            val fysiskDokument: ByteArray,
            val variantformat: String
        )
    }

    enum class JournalpostType {
        INNGAAENDE,
        UTGAAENDE,
        NOTAT,
    }

    data class Bruker(
        val id: String,
        val idType: IdType
    ) {
        enum class IdType {
            FNR,
            ORGNR,
            AKTOERID,
        }
    }

    data class Sak(
        val fagsakId: String,
        val fagsaksystem: String,
        val sakstype: Type
    ) {
        enum class Type {
            FAGSAK,
            GENERELL_SAK,
        }
    }

    data class Tilleggsopplysning(
        val nokkel: String,
        val verdi: String
    )
}

data class OpprettJournalpostResponse(
    val journalpostId: JournalpostId,
    val journalpostferdigstilt: Boolean,
    val dokumenter: List<DokumentInfoId>
) {
    data class DokumentInfoId(val dokumentInfoId: String)
}
