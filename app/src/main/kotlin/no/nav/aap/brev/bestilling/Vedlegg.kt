package no.nav.aap.brev.bestilling

import no.nav.aap.brev.journalføring.DokumentInfoId
import no.nav.aap.brev.journalføring.JournalpostId

data class Vedlegg(val journalpostId: JournalpostId, val dokumentInfoId: DokumentInfoId)
