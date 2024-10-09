package no.nav.aap.brev.bestilling

import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.prosessering.ProsesseringStatus
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
