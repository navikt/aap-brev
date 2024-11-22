package no.nav.aap.brev.journalføring

import java.util.*

class DokarkivGateway : ArkivGateway {
    override fun journalførBrev() {

    }
}

data class OpprettJournalpostRequest(
    val avsenderMottaker: AvsenderMottaker,
    val behandlingstema: String,
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

data class OpprettetJournalpostResponse(
    val journalpostId: String,
    val journalpostferdigstilt: Boolean,
    val dokumenter: List<DokumentInfoId>
) {
    data class DokumentInfoId(val dokumentInfoId: String)
}
