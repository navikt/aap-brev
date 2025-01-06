package no.nav.aap.brev.arkivoppslag

import no.nav.aap.brev.journalføring.DokumentInfoId
import no.nav.aap.brev.journalføring.JournalpostId

data class Journalpost(
    val journalpostId: JournalpostId,
    val journalstatus: String,
    val brukerHarTilgang: Boolean,
    val sak: Sak,
    val dokumenter: List<Dokument>
) {
    data class Sak(
        val fagsakId: String,
        val fagsaksystem: String,
        val sakstype: String,
        val tema: String,
    )

    data class Dokument(
        val dokumentInfoId: DokumentInfoId,
        val dokumentvarianter: List<Variant>
    ) {
        data class Variant(val brukerHarTilgang: Boolean)
    }
}
