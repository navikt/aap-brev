package no.nav.aap.brev.innhold

import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.Faktagrunnlag
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.FaktagrunnlagType
import no.nav.aap.brev.bestilling.BehandlingReferanse

interface HentFagtagrunnlagGateway {
    fun hent(
        behandlingReferanse: BehandlingReferanse,
        faktagrunnlag: Set<FaktagrunnlagType>
    ): Set<Faktagrunnlag>
}