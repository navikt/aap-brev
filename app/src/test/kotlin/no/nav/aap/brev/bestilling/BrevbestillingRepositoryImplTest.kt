package no.nav.aap.brev.bestilling

import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.SignaturGrunnlag
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.prosessering.ProsesseringStatus
import no.nav.aap.brev.test.fakes.brev
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.brev.test.randomBrukerIdent
import no.nav.aap.brev.test.randomDistribusjonBestillingId
import no.nav.aap.brev.test.randomDokumentInfoId
import no.nav.aap.brev.test.randomJournalpostId
import no.nav.aap.brev.test.randomNavIdent
import no.nav.aap.brev.test.randomRolle
import no.nav.aap.brev.test.randomSaksnummer
import no.nav.aap.brev.test.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BrevbestillingRepositoryImplTest {

    companion object {
        private val dataSource = InitTestDatabase.dataSource
    }

    @Test
    fun `lagrer, henter og oppdaterer`() {
        dataSource.transaction { connection ->
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)

            val saksnummer = randomSaksnummer()
            val brukerIdent = randomBrukerIdent()
            val behandlingReferanse = randomBehandlingReferanse()
            val unikReferanse = randomUnikReferanse()
            val brevtype = Brevtype.INNVILGELSE
            val språk = Språk.NB
            val vedlegg = setOf<Vedlegg>(
                Vedlegg(journalpostId = randomJournalpostId(), randomDokumentInfoId()),
                Vedlegg(journalpostId = randomJournalpostId(), randomDokumentInfoId()),
            )
            val brev = brev()
            val signaturer = listOf<SignaturGrunnlag>(
                SignaturGrunnlag(randomNavIdent(), randomRolle()),
                SignaturGrunnlag(randomNavIdent(), randomRolle()),
            )
            val journalpostId = randomJournalpostId()
            val distribusjonBestillingId = randomDistribusjonBestillingId()

            var bestilling =
                brevbestillingRepository.opprettBestilling(
                    saksnummer = saksnummer,
                    brukerIdent = brukerIdent,
                    behandlingReferanse = behandlingReferanse,
                    unikReferanse = unikReferanse,
                    brevtype = brevtype,
                    språk = språk,
                    vedlegg = vedlegg
                )

            assertEquals(saksnummer, bestilling.saksnummer)
            assertEquals(brukerIdent, bestilling.brukerIdent)
            assertEquals(behandlingReferanse, bestilling.behandlingReferanse)
            assertEquals(unikReferanse, bestilling.unikReferanse)
            assertEquals(brevtype, bestilling.brevtype)
            assertEquals(språk, bestilling.språk)
            assertEquals(vedlegg, bestilling.vedlegg)
            assertNull(bestilling.brev)
            assertNull(bestilling.prosesseringStatus)
            assertNull(bestilling.journalpostId)
            assertNull(bestilling.journalpostFerdigstilt)

            brevbestillingRepository.oppdaterBrev(bestilling.referanse, brev)
            bestilling = brevbestillingRepository.hent(bestilling.referanse)

            assertEquals(brev, bestilling.brev)

            brevbestillingRepository.oppdaterProsesseringStatus(
                bestilling.referanse,
                ProsesseringStatus.FAKTAGRUNNLAG_HENTET
            )
            bestilling = brevbestillingRepository.hent(bestilling.referanse)

            assertEquals(ProsesseringStatus.FAKTAGRUNNLAG_HENTET, bestilling.prosesseringStatus)


            brevbestillingRepository.lagreSignaturer(bestilling.id, signaturer)
            bestilling = brevbestillingRepository.hent(bestilling.referanse)
            assertEquals(signaturer, bestilling.signaturer)

            brevbestillingRepository.lagreJournalpost(bestilling.id, journalpostId, journalpostFerdigstilt = true)
            bestilling = brevbestillingRepository.hent(bestilling.referanse)

            assertEquals(journalpostId, bestilling.journalpostId)
            assertTrue(bestilling.journalpostFerdigstilt == true)

            brevbestillingRepository.lagreDistribusjonBestilling(bestilling.id, distribusjonBestillingId)
            bestilling = brevbestillingRepository.hent(bestilling.referanse)

            assertEquals(distribusjonBestillingId, bestilling.distribusjonBestillingId)

            assertEquals(bestilling, brevbestillingRepository.hent(unikReferanse))
        }
    }
}
