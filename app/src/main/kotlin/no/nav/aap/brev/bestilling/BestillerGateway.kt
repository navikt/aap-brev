package no.nav.aap.brev.bestilling

import no.nav.aap.brev.kontrakt.Status

interface BestillerGateway {
    fun oppdaterBrevStatus(brevbestilling: Brevbestilling, status: Status)
}