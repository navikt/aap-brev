package no.nav.aap.brev.kontrakt

import java.time.LocalDate
import java.util.UUID

data class JournalførBehandlerBestillingRequest(
    val brukerFnr: String,
    val saksnummer: String,
    val mottakerHprnr: String,
    val mottakerNavn: String,
    val eksternReferanseId: UUID,
    val brevkode: String,
    val tittel: String,
    val brevAvsnitt: List<String>,
    val dato: LocalDate,
    val bestillerNavIdent: String,
    val overstyrInnsynsregel: Boolean = true
)