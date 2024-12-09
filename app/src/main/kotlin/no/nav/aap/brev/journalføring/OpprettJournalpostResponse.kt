package no.nav.aap.brev.journalføring

data class OpprettJournalpostResponse(
    val journalpostId: JournalpostId,
    val journalpostferdigstilt: Boolean,
    val dokumenter: List<Dokument>
) {
    data class Dokument(val dokumentInfoId: String)
}