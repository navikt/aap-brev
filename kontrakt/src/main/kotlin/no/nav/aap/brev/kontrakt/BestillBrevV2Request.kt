package no.nav.aap.brev.kontrakt

import java.util.*

data class BestillBrevV2Request(
    val saksnummer: String,
    val brukerIdent: String,
    val behandlingReferanse: UUID,
    val brevtype: Brevtype,
    val unikReferanse: String,
    val sprak: Språk,
    val faktagrunnlag: Set<Faktagrunnlag>,
    val ferdigstillAutomatisk: Boolean,
    val vedlegg: Set<Vedlegg> = emptySet(),
)
