package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.Saksnummer
import java.util.UUID

data class JournalpostInfo(
    val fnr: String,
    val navn: String,
    val saksnummer: Saksnummer,
    val eksternReferanseId: UUID,
    val tittel: String,
    val brevkode: String)