package no.nav.aap.brev

import no.nav.aap.brev.domene.BehandlingReferanse
import no.nav.aap.brev.domene.Brev
import no.nav.aap.brev.domene.Brevbestilling
import no.nav.aap.brev.domene.BrevbestillingReferanse
import no.nav.aap.brev.domene.Brevtype
import no.nav.aap.brev.domene.Språk
import no.nav.aap.brev.domene.ProsesseringStatus

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
