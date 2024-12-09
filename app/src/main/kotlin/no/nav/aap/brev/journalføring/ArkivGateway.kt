package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.Pdf
import no.nav.aap.brev.kontrakt.Vedlegg

interface ArkivGateway {
    fun journalførBrev(
        journalpostInfo: JournalpostInfo,
        pdf: Pdf,
        forsøkFerdigstill: Boolean,
    ): OpprettJournalpostResponse

    fun ferdigstillJournalpost(journalpostId: JournalpostId)

    fun tilknyttVedlegg(journalpostId: JournalpostId, vedlegg: Set<Vedlegg>)

    fun ekspediterJournalpost(journalpostId: String)
}

