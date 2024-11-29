package no.nav.aap.brev.distribusjon

import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
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

    fun distribuerBrev(brevbestillingReferanse: BrevbestillingReferanse) {
        val brevbestilling = brevbestillingRepository.hent(brevbestillingReferanse)
        val distribusjonBestillingId = distribusjonGateway.distribuerJournalpost(
            checkNotNull(brevbestilling.journalpostId),
            brevbestilling.brevtype
        )
        brevbestillingRepository.lagreDistribusjonBestilling(brevbestilling.id, distribusjonBestillingId)
    }
}
