package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.bestilling.Pdf
import no.nav.aap.brev.bestilling.Personinfo
import no.nav.aap.brev.journalføring.OpprettJournalpostRequest.AvsenderMottaker
import no.nav.aap.brev.journalføring.OpprettJournalpostRequest.Bruker
import no.nav.aap.brev.journalføring.OpprettJournalpostRequest.Dokument
import no.nav.aap.brev.journalføring.OpprettJournalpostRequest.Dokument.DokumentVariant
import no.nav.aap.brev.journalføring.OpprettJournalpostRequest.JournalpostType
import no.nav.aap.brev.journalføring.OpprettJournalpostRequest.Sak
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.post
import java.net.URI
import java.util.*

class DokarkivGateway : ArkivGateway {
    private val baseUri = URI.create(requiredConfigForKey("integrasjon.dokarkiv.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.dokarkiv.scope"))
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider
    )

    override fun journalførBrev(
        bestilling: Brevbestilling,
        personinfo: Personinfo,
        pdf: Pdf,
    ) {
        val uri = baseUri.resolve("/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true")
        val request = lagRequest(bestilling, personinfo, pdf)
        val httpRequest = PostRequest(
            body = request,
            additionalHeaders = listOf(
//                Header("Nav-User-Id", navIdent) // TODO vurder om vi skal sette denne
            )
        )
        client.post<OpprettJournalpostRequest, OpprettJournalpostResponse>(uri, httpRequest)
    }

    private fun lagRequest(
        bestilling: Brevbestilling,
        personinfo: Personinfo,
        pdf: Pdf,
    ): OpprettJournalpostRequest {
        val overskrift = requireNotNull(bestilling.brev?.overskrift)
        val brevkode = bestilling.brevtype.name
        return OpprettJournalpostRequest(
            avsenderMottaker = AvsenderMottaker(
                id = personinfo.fnr,
                idType = AvsenderMottaker.IdType.FNR,
            ),
            behandlingstema = null, // TODO
            bruker = Bruker(
                id = personinfo.fnr,
                idType = Bruker.IdType.FNR
            ),
            dokumenter = listOf(
                Dokument(
                    tittel = overskrift,
                    brevkode = brevkode,
                    dokumentVarianter = listOf(
                        DokumentVariant(
                            filtype = "PDFA",
                            fysiskDokument = pdf.bytes,
                            variantformat = "ARKIV",
                        )
                    ),
                )
            ),
            eksternReferanseId = bestilling.referanse.referanse,
            journalfoerendeEnhet = "9999", // TODO
            journalposttype = JournalpostType.UTGAAENDE,
            sak = Sak(
                fagsakId = bestilling.saksnummer.nummer,
                fagsaksystem = "KELVIN",
                sakstype = Sak.Type.FAGSAK
            ),
            tema = "AAP",
            tilleggsopplysninger = emptyList(),
            tittel = overskrift,
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
    val journalpostId: String,
    val journalpostferdigstilt: Boolean,
    val dokumenter: List<DokumentInfoId>
) {
    data class DokumentInfoId(val dokumentInfoId: String)
}
