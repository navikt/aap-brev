package no.nav.aap.brev.journalføring

data class TilknyttVedleggResponse(val feiledeDokumenter: List<FeiledeDokumenter>) {
    data class FeiledeDokumenter(
        val kildeJournalpostId: String,
        val dokumentInfoId: String,
        val arsakKode: String,
    )
}