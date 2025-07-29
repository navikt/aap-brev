package no.nav.aap.brev.distribusjon

import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.OpprettetJournalpost
import no.nav.aap.komponenter.dbconnect.DBConnection

class DistribusjonService(
    private val brevbestillingRepository: BrevbestillingRepository,
    private val distribusjonGateway: DistribusjonGateway,
) {

    companion object {
        fun konstruer(connection: DBConnection): DistribusjonService {
            return DistribusjonService(BrevbestillingRepositoryImpl(connection), DokdistfordelingGateway())
        }
    }

    fun distribuerBrev(journalpost: OpprettetJournalpost) {
        check(journalpost.distribusjonBestillingId == null) {
            "Brevet er allerede distribuert."
        }
        check(journalpost.ferdigstilt) {
            "Kan ikke distribuere en bestilling som ikke er journalført."
        }
        val brevbestilling = brevbestillingRepository.hent(journalpost.brevbestillingId)

        val distribusjonBestillingId = distribusjonGateway.distribuerJournalpost(
            journalpost.journalpostId,
            brevbestilling.brevtype,
            journalpost.mottaker
        )
        brevbestillingRepository.lagreDistribusjonBestilling(journalpost.journalpostId, distribusjonBestillingId)
    }
}
