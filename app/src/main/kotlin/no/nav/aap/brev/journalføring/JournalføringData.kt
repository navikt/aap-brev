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
        require(mottakerNavn != null || mottakerType == MottakerType.FNR) {
            "MottakerNavn må være satt dersom MottakerType ikke er FNR."
        }
        require(
            (mottakerType != null && mottakerIdent != null)
                    || (mottakerType == null && mottakerIdent == null)
        ) {
            "MottakerType og MottakerIdent må være satt sammen, eller begge må være null"
        }
    }
}