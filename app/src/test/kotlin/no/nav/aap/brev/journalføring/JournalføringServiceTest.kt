package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.test.fakes.journalpostForBestilling
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.brev.test.randomBrukerIdent
import no.nav.aap.brev.test.randomJournalpostId
import no.nav.aap.brev.test.randomSaksnummer
import no.nav.aap.brev.test.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JournalføringServiceTest {

    companion object {
        private val dataSource = InitTestDatabase.freshDatabase()

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            Fakes.start()
        }
    }

    @Test
    fun `journalfører brevet og lagrer journalpostId`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val journalføringService = JournalføringService.konstruer(connection)

            val behandlingReferanse = randomBehandlingReferanse()
            val referanse = brevbestillingService.opprettBestillingV2(
                saksnummer = randomSaksnummer(),
                brukerIdent = randomBrukerIdent(),
                behandlingReferanse = behandlingReferanse,
                unikReferanse = randomUnikReferanse(),
                brevtype = Brevtype.INNVILGELSE,
                språk = Språk.NB,
                faktagrunnlag = emptySet(),
                vedlegg = emptySet(),
                ferdigstillAutomatisk = false,
            ).brevbestilling.referanse

            val forventetJournalpostId = randomJournalpostId()
            journalpostForBestilling(referanse, forventetJournalpostId)

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

            val referanse = brevbestillingService.opprettBestillingV2(
                saksnummer = randomSaksnummer(),
                brukerIdent = randomBrukerIdent(),
                behandlingReferanse = randomBehandlingReferanse(),
                unikReferanse = randomUnikReferanse(),
                brevtype = Brevtype.INNVILGELSE,
                språk = Språk.NB,
                faktagrunnlag = emptySet(),
                vedlegg = emptySet(),
                ferdigstillAutomatisk = false,
            ).brevbestilling.referanse

            // TODO slett brev med custom sql eller slett testen

            connection.execute(
                "UPDATE BREVBESTILLING SET BREV = ?::jsonb WHERE REFERANSE = ?"
            ) {
                setParams {
                    setString(1, null)
                    setUUID(2, referanse.referanse)
                }
            }

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
            val journalføringService = JournalføringService.konstruer(connection)

            val referanse = brevbestillingService.opprettBestillingV2(
                saksnummer = randomSaksnummer(),
                brukerIdent = randomBrukerIdent(),
                behandlingReferanse = randomBehandlingReferanse(),
                unikReferanse = randomUnikReferanse(),
                brevtype = Brevtype.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT,
                språk = Språk.NB,
                faktagrunnlag = emptySet(),
                vedlegg = emptySet(),
                ferdigstillAutomatisk = false,
            ).brevbestilling.referanse

            val exception = assertThrows<IllegalStateException> {
                journalføringService.journalførBrevbestilling(referanse)
            }
            assertEquals(exception.message, "Kan ikke lage PDF av brev med manglende faktagrunnlag FRIST_DATO_11_7.")
        }
    }

    @Test
    fun `validering feiler dersom brevet allerede er journalført`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val journalføringService = JournalføringService.konstruer(connection)

            val behandlingReferanse = randomBehandlingReferanse()
            val referanse = brevbestillingService.opprettBestillingV2(
                saksnummer = randomSaksnummer(),
                brukerIdent = randomBrukerIdent(),
                behandlingReferanse = behandlingReferanse,
                unikReferanse = randomUnikReferanse(),
                brevtype = Brevtype.INNVILGELSE,
                språk = Språk.NB,
                faktagrunnlag = emptySet(),
                vedlegg = emptySet(),
                ferdigstillAutomatisk = false,
            ).brevbestilling.referanse

            val forventetJournalpostId = randomJournalpostId()
            journalpostForBestilling(referanse, forventetJournalpostId)

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
            val journalføringService = JournalføringService.konstruer(connection)

            val behandlingReferanse = randomBehandlingReferanse()
            val referanse = brevbestillingService.opprettBestillingV2(
                saksnummer = randomSaksnummer(),
                brukerIdent = randomBrukerIdent(),
                behandlingReferanse = behandlingReferanse,
                unikReferanse = randomUnikReferanse(),
                brevtype = Brevtype.INNVILGELSE,
                språk = Språk.NB,
                faktagrunnlag = emptySet(),
                vedlegg = emptySet(),
                ferdigstillAutomatisk = false,
            ).brevbestilling.referanse

            val forventetJournalpostId = randomJournalpostId()
            journalpostForBestilling(referanse, forventetJournalpostId, finnesAllerede = true)

            journalføringService.journalførBrevbestilling(referanse)

            val bestilling = brevbestillingService.hent(referanse)
            assertEquals(forventetJournalpostId, bestilling.journalpostId)
        }
    }
}
