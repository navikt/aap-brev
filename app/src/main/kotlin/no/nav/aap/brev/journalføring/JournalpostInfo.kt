package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.Saksnummer
import java.util.UUID

data class JournalpostInfo(
    val brukerFnr: String,
    val mottakerIdent: String,
    val mottakerType: MottakerType,
    val saksnummer: Saksnummer,
    val eksternReferanseId: UUID,
    val tittel: String,
    val brevkode: String
) {
    enum class MottakerType {
        FNR, HPRNR
    }
}