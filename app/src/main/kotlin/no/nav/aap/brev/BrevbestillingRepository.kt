package no.nav.aap.brev

import no.nav.aap.brev.domene.BehandlingReferanse
import no.nav.aap.brev.domene.Brev
import no.nav.aap.brev.domene.Brevbestilling
import no.nav.aap.brev.domene.BrevbestillingReferanse
import no.nav.aap.brev.domene.Brevtype
import no.nav.aap.brev.domene.Språk

interface BrevbestillingRepository {
    fun opprettBestilling(
        behandlingReferanse: BehandlingReferanse,
        brevtype: Brevtype,
        sprak: Språk,
        brev: Brev,
    ): BrevbestillingReferanse

    fun hent(referanse: BrevbestillingReferanse): Brevbestilling

    fun oppdaterBrev(
        referanse: BrevbestillingReferanse,
        brev: Brev,
    )
}