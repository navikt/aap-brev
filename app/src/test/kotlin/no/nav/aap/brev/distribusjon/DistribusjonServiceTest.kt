package no.nav.aap.brev.distribusjon

import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.bestilling.IdentType
import no.nav.aap.brev.bestilling.JournalpostRepositoryImpl
import no.nav.aap.brev.bestilling.Mottaker
import no.nav.aap.brev.bestilling.MottakerRepositoryImpl
import no.nav.aap.brev.bestilling.OpprettetJournalpost
import no.nav.aap.brev.innhold.BrevinnholdService
import no.nav.aap.brev.innhold.FaktagrunnlagService
import no.nav.aap.brev.journalføring.JournalføringService
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.test.fakes.distribusjonBestillingIdForJournalpost
import no.nav.aap.brev.test.fakes.journalpostForBestilling
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.brev.test.randomBrukerIdent
import no.nav.aap.brev.test.randomDistribusjonBestillingId
import no.nav.aap.brev.test.randomJournalpostId
import no.nav.aap.brev.test.randomSaksnummer
import no.nav.aap.brev.test.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DistribusjonServiceTest {

    companion object {
        private val dataSource = InitTestDatabase.freshDatabase()

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            Fakes.start()
        }
    }

    @Test
    fun `distribuerer journalpost og lagrer distribusjon bestilling id`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val brevinnholdService = BrevinnholdService.konstruer(connection)
            val journalføringService = JournalføringService.konstruer(connection)
            val distribusjonService = DistribusjonService.konstruer(connection)
            val faktagrunnlagService = FaktagrunnlagService.konstruer(connection)
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
            mottakerRepository.lagreMottakere(bestilling.id, mottakereLikBrukerIdent(bestilling))
            val mottakere = mottakerRepository.hentMottakere(referanse)

            val journalpostId = randomJournalpostId()
            journalpostForBestilling(mottakere.first().bestillingMottakerReferanse, journalpostId)

            val forventetDistribusjonBestillingId = randomDistribusjonBestillingId()
            distribusjonBestillingIdForJournalpost(journalpostId, forventetDistribusjonBestillingId)

            brevinnholdService.hentOgLagre(referanse)
            faktagrunnlagService.hentOgFyllInnFaktagrunnlag(referanse)
            journalføringService.journalførBrevbestilling(referanse, mottakere)

            val journalpost = journalpostRepository.hentAlleFor(referanse).first()
            distribusjonService.distribuerBrev(journalpost)

            val oppdatertJournalpost = journalpostRepository.hentAlleFor(bestilling.referanse).first()
            assertEquals(forventetDistribusjonBestillingId, oppdatertJournalpost.distribusjonBestillingId)
        }
    }

    @Test
    fun `validering feiler dersom brevet ikke er journalført`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val distribusjonService = DistribusjonService.konstruer(connection)

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

            // Kan ikke se at joark noen gang kalles her - selv før endringene?
//            val journalpostId = randomJournalpostId()
//            journalpostForBestilling(referanse, journalpostId)

            val exception = assertThrows<IllegalStateException> {
                distribusjonService.distribuerBrev(
                    OpprettetJournalpost(
                        journalpostId = randomJournalpostId(),
                        mottaker = mottakereLikBrukerIdent(bestilling).first(),
                        ferdigstilt = false,
                        distribusjonBestillingId = null,
                        brevbestillingId = bestilling.id,
                        vedlegg = emptySet()
                    )
                )
            }
            assertEquals(exception.message, "Kan ikke distribuere en bestilling som ikke er journalført.")
        }
    }

    @Test
    fun `validering feiler dersom brevet allerede er distribuert`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val distribusjonService = DistribusjonService.konstruer(connection)

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

            val exception = assertThrows<IllegalStateException> {
                distribusjonService.distribuerBrev(
                    OpprettetJournalpost(
                        journalpostId = randomJournalpostId(),
                        mottaker = mottakereLikBrukerIdent(bestilling).first(),
                        ferdigstilt = true,
                        distribusjonBestillingId = randomDistribusjonBestillingId(),
                        brevbestillingId = bestilling.id,
                        vedlegg = emptySet()
                    )
                )
            }
            assertEquals(exception.message, "Brevet er allerede distribuert.")
        }

    }

    @Test
    fun `håndterer respons med http status 409 pga allerede distribuert`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val brevinnholdService = BrevinnholdService.konstruer(connection)
            val journalføringService = JournalføringService.konstruer(connection)
            val distribusjonService = DistribusjonService.konstruer(connection)
            val faktagrunnlagService = FaktagrunnlagService.konstruer(connection)
            val journalpostRepository = JournalpostRepositoryImpl(connection)
            val mottakerRepostory = MottakerRepositoryImpl(connection)

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
            
            // Evt. kall ferdigstill?
            mottakerRepostory.lagreMottakere(bestilling.id, mottakereLikBrukerIdent(bestilling))
            val mottakere = mottakerRepostory.hentMottakere(referanse)

            val journalpostId = randomJournalpostId()
            journalpostForBestilling(mottakere.first().bestillingMottakerReferanse, journalpostId)

            val forventetDistribusjonBestillingId = randomDistribusjonBestillingId()
            distribusjonBestillingIdForJournalpost(
                journalpost = journalpostId,
                distribusjonBestillingId = forventetDistribusjonBestillingId,
                finnesAllerede = true
            )

            brevinnholdService.hentOgLagre(referanse)
            faktagrunnlagService.hentOgFyllInnFaktagrunnlag(referanse)
            journalføringService.journalførBrevbestilling(referanse, mottakere)

            val journalposter = journalpostRepository.hentAlleFor(referanse)

            journalposter.forEach {
                distribusjonService.distribuerBrev(it)
            }

            val journalpost = journalpostRepository.hentAlleFor(referanse).first()

            assertEquals(forventetDistribusjonBestillingId, journalpost.distribusjonBestillingId)
        }

    }

    private fun opprettMottakereLikBrukerIdent(
        bestilling: Brevbestilling
    ) {
        return dataSource.transaction { connection ->
            MottakerRepositoryImpl(connection).lagreMottakere(
                bestilling.id, mottakereLikBrukerIdent(bestilling)
            )
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