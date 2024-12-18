package no.nav.aap.brev.arkivoppslag

data class SafJournalpostData(val journalpost: Journalpost?) {
    data class Journalpost(
        val sak: Sak?,
    ) {
        data class Sak(
            val fagsakId: String?,
            val fagsaksystem: String?,
            val sakstype: String?,
            val tema: String?,
        )
    }
}
