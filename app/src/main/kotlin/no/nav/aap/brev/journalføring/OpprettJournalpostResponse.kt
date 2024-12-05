package no.nav.aap.brev.journalføring

data class OpprettJournalpostResponse(
    val journalpostId: JournalpostId,
    val journalpostferdigstilt: Boolean,
    val dokumenter: List<DokumentInfoId>
) {
    data class DokumentInfoId(val dokumentInfoId: String)
}