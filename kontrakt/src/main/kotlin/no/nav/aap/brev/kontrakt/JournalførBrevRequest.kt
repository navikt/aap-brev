package no.nav.aap.brev.kontrakt

import java.util.UUID

data class JournalførBrevRequest(
    val fnr: String,
    val navn: String,
    val saksnummer: String,
    val eksternReferanseId: UUID,
    val tittel: String,
    val brevkode: String,
    val brev: PdfBrev
)