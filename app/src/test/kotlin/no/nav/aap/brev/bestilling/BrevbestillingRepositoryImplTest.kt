package no.nav.aap.brev.bestilling

import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Rolle
import no.nav.aap.brev.kontrakt.SignaturGrunnlag
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.prosessering.ProsesseringStatus
import no.nav.aap.brev.test.fakes.brev
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.brev.test.randomBrukerIdent
import no.nav.aap.brev.test.randomDokumentInfoId
import no.nav.aap.brev.test.randomJournalpostId
import no.nav.aap.brev.test.randomNavIdent
import no.nav.aap.brev.test.randomSaksnummer
import no.nav.aap.brev.test.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BrevbestillingRepositoryImplTest {

    companion object {
        private val dataSource = InitTestDatabase.freshDatabase()
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
            val vedlegg = setOf(
                Vedlegg(journalpostId = randomJournalpostId(), randomDokumentInfoId()),
                Vedlegg(journalpostId = randomJournalpostId(), randomDokumentInfoId()),
            )
            val brev = brev()
            val signaturNavIdent1 = randomNavIdent()
            val signaturNavIdent2 = randomNavIdent()
            val signaturer = listOf(
                SignaturGrunnlag(signaturNavIdent1, null),
                SignaturGrunnlag(signaturNavIdent2, Rolle.KVALITETSSIKRER),
            )
            val journalpostId = randomJournalpostId()

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
            assertNull(bestilling.status)
            assertNull(bestilling.prosesseringStatus)

            brevbestillingRepository.oppdaterBrev(bestilling.referanse, brev)
            bestilling = brevbestillingRepository.hent(bestilling.referanse)

            assertEquals(brev, bestilling.brev)

            brevbestillingRepository.oppdaterStatus(
                bestilling.id,
                Status.UNDER_ARBEID
            )
            bestilling = brevbestillingRepository.hent(bestilling.referanse)

            assertEquals(Status.UNDER_ARBEID, bestilling.status)

            brevbestillingRepository.oppdaterProsesseringStatus(
                bestilling.referanse,
                ProsesseringStatus.STARTET
            )
            bestilling = brevbestillingRepository.hent(bestilling.referanse)

            assertEquals(ProsesseringStatus.STARTET, bestilling.prosesseringStatus)

            brevbestillingRepository.lagreSignaturer(bestilling.id, signaturer)
            bestilling = brevbestillingRepository.hent(bestilling.referanse)

            assertEquals(
                listOf(
                    SorterbarSignatur(signaturNavIdent1, 1, null), SorterbarSignatur(
                        signaturNavIdent2, 2,
                        Rolle.KVALITETSSIKRER
                    )
                ),
                bestilling.signaturer
            )

            brevbestillingRepository.lagreJournalpost(bestilling.id, journalpostId, journalpostFerdigstilt = false)
            bestilling = brevbestillingRepository.hent(bestilling.referanse)

            assertEquals(bestilling, brevbestillingRepository.hent(unikReferanse))
            assertEquals(
                brevbestillingRepository.hent(bestilling.referanse),
                brevbestillingRepository.hentForOppdatering(bestilling.referanse)
            )
        }
    }
}
