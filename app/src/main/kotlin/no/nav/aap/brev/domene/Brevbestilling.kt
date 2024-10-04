package no.nav.aap.brev.domene

import java.time.LocalDateTime

data class Brevbestilling(
    val referanse: BrevbestillingReferanse,
    val brev: Brev?,
    val opprettet: LocalDateTime,
    val oppdatert: LocalDateTime,
    val behandlingReferanse: BehandlingReferanse,
    val brevtype: Brevtype,
    val språk: Språk,
    val prosesseringStatus: ProsesseringStatus?,
)
