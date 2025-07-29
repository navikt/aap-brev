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

            val bestillingMottakerReferanse = "${bestilling.unikReferanse.referanse}-1"
            mottakerRepository.lagreMottakere(
                bestilling.id,
                mottakereLikBrukerIdent(bestilling, bestillingMottakerReferanse)
            )

            val forventetJournalpostId = randomJournalpostId()
            journalpostForBestilling(bestillingMottakerReferanse, forventetJournalpostId)

            faktagrunnlagService.hentOgFyllInnFaktagrunnlag(bestilling.referanse)

            journalføringService.journalførBrevbestilling(bestilling.referanse)
            val journalposter = journalpostRepository.hentAlleFor(bestilling.referanse)

            assertEquals(forventetJournalpostId, brevbestillingService.hent(bestilling.referanse).journalpostId)
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
            val bestillingMottakerReferanse = "${bestilling.unikReferanse.referanse}-1"
            mottakerRepository.lagreMottakere(
                bestilling.id,
                mottakereLikBrukerIdent(bestilling, bestillingMottakerReferanse)
            )

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
            assertEquals(exception.message, "Kan ikke generere pdf av brevbestilling uten brev.")
        }
    }

    @Test
    fun `validering feiler dersom brevet inneholder manglende faktagrunnlag`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val journalføringService = JournalføringService.konstruer(connection)
            val mottakerRepository = MottakerRepositoryImpl(connection)

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
            mottakerRepository.lagreMottakere(
                bestilling.id,
                mottakereLikBrukerIdent(bestilling, "${bestilling.unikReferanse.referanse}-1")
            )
            val referanse = bestilling.referanse
            val exception = assertThrows<IllegalStateException> {
                journalføringService.journalførBrevbestilling(
                    referanse,
                )
            }
            assertEquals(exception.message, "Kan ikke lage PDF av brev med manglende faktagrunnlag FRIST_DATO_11_7.")
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
            val bestillingMottakerReferanse = "${bestilling.unikReferanse.referanse}-1"
            mottakerRepository.lagreMottakere(
                bestilling.id,
                mottakereLikBrukerIdent(bestilling, bestillingMottakerReferanse)
            )

            val forventetJournalpostId = randomJournalpostId()
            journalpostForBestilling(
                bestillingMottakerReferanse,
                forventetJournalpostId,
                finnesAllerede = true
            )

            journalføringService.journalførBrevbestilling(referanse)

            assertEquals(forventetJournalpostId, brevbestillingService.hent(referanse).journalpostId)
            val journalpost = journalpostRepository.hentAlleFor(referanse).single()
            assertEquals(forventetJournalpostId, journalpost.journalpostId)
        }
    }

    private fun mottakereLikBrukerIdent(
        brevbestilling: Brevbestilling,
        bestillingMottakerReferanse: String
    ): List<Mottaker> {
        requireNotNull(brevbestilling.brukerIdent) { "Denne hjelpemetoden støtter ikke null" }
        return listOf(
            Mottaker(
                ident = brevbestilling.brukerIdent,
                identType = IdentType.FNR,
                bestillingMottakerReferanse = bestillingMottakerReferanse,
            )
        )
    }
}
