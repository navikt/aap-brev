package no.nav.aap.brev.journalføring

import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.Faktagrunnlag
import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.innhold.BrevinnholdService
import no.nav.aap.brev.innhold.FaktagrunnlagService
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.test.fakes.faktagrunnlagForBehandling
import no.nav.aap.brev.test.fakes.journalpostForBestilling
import no.nav.aap.brev.test.fakes.randomBehandlingReferanse
import no.nav.aap.brev.test.fakes.randomJournalpostId
import no.nav.aap.brev.test.fakes.randomSaksnummer
import no.nav.aap.brev.test.fakes.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JournalføringServiceTest {

    companion object {
        private val fakes = Fakes()
        private val dataSource = InitTestDatabase.dataSource

        @JvmStatic
        @AfterAll
        fun afterAll() {
            fakes.close()
        }
    }

    @Test
    fun `journalfører brevet og lagrer journalpostId`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val brevinnholdService = BrevinnholdService.konstruer(connection)
            val journalføringService = JournalføringService.konstruer(connection)
            val faktagrunnlagService = FaktagrunnlagService.konstruer(connection)

            val behandlingReferanse = randomBehandlingReferanse()
            val referanse = brevbestillingService.opprettBestilling(
                saksnummer = randomSaksnummer(),
                behandlingReferanse = behandlingReferanse,
                unikReferanse = randomUnikReferanse(),
                brevtype = Brevtype.INNVILGELSE,
                språk = Språk.NB,
                vedlegg = emptySet(),
            ).referanse

            faktagrunnlagForBehandling(behandlingReferanse, setOf(Faktagrunnlag.Testverdi("Testverdi")))
            val forventetJournalpostId = randomJournalpostId()
            journalpostForBestilling(referanse, forventetJournalpostId)

            brevinnholdService.hentOgLagre(referanse)

            faktagrunnlagService.hentOgFyllInnFaktagrunnlag(referanse)

            journalføringService.journalførBrevbestilling(referanse)

            val bestilling = brevbestillingService.hent(referanse)
            assertEquals(forventetJournalpostId, bestilling.journalpostId)
        }
    }

    @Test
    fun `validering feiler dersom bestillingen mangler brev`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val journalføringService = JournalføringService.konstruer(connection)

            val referanse = brevbestillingService.opprettBestilling(
                saksnummer = randomSaksnummer(),
                behandlingReferanse = randomBehandlingReferanse(),
                unikReferanse = randomUnikReferanse(),
                brevtype = Brevtype.INNVILGELSE,
                språk = Språk.NB,
                vedlegg = emptySet(),
            ).referanse

            val exception = assertThrows<IllegalStateException> {
                journalføringService.journalførBrevbestilling(referanse)
            }
            assertEquals(exception.message, "Kan ikke journalføre bestilling uten brev.")
        }
    }

    @Test
    fun `validering feiler dersom brevet inneholder manglende faktagrunnlag`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val brevinnholdService = BrevinnholdService.konstruer(connection)
            val journalføringService = JournalføringService.konstruer(connection)

            val referanse = brevbestillingService.opprettBestilling(
                saksnummer = randomSaksnummer(),
                behandlingReferanse = randomBehandlingReferanse(),
                unikReferanse = randomUnikReferanse(),
                brevtype = Brevtype.INNVILGELSE,
                språk = Språk.NB,
                vedlegg = emptySet(),
            ).referanse

            brevinnholdService.hentOgLagre(referanse)

            val exception = assertThrows<IllegalStateException> {
                journalføringService.journalførBrevbestilling(referanse)
            }
            assertEquals(exception.message, "Kan ikke lage PDF av brev med manglende faktagrunnlag TESTVERDI.")
        }
    }

    @Test
    fun `validering feiler dersom brevet allerede er journalført`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val brevinnholdService = BrevinnholdService.konstruer(connection)
            val journalføringService = JournalføringService.konstruer(connection)
            val faktagrunnlagService = FaktagrunnlagService.konstruer(connection)

            val behandlingReferanse = randomBehandlingReferanse()
            val referanse = brevbestillingService.opprettBestilling(
                saksnummer = randomSaksnummer(),
                behandlingReferanse = behandlingReferanse,
                unikReferanse = randomUnikReferanse(),
                brevtype = Brevtype.INNVILGELSE,
                språk = Språk.NB,
                vedlegg = emptySet(),
            ).referanse

            faktagrunnlagForBehandling(behandlingReferanse, setOf(Faktagrunnlag.Testverdi("Testverdi")))
            val forventetJournalpostId = randomJournalpostId()
            journalpostForBestilling(referanse, forventetJournalpostId)

            brevinnholdService.hentOgLagre(referanse)

            faktagrunnlagService.hentOgFyllInnFaktagrunnlag(referanse)

            journalføringService.journalførBrevbestilling(referanse)

            val bestilling = brevbestillingService.hent(referanse)
            assertEquals(forventetJournalpostId, bestilling.journalpostId)

            val exception = assertThrows<IllegalStateException> {
                journalføringService.journalførBrevbestilling(referanse)
            }

            assertEquals(exception.message, "Kan ikke journalføre brev for bestilling som allerede er journalført.")
        }

    }

    @Test
    fun `håndterer respons med http status 409 pga allerede journalført`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val brevinnholdService = BrevinnholdService.konstruer(connection)
            val journalføringService = JournalføringService.konstruer(connection)
            val faktagrunnlagService = FaktagrunnlagService.konstruer(connection)

            val behandlingReferanse = randomBehandlingReferanse()
            val referanse = brevbestillingService.opprettBestilling(
                saksnummer = randomSaksnummer(),
                behandlingReferanse = behandlingReferanse,
                unikReferanse = randomUnikReferanse(),
                brevtype = Brevtype.INNVILGELSE,
                språk = Språk.NB,
                vedlegg = emptySet(),
            ).referanse

            faktagrunnlagForBehandling(behandlingReferanse, setOf(Faktagrunnlag.Testverdi("Testverdi")))
            val forventetJournalpostId = randomJournalpostId()
            journalpostForBestilling(referanse, forventetJournalpostId, finnesAllerede = true)

            brevinnholdService.hentOgLagre(referanse)

            faktagrunnlagService.hentOgFyllInnFaktagrunnlag(referanse)

            journalføringService.journalførBrevbestilling(referanse)

            val bestilling = brevbestillingService.hent(referanse)
            assertEquals(forventetJournalpostId, bestilling.journalpostId)
        }
    }
}
