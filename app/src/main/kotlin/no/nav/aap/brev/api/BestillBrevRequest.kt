package no.nav.aap.brev.api

import no.nav.aap.brev.domene.BehandlingReferanse
import no.nav.aap.brev.domene.Brevtype
import no.nav.aap.brev.domene.Språk

data class BestillBrevRequest(
    val behandlingReferanse: BehandlingReferanse,
    val brevtype: Brevtype,
    val sprak: Språk,
)
