package no.nav.aap.brev.bestilling

import no.nav.aap.brev.distribusjon.DistribusjonBestillingId
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.prosessering.ProsesseringStatus
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.SignaturGrunnlag

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

    fun oppdaterBrev(
        referanse: BrevbestillingReferanse,
        brev: Brev,
    )

    fun oppdaterProsesseringStatus(
        referanse: BrevbestillingReferanse,
        prosesseringStatus: ProsesseringStatus,
    )

    fun lagreSignaturer(brevbestillingId: BrevbestillingId, signaturer: List<SignaturGrunnlag>)

    fun lagreJournalpost(id: BrevbestillingId, journalpostId: JournalpostId, journalpostFerdigstilt: Boolean)

    fun lagreJournalpostFerdigstilt(id: BrevbestillingId, journalpostFerdigstilt: Boolean)

    fun lagreDistribusjonBestilling(id: BrevbestillingId, distribusjonBestillingId: DistribusjonBestillingId)
}
