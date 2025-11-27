package no.nav.aap.brev.bestilling

import no.nav.aap.brev.distribusjon.DistribusjonBestillingId
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.prosessering.ProsesseringStatus
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.SignaturGrunnlag
import no.nav.aap.brev.kontrakt.Status

interface BrevbestillingRepository {
    fun opprettBestilling(
        saksnummer: Saksnummer,
        brukerIdent: String?,
        behandlingReferanse: BehandlingReferanse,
        unikReferanse: UnikReferanse,
        brevtype: Brevtype,
        språk: Språk,
        vedlegg: Set<Vedlegg>,
    ): Brevbestilling

    fun hent(referanse: BrevbestillingReferanse): Brevbestilling

    fun hent(unikReferanse: UnikReferanse): Brevbestilling?

    fun hentForOppdatering(referanse: BrevbestillingReferanse): Brevbestilling

    fun oppdaterBrev(
        referanse: BrevbestillingReferanse,
        brev: Brev,
    )

    fun oppdaterBrevmal(
        id: BrevbestillingId,
        brevmal: BrevmalJson,
    )

    fun oppdaterBrevdata(
        id: BrevbestillingId,
        brevdata: Brevdata,
    )

    fun oppdaterProsesseringStatus(
        referanse: BrevbestillingReferanse,
        prosesseringStatus: ProsesseringStatus,
    )

    fun nullstillProsesseringStatus(
        referanse: BrevbestillingReferanse
    )

    fun lagreSignaturer(brevbestillingId: BrevbestillingId, signaturer: List<SignaturGrunnlag>)

    fun oppdaterStatus(id: BrevbestillingId, status: Status)

    fun lagreJournalpost(id: BrevbestillingId, journalpostId: JournalpostId, journalpostFerdigstilt: Boolean)

    fun lagreJournalpostFerdigstilt(id: BrevbestillingId, journalpostFerdigstilt: Boolean)

    fun lagreDistribusjonBestilling(id: BrevbestillingId, distribusjonBestillingId: DistribusjonBestillingId)
}
