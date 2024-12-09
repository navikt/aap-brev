package no.nav.aap.brev.journalføring

data class TilknyttVedleggRequest(val dokument: List<DokumentVedlegg>) {
    data class DokumentVedlegg(val kildeJournalpostId: String, val dokumentInfoId: String)
}
