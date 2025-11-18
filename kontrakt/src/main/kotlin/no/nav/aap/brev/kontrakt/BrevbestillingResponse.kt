package no.nav.aap.brev.kontrakt

import java.time.LocalDateTime
import java.util.UUID

data class BrevbestillingResponse(
    val referanse: UUID,
    val brev: Brev?,
    val brevmal: String?,
    val brevdata: BrevdataDto?,
    val opprettet: LocalDateTime,
    val oppdatert: LocalDateTime,
    val behandlingReferanse: UUID,
    val brevtype: Brevtype,
    val språk: Språk,
    val status: Status,
)
