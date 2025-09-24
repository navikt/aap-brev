package no.nav.aap.brev.bestilling

import Brevdata
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevmal
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.prosessering.ProsesseringStatus
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.time.LocalDateTime

data class Brevbestilling(
    val id: BrevbestillingId,
    val saksnummer: Saksnummer,
    val referanse: BrevbestillingReferanse,
    val brev: Brev?,
    val brevmal: ObjectNode?,
    val brevdata: Brevdata?,
    val brukerIdent: String?,
    val signaturer: List<SorterbarSignatur>,
    val opprettet: LocalDateTime,
    val oppdatert: LocalDateTime,
    val behandlingReferanse: BehandlingReferanse,
    val unikReferanse: UnikReferanse,
    val brevtype: Brevtype,
    val språk: Språk,
    val status: Status?,
    val prosesseringStatus: ProsesseringStatus?,
    val vedlegg: Set<Vedlegg>,
) {
    fun deserialisertBrevmal(): Brevmal? {
        brevmal ?: return null
        return DefaultJsonMapper.objectMapper().treeToValue(brevmal, Brevmal::class.java)
    }
}
