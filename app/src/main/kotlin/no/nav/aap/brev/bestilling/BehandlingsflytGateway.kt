package no.nav.aap.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.BrevbestillingLøsningStatus
import no.nav.aap.brev.kontrakt.Status

class BehandlingsflytGateway : BestillerGateway {
    override fun oppdaterBrevStatus(referanse: BrevbestillingReferanse, status: Status) {
        val brevbestillingLøsningStatus =  when (status) {
            Status.REGISTRERT -> return
            Status.UNDER_ARBEID -> BrevbestillingLøsningStatus.KLAR_FOR_EDITERING
            Status.FERDIGSTILT -> BrevbestillingLøsningStatus.AUTOMATISK_FERDIGSTILT
        }
    }

}