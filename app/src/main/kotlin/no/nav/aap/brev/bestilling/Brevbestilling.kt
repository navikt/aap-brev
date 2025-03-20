package no.nav.aap.brev.bestilling

import no.nav.aap.brev.distribusjon.DistribusjonBestillingId
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Signatur
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.prosessering.ProsesseringStatus
import java.time.LocalDateTime

data class Brevbestilling(
    val id: BrevbestillingId,
    val saksnummer: Saksnummer,
    val referanse: BrevbestillingReferanse,
    val brev: Brev?,
    val brukerIdent: String?,
    val signaturer: List<Signatur>,
    val opprettet: LocalDateTime,
    val oppdatert: LocalDateTime,
    val behandlingReferanse: BehandlingReferanse,
    val unikReferanse: UnikReferanse,
    val brevtype: Brevtype,
    val språk: Språk,
    val prosesseringStatus: ProsesseringStatus?,
    val journalpostId: JournalpostId?,
    val journalpostFerdigstilt: Boolean?,
    val distribusjonBestillingId: DistribusjonBestillingId?,
    val vedlegg: Set<Vedlegg>,
)
