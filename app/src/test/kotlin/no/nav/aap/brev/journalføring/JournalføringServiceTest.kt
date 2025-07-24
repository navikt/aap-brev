package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.bestilling.IdentType
import no.nav.aap.brev.bestilling.JournalpostRepositoryImpl
import no.nav.aap.brev.bestilling.Mottaker
import no.nav.aap.brev.bestilling.MottakerRepositoryImpl
import no.nav.aap.brev.innhold.FaktagrunnlagService
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
import org.junit.jupiter.api.Disabled
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
            val faktagrunnlagService = FaktagrunnlagService.konstruer(connection)
            val journalpostRepository = JournalpostRepositoryImpl(connection)
            val mottakerRepository = MottakerRepositoryImpl(connection)

            val bestilling = brevbestillingService.opprettBestillingV2(
                saksnummer = randomSaksnummer(),
                brukerIdent = randomBrukerIdent(),
                behandlingReferanse = behandlingReferanse,
                unikReferanse = randomUnikReferanse(),
                brevtype = Brevtype.INNVILGELSE,
                språk = Språk.NB,
                faktagrunnlag = emptySet(),
                vedlegg = emptySet(),
                ferdigstillAutomatisk = false,
            ).brevbestilling

            val referanse = bestilling.referanse
            mottakerRepository.lagreMottakere(bestilling.id, mottakereLikBrukerIdent(bestilling))

            val forventetJournalpostId = randomJournalpostId()
            journalpostForBestilling(referanse, forventetJournalpostId)

            faktagrunnlagService.hentOgFyllInnFaktagrunnlag(referanse)

            val mottakere = mottakerRepository.hentMottakere(referanse)
            journalføringService.journalførBrevbestilling(referanse, mottakere)
            val journalposter = journalpostRepository.hentAlleFor(referanse)

            assertEquals(forventetJournalpostId, journalposter.first().journalpostId)
        }
    }

    @Test
    fun `validering feiler dersom bestillingen mangler brev`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val journalføringService = JournalføringService.konstruer(connection)
            val mottakerRepository = MottakerRepositoryImpl(connection)

            val bestilling = brevbestillingService.opprettBestillingV2(
                saksnummer = randomSaksnummer(),
                brukerIdent = randomBrukerIdent(),
                behandlingReferanse = randomBehandlingReferanse(),
                unikReferanse = randomUnikReferanse(),
                brevtype = Brevtype.INNVILGELSE,
                språk = Språk.NB,
                faktagrunnlag = emptySet(),
                vedlegg = emptySet(),
                ferdigstillAutomatisk = false,
            ).brevbestilling
            val referanse = bestilling.referanse
            mottakerRepository.lagreMottakere(bestilling.id, mottakereLikBrukerIdent(bestilling))
            val mottakere = mottakerRepository.hentMottakere(referanse)

            connection.execute(
                "UPDATE BREVBESTILLING SET BREV = ?::jsonb WHERE REFERANSE = ?"
            ) {
                setParams {
                    setString(1, null)
                    setUUID(2, referanse.referanse)
                }
            }

            val exception = assertThrows<IllegalStateException> {
                journalføringService.journalførBrevbestilling(referanse, mottakere)
            }
            assertEquals(exception.message, "Kan ikke generere pdf av brevbestilling uten brev.")
        }
    }

    @Test
    fun `validering feiler dersom brevet inneholder manglende faktagrunnlag`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val journalføringService = JournalføringService.konstruer(connection)

            val bestilling = brevbestillingService.opprettBestillingV2(
                saksnummer = randomSaksnummer(),
                brukerIdent = randomBrukerIdent(),
                behandlingReferanse = randomBehandlingReferanse(),
                unikReferanse = randomUnikReferanse(),
                brevtype = Brevtype.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT,
                språk = Språk.NB,
                faktagrunnlag = emptySet(),
                vedlegg = emptySet(),
                ferdigstillAutomatisk = false,
            ).brevbestilling
            val referanse = bestilling.referanse

            val exception = assertThrows<IllegalStateException> {
                journalføringService.journalførBrevbestilling(referanse, mottakereLikBrukerIdent(bestilling))
            }
            assertEquals(exception.message, "Kan ikke lage PDF av brev med manglende faktagrunnlag FRIST_DATO_11_7.")
        }
    }

    @Disabled("Filtrere vekk i forkant - erstatt kanskje med en test for dette?")
    @Test
    fun `validering feiler dersom brevet allerede er journalført`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val journalføringService = JournalføringService.konstruer(connection)

            val journalpostRepository = JournalpostRepositoryImpl(connection)
            val mottakerRepository = MottakerRepositoryImpl(connection)

            val behandlingReferanse = randomBehandlingReferanse()
            val bestilling = brevbestillingService.opprettBestillingV2(
                saksnummer = randomSaksnummer(),
                brukerIdent = randomBrukerIdent(),
                behandlingReferanse = behandlingReferanse,
                unikReferanse = randomUnikReferanse(),
                brevtype = Brevtype.INNVILGELSE,
                språk = Språk.NB,
                faktagrunnlag = emptySet(),
                vedlegg = emptySet(),
                ferdigstillAutomatisk = false,
            ).brevbestilling

            val referanse = bestilling.referanse
            val forventetJournalpostId = randomJournalpostId()
            journalpostForBestilling(referanse, forventetJournalpostId)

            mottakerRepository.lagreMottakere(bestilling.id, mottakereLikBrukerIdent(bestilling))
            val mottakere = mottakerRepository.hentMottakere(referanse)

            journalføringService.journalførBrevbestilling(referanse, mottakere)

            val journalpost = journalpostRepository.hentAlleFor(referanse).single()
            assertEquals(forventetJournalpostId, journalpost.journalpostId)

            val exception = assertThrows<IllegalStateException> {
                journalføringService.journalførBrevbestilling(referanse, mottakere)
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
            val mottakerRepository = MottakerRepositoryImpl(connection)
            val journalpostRepository = JournalpostRepositoryImpl(connection)
       
            val bestilling = brevbestillingService.opprettBestillingV2(
                saksnummer = randomSaksnummer(),
                brukerIdent = randomBrukerIdent(),
                behandlingReferanse = behandlingReferanse,
                unikReferanse = randomUnikReferanse(),
                brevtype = Brevtype.INNVILGELSE,
                språk = Språk.NB,
                faktagrunnlag = emptySet(),
                vedlegg = emptySet(),
                ferdigstillAutomatisk = false,
            ).brevbestilling
            val referanse = bestilling.referanse
            mottakerRepository.lagreMottakere(bestilling.id, mottakereLikBrukerIdent(bestilling))

            val forventetJournalpostId = randomJournalpostId()
            journalpostForBestilling(referanse, forventetJournalpostId, finnesAllerede = true)

            val mottakere = mottakerRepository.hentMottakere(referanse)
            journalføringService.journalførBrevbestilling(referanse, mottakere)

            val journalpost = journalpostRepository.hentAlleFor(referanse).single()
            assertEquals(forventetJournalpostId, journalpost.journalpostId)
        }
    }

    private fun mottakereLikBrukerIdent(brevbestilling: Brevbestilling): List<Mottaker> {
        requireNotNull(brevbestilling.brukerIdent) { "Denne hjelpemetoden støtter ikke null" }
        return listOf(
            Mottaker(
                ident = brevbestilling.brukerIdent,
                identType = IdentType.FNR,
                bestillingMottakerReferanse = "${brevbestilling.referanse.referanse}-1",
            )
        )
    }
}
