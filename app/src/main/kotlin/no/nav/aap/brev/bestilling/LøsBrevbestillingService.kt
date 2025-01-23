package no.nav.aap.brev.bestilling

import no.nav.aap.brev.innhold.erFullstendig
import no.nav.aap.brev.innhold.harFaktagrunnlag
import no.nav.aap.brev.innhold.kanRedigeres
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.komponenter.dbconnect.DBConnection

class LøsBrevbestillingService(
    private val brevbestillingRepository: BrevbestillingRepository,
    private val bestillerGateway: BestillerGateway,
) {

    companion object {
        fun konstruer(connection: DBConnection): LøsBrevbestillingService {
            return LøsBrevbestillingService(
                bestillerGateway = BehandlingsflytGateway(),
                brevbestillingRepository = BrevbestillingRepositoryImpl(connection),
            )
        }
    }

    fun løsBestilling(referanse: BrevbestillingReferanse): Status {
        val bestilling = brevbestillingRepository.hent(referanse)
        checkNotNull(bestilling.brev)

        val skalFerigstilles = skalFerdigstilles(bestilling.brev)
        val status = if (skalFerigstilles) {
            Status.FERDIGSTILT
        } else {
            Status.UNDER_ARBEID
        }
        bestillerGateway.oppdaterBrevStatus(
            bestilling,
            status
        )
        return status
    }

    private fun skalFerdigstilles(brev: Brev): Boolean {
        return !brev.harFaktagrunnlag() && brev.erFullstendig() && !brev.kanRedigeres()
    }
}