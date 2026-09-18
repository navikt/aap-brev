package no.nav.aap.brev.kontrakt

public data class JournalførBehandlerBestillingResponse(
    val journalpostId: String,
    val journalpostFerdigstilt: Boolean,
    val dokumenter: List<String>,
)
