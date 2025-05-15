package no.nav.aap.brev.bestilling

import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.komponenter.dbconnect.DBConnection

class FerdigstillService(private val brevbestillingRepository: BrevbestillingRepository) {
    companion object {
        fun konstruer(connection: DBConnection): FerdigstillService {
            return FerdigstillService(
                brevbestillingRepository = BrevbestillingRepositoryImpl(connection),
            )
        }
    }

    fun validerFerdigstilling(referanse: BrevbestillingReferanse) {
        val bestilling = brevbestillingRepository.hent(referanse)
        check(bestilling.status == Status.FERDIGSTILT || bestilling.status == null) { // TODO midlertidlig tillat null
            "Kan ikke fortsette ferdigstilling av bestilling med referanse: ${referanse.referanse} i status ${bestilling.status}"
        }
    }
}