package no.nav.aap.brev.bestilling

import no.nav.aap.brev.IntegrationTest
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.test.fakes.gittJournalpostIArkivet
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.brev.test.randomBrukerIdent
import no.nav.aap.brev.test.randomDokumentInfoId
import no.nav.aap.brev.test.randomJournalpostId
import no.nav.aap.brev.test.randomSaksnummer
import no.nav.aap.brev.test.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BestillingDuplikathåndteringTest : IntegrationTest() {

    @Test
    fun `håndterer duplikat bestilling`() {
        val saksnummer = randomSaksnummer()
        val brukerIdent = randomBrukerIdent()
        val behandlingReferanse = randomBehandlingReferanse()
        val unikReferanse = randomUnikReferanse()
        val dokumentInfoId = randomDokumentInfoId()
        val journalpost = gittJournalpostIArkivet(
            journalpostId = randomJournalpostId(),
            saksnummer = saksnummer,
            dokumentInfoId = dokumentInfoId
        )
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)

            val resultatFørste = brevbestillingService.opprettBestillingV2(
                saksnummer = saksnummer,
                brukerIdent = brukerIdent,
                behandlingReferanse = behandlingReferanse,
                unikReferanse = unikReferanse,
                brevtype = Brevtype.INNVILGELSE,
                språk = Språk.NB,
                faktagrunnlag = emptySet(),
                vedlegg = setOf(Vedlegg(journalpost.journalpostId, dokumentInfoId)),
                ferdigstillAutomatisk = false,
            )

            assertFalse(resultatFørste.alleredeOpprettet)

            val resultatAndre = brevbestillingService.opprettBestillingV2(
                saksnummer = saksnummer,
                brukerIdent = brukerIdent,
                behandlingReferanse = behandlingReferanse,
                unikReferanse = unikReferanse,
                brevtype = Brevtype.INNVILGELSE,
                språk = Språk.NB,
                faktagrunnlag = emptySet(),
                vedlegg = setOf(Vedlegg(journalpost.journalpostId, dokumentInfoId)),
                ferdigstillAutomatisk = false,
            )

            assertTrue(resultatAndre.alleredeOpprettet)
        }
    }

    @Test
    fun `feiler dersom bestilling med lik unik referanse ikke er identisk med opprinnelig bestilling`() {
        val saksnummer = randomSaksnummer()
        val brukerIdent = randomBrukerIdent()
        val behandlingReferanse = randomBehandlingReferanse()
        val unikReferanse = randomUnikReferanse()
        val dokumentInfoId = randomDokumentInfoId()
        val journalpost = gittJournalpostIArkivet(
            journalpostId = randomJournalpostId(),
            saksnummer = saksnummer,
            dokumentInfoId = dokumentInfoId
        )
        val bestilling = dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)

            val referanse = brevbestillingService.opprettBestillingV2(
                saksnummer = saksnummer,
                brukerIdent = brukerIdent,
                behandlingReferanse = behandlingReferanse,
                unikReferanse = unikReferanse,
                brevtype = Brevtype.INNVILGELSE,
                språk = Språk.NB,
                faktagrunnlag = emptySet(),
                vedlegg = setOf(Vedlegg(journalpost.journalpostId, dokumentInfoId)),
                ferdigstillAutomatisk = false,
            ).brevbestilling.referanse

            brevbestillingRepository.hent(referanse)
        }

        assertThrowsVedLikUnikReferanseMenUlikBestilling(
            bestilling = bestilling,
            saksnummer = randomSaksnummer(),
        )

        assertThrowsVedLikUnikReferanseMenUlikBestilling(
            bestilling = bestilling,
            brukerIdent = randomBrukerIdent(),
        )

        assertThrowsVedLikUnikReferanseMenUlikBestilling(
            bestilling = bestilling,
            behandlingReferanse = randomBehandlingReferanse()
        )

        assertThrowsVedLikUnikReferanseMenUlikBestilling(
            bestilling = bestilling,
            brevtype = Brevtype.AVSLAG,
        )

        assertThrowsVedLikUnikReferanseMenUlikBestilling(
            bestilling = bestilling,
            språk = Språk.NN
        )

        assertThrowsVedLikUnikReferanseMenUlikBestilling(
            bestilling = bestilling,
            vedlegg = emptySet()
        )
    }

    fun assertThrowsVedLikUnikReferanseMenUlikBestilling(
        bestilling: Brevbestilling,
        saksnummer: Saksnummer = bestilling.saksnummer,
        brukerIdent: String? = bestilling.brukerIdent,
        behandlingReferanse: BehandlingReferanse = bestilling.behandlingReferanse,
        brevtype: Brevtype = bestilling.brevtype,
        språk: Språk = bestilling.språk,
        vedlegg: Set<Vedlegg> = bestilling.vedlegg,
    ) {
        val endretBestilling = bestilling.copy(
            saksnummer = saksnummer,
            brukerIdent = brukerIdent,
            behandlingReferanse = behandlingReferanse,
            brevtype = brevtype,
            språk = språk,
            vedlegg = vedlegg,
        )
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)

            val exception = assertThrows<IllegalStateException> {
                brevbestillingService.opprettBestillingV2(
                    saksnummer = endretBestilling.saksnummer,
                    brukerIdent = endretBestilling.brukerIdent,
                    behandlingReferanse = endretBestilling.behandlingReferanse,
                    unikReferanse = endretBestilling.unikReferanse,
                    brevtype = endretBestilling.brevtype,
                    språk = endretBestilling.språk,
                    faktagrunnlag = emptySet(),
                    vedlegg = endretBestilling.vedlegg,
                    ferdigstillAutomatisk = false,
                )
            }

            assertEquals(
                exception.message,
                "Bestilling med unikReferanse=${bestilling.unikReferanse.referanse} finnnes allerede, men er ikke samme bestilling."
            )
        }
    }
}
