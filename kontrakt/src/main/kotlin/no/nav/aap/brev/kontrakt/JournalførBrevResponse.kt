package no.nav.aap.brev.kontrakt

data class JournalførBrevResponse(
    val journalpostId: String,
    val journalpostferdigstilt: Boolean,
    val dokumenter: List<String>,
)
