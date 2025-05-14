package no.nav.aap.brev.innhold

import no.nav.aap.brev.bestilling.BehandlingReferanse
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.kontrakt.FaktagrunnlagType

interface HentFagtagrunnlagGateway {
    fun hent(
        behandlingReferanse: BehandlingReferanse,
        faktagrunnlag: Set<FaktagrunnlagType>
    ): Set<Faktagrunnlag>
}