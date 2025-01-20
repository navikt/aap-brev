package no.nav.aap.brev.bestilling

import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.prosessering.ProsesseringStatus
import no.nav.aap.brev.test.fakes.brev
import no.nav.aap.brev.test.fakes.randomBehandlingReferanse
import no.nav.aap.brev.test.fakes.randomDistribusjonBestillingId
import no.nav.aap.brev.test.fakes.randomDokumentInfoId
import no.nav.aap.brev.test.fakes.randomJournalpostId
import no.nav.aap.brev.test.fakes.randomSaksnummer
import no.nav.aap.brev.test.fakes.randomUnikReferanse
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
            val behandlingReferanse = randomBehandlingReferanse()
            val unikReferanse = randomUnikReferanse()
            val brevtype = Brevtype.INNVILGELSE
            val språk = Språk.NB
            val vedlegg = setOf<Vedlegg>(
                Vedlegg(journalpostId = randomJournalpostId(), randomDokumentInfoId()),
                Vedlegg(journalpostId = randomJournalpostId(), randomDokumentInfoId()),
            )
            val brev = brev()
            val journalpostId = randomJournalpostId()
            val distribusjonBestillingId = randomDistribusjonBestillingId()

            var referanse =
                brevbestillingRepository.opprettBestilling(
                    saksnummer = saksnummer,
                    behandlingReferanse = behandlingReferanse,
                    unikReferanse = unikReferanse,
                    brevtype = brevtype,
                    språk = språk,
                    vedlegg = vedlegg
                ).referanse

            var bestilling = brevbestillingRepository.hent(referanse)

            assertEquals(saksnummer, bestilling.saksnummer)
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

            brevbestillingRepository.lagreJournalpost(bestilling.id, journalpostId, journalpostFerdigstilt = true)
            bestilling = brevbestillingRepository.hent(bestilling.referanse)

            assertEquals(journalpostId, bestilling.journalpostId)
            assertTrue(bestilling.journalpostFerdigstilt == true)

            brevbestillingRepository.lagreDistribusjonBestilling(bestilling.id, distribusjonBestillingId)
            bestilling = brevbestillingRepository.hent(bestilling.referanse)

            assertEquals(distribusjonBestillingId, bestilling.distribusjonBestillingId)
        }
    }
}
