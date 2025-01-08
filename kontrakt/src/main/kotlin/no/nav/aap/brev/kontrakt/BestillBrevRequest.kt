package no.nav.aap.brev.kontrakt

import java.util.*

data class BestillBrevRequest(
    val saksnummer: String,
    val behandlingReferanse: UUID,
    val brevtype: Brevtype,
    val unikReferanse: String,
    val sprak: Språk,
    val vedlegg: Set<Vedlegg> = emptySet(),
)

data class Vedlegg(val journalpostId: String, val dokumentInfoId: String)
