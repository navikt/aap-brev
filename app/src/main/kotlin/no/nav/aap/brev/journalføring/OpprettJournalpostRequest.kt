package no.nav.aap.brev.journalføring

data class OpprettJournalpostRequest(
    val avsenderMottaker: AvsenderMottaker,
    val behandlingstema: String?,
    val bruker: Bruker,
    val dokumenter: List<Dokument>,
    val eksternReferanseId: String,
    val journalfoerendeEnhet: String,
    val journalposttype: JournalpostType,
    val sak: Sak,
    val tema: String,
    val tilleggsopplysninger: List<Tilleggsopplysning>,
    val tittel: String,
    val overstyrInnsynsregler: Innsynsregl?,
) {
    data class AvsenderMottaker(
        val id: String? = null,
        val idType: IdType? = null,
        val navn: String? = null
    ) {
        init {
            require(navn != null || idType == IdType.FNR || idType == IdType.ORGNR) {
                "navn må være satt dersom idType ikke er FNR eller ORGNR."
            }
            require(
                (idType != null && id != null)
                        || (idType == null && id == null)
            ) {
                "idType og id må være satt sammen, eller begge må være null"
            }
        }

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
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as DokumentVariant

                if (filtype != other.filtype) return false
                if (!fysiskDokument.contentEquals(other.fysiskDokument)) return false
                if (variantformat != other.variantformat) return false

                return true
            }

            override fun hashCode(): Int {
                var result = filtype.hashCode()
                result = 31 * result + fysiskDokument.contentHashCode()
                result = 31 * result + variantformat.hashCode()
                return result
            }
        }
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

    enum class Innsynsregl {
        VISES_MASKINELT_GODKJENT,
        VISES_MANUELT_GODKJENT,
    }
}
