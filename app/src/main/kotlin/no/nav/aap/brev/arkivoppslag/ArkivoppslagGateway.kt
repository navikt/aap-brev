package no.nav.aap.brev.arkivoppslag

import no.nav.aap.brev.journalføring.JournalpostId

interface ArkivoppslagGateway {
    fun hentSaksinfo(journalpostId: JournalpostId): JournalpostSaksinfo
}
