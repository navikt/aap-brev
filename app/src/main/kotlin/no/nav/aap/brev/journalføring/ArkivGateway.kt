package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.Pdf

interface ArkivGateway {
    fun journalførBrev(journalpostInfo: JournalpostInfo,
                       pdf: Pdf): OpprettJournalpostResponse
}

