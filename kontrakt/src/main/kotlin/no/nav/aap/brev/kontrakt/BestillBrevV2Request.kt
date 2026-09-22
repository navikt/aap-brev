package no.nav.aap.brev.kontrakt

import java.util.*

public data class BestillBrevV2Request(
    val saksnummer: String,
    val brukerIdent: String,
    val behandlingReferanse: UUID,
    val brevtype: Brevtype,
    val unikReferanse: String,
    val sprak: Språk,
    val faktagrunnlag: Set<Faktagrunnlag>,
    val ferdigstillAutomatisk: Boolean,
    val signaturer: List<SignaturGrunnlag> = emptyList(),
    val vedlegg: Set<Vedlegg> = emptySet(),
)
