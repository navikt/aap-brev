package no.nav.aap.brev.bestilling

import no.nav.aap.brev.domene.BehandlingReferanse
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.prosessering.ProsesseringStatus
import no.nav.aap.brev.kontrakt.Brev

interface BrevbestillingRepository {
    fun opprettBestilling(
        behandlingReferanse: BehandlingReferanse,
        brevtype: Brevtype,
        språk: Språk,
    ): BrevbestillingReferanse

    fun hent(referanse: BrevbestillingReferanse): Brevbestilling

    fun oppdaterBrev(
        referanse: BrevbestillingReferanse,
        brev: Brev,
    )

    fun oppdaterProsesseringStatus(
        referanse: BrevbestillingReferanse,
        prosesseringStatus: ProsesseringStatus,
    )
}
