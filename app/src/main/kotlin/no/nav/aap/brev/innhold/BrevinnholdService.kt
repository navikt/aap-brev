package no.nav.aap.brev.innhold

import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.komponenter.dbconnect.DBConnection

class BrevinnholdService(
    private val brevinnholdGateway: BrevinnholdGateway,
    private val brevbestillingRepository: BrevbestillingRepository,
) {
    companion object {
        fun konstruer(connection: DBConnection): BrevinnholdService {
            return BrevinnholdService(
                brevinnholdGateway = BrevSanityProxyGateway(),
                brevbestillingRepository = BrevbestillingRepository.konstruer(connection),
            )
        }
    }

    fun hentOgLagreBrev(referanse: BrevbestillingReferanse) {
        val bestilling = brevbestillingRepository.hent(referanse)
        val brev = brevinnholdGateway.hentBrev(bestilling.brevtype, bestilling.språk)

        brevbestillingRepository.oppdaterBrev(bestilling.referanse, brev)
    }

    fun hentOgLagreBrevmal(referanse: BrevbestillingReferanse) {
        val bestilling = brevbestillingRepository.hent(referanse)
        val brevmal = brevinnholdGateway.hentBrevmal(bestilling.brevtype, bestilling.språk)

        // Sjekk at vi klarer å deserialisere til `Brevmal`
        brevmal.tilBrevmal()

        brevbestillingRepository.oppdaterBrevmal(bestilling.id, brevmal)
    }
}