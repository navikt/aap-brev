package no.nav.aap.brev.distribusjon

import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.JournalpostRepository
import no.nav.aap.brev.bestilling.JournalpostRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection

class DistribusjonService(
    private val brevbestillingRepository: BrevbestillingRepository,
    private val opprettJournalpostRepository: JournalpostRepository,
    private val distribusjonGateway: DistribusjonGateway,
) {

    companion object {
        fun konstruer(connection: DBConnection): DistribusjonService {
            return DistribusjonService(
                BrevbestillingRepositoryImpl(connection),
                JournalpostRepositoryImpl(connection),
                DokdistfordelingGateway()
            )
        }
    }

    fun distribuerBrev(brevbestillingReferanse: BrevbestillingReferanse) {
        val brevbestilling = brevbestillingRepository.hent(brevbestillingReferanse)

        checkNotNull(brevbestilling.journalpostId) {
            "Kan ikke distribuere en bestilling som ikke er journalført."
        }

        check(brevbestilling.distribusjonBestillingId == null) {
            "Brevet er allerede distribuert."
        }

        val distribusjonBestillingId = distribusjonGateway.distribuerJournalpost(
            brevbestilling.journalpostId,
            brevbestilling.brevtype
        )
        brevbestillingRepository.lagreDistribusjonBestilling(brevbestilling.id, distribusjonBestillingId)
        opprettJournalpostRepository.lagreDistribusjonBestilling(brevbestilling.journalpostId, distribusjonBestillingId)
    }
}
