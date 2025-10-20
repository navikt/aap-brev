package no.nav.aap.brev.innhold

import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection

class BrevinnholdService(
    private val brevinnholdGateway: BrevinnholdGateway,
    private val brevbestillingRepository: BrevbestillingRepository,
) {
    companion object {
        fun konstruer(connection: DBConnection): BrevinnholdService {
            return BrevinnholdService(
                brevinnholdGateway = BrevSanityProxyGateway(),
                brevbestillingRepository = BrevbestillingRepositoryImpl(connection),
            )
        }
    }

    fun hentOgLagreBrev(referanse: BrevbestillingReferanse) {
        val bestilling = brevbestillingRepository.hent(referanse)
        val brev = brevinnholdGateway.hentBrev(bestilling.brevtype, bestilling.språk)

        brevbestillingRepository.oppdaterBrev(bestilling.referanse, brev)
    }
}