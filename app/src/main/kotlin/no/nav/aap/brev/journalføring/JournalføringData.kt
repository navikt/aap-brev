package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.Saksnummer

data class JournalføringData(
    val brukerFnr: String,
    val mottakerIdent: String?,
    val mottakerNavn: String?,
    val mottakerType: MottakerType?,
    val saksnummer: Saksnummer,
    val eksternReferanseId: String,
    val tittelJournalpost: String,
    val tittelBrev: String,
    val brevkode: String,
    val overstyrInnsynsregel: Boolean,
) {
    enum class MottakerType {
        FNR, HPRNR, ORGNR, UTL_ORG
    }

    init {
        require(mottakerNavn != null || mottakerType == MottakerType.FNR || mottakerType == MottakerType.ORGNR) {
            "mottakerNavn må være satt dersom mottakerType ikke er FNR eller ORGNR."
        }
        require(
            (mottakerType != null && mottakerIdent != null)
                    || (mottakerType == null && mottakerIdent == null)
        ) {
            "mottakerType og mottakerIdent må være satt sammen, eller begge må være null."
        }
    }
}