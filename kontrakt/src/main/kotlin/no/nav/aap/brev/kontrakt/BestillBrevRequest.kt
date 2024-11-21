package no.nav.aap.brev.kontrakt

import java.util.*

data class BestillBrevRequest(
    val saksnummer: String,
    val behandlingReferanse: UUID,
    val brevtype: Brevtype,
    val sprak: Språk,
)
