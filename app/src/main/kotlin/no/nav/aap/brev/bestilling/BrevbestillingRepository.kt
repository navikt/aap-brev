package no.nav.aap.brev.bestilling

import no.nav.aap.brev.distribusjon.DistribusjonBestillingId
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.prosessering.ProsesseringStatus
import no.nav.aap.brev.kontrakt.Brev

interface BrevbestillingRepository {
    fun opprettBestilling(
        saksnummer: Saksnummer,
        behandlingReferanse: BehandlingReferanse,
        unikReferanse: String,
        brevtype: Brevtype,
        språk: Språk,
        vedlegg: Set<Vedlegg>,
    ): Brevbestilling

    fun hent(referanse: BrevbestillingReferanse): Brevbestilling

    fun oppdaterBrev(
        referanse: BrevbestillingReferanse,
        brev: Brev,
    )

    fun oppdaterProsesseringStatus(
        referanse: BrevbestillingReferanse,
        prosesseringStatus: ProsesseringStatus,
    )

    fun lagreJournalpost(id: BrevbestillingId, journalpostId: JournalpostId, journalpostFerdigstilt: Boolean)

    fun lagreDistribusjonBestilling(id: BrevbestillingId, distribusjonBestillingId: DistribusjonBestillingId)
}
