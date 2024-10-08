package no.nav.aap.brev.kontrakt

import java.util.UUID

data class BestillBrevRequest(
    val behandlingReferanse: UUID,
    val brevtype: Brevtype,
    val sprak: Språk,
)
