package no.nav.aap.brev.distribusjon

import no.nav.aap.brev.bestilling.Mottaker
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.kontrakt.Brevtype

interface DistribusjonGateway {
    fun distribuerJournalpost(
        journalpostId: JournalpostId,
        brevtype: Brevtype,
        mottaker: Mottaker
    ): DistribusjonBestillingId
}
