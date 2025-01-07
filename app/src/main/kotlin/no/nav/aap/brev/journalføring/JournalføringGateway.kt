package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.Pdf
import no.nav.aap.brev.bestilling.Vedlegg

interface JournalføringGateway {
    fun journalførBrev(
        journalføringData: JournalføringData,
        pdf: Pdf,
        forsøkFerdigstill: Boolean,
    ): OpprettJournalpostResponse

    fun ferdigstillJournalpost(journalpostId: JournalpostId)

    fun tilknyttVedlegg(journalpostId: JournalpostId, vedlegg: Set<Vedlegg>)

    fun ekspediterJournalpost(journalpostId: String, utsendingskanal: String)
}

