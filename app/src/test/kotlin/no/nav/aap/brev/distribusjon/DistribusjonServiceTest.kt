package no.nav.aap.brev.distribusjon

import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.Faktagrunnlag
import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.innhold.BrevinnholdService
import no.nav.aap.brev.innhold.FaktagrunnlagService
import no.nav.aap.brev.journalføring.JournalføringService
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.test.fakes.distribusjonBestillingIdForJournalpost
import no.nav.aap.brev.test.fakes.faktagrunnlagForBehandling
import no.nav.aap.brev.test.fakes.journalpostForBestilling
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.brev.test.randomDistribusjonBestillingId
import no.nav.aap.brev.test.randomJournalpostId
import no.nav.aap.brev.test.randomSaksnummer
import no.nav.aap.brev.test.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DistribusjonServiceTest {

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
    fun `distribuerer journalpost og lagrer distribusjon bestilling id`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val brevinnholdService = BrevinnholdService.konstruer(connection)
            val journalføringService = JournalføringService.konstruer(connection)
            val distribusjonService = DistribusjonService.konstruer(connection)
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
            val journalpostId = randomJournalpostId()
            journalpostForBestilling(referanse, journalpostId)

            val forventetDistribusjonBestillingId = randomDistribusjonBestillingId()
            distribusjonBestillingIdForJournalpost(journalpostId, forventetDistribusjonBestillingId)

            brevinnholdService.hentOgLagre(referanse)
            faktagrunnlagService.hentOgFyllInnFaktagrunnlag(referanse)
            journalføringService.journalførBrevbestilling(referanse)
            distribusjonService.distribuerBrev(referanse)

            val bestilling = brevbestillingService.hent(referanse)
            assertEquals(forventetDistribusjonBestillingId, bestilling.distribusjonBestillingId)
        }
    }

    @Test
    fun `validering feiler dersom brevet ikke er journalført`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val brevinnholdService = BrevinnholdService.konstruer(connection)
            val distribusjonService = DistribusjonService.konstruer(connection)
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
            val journalpostId = randomJournalpostId()
            journalpostForBestilling(referanse, journalpostId)

            brevinnholdService.hentOgLagre(referanse)
            faktagrunnlagService.hentOgFyllInnFaktagrunnlag(referanse)

            val exception = assertThrows<IllegalStateException> {
                distribusjonService.distribuerBrev(referanse)
            }
            assertEquals(exception.message, "Kan ikke distribuere en bestilling som ikke er journalført.")
        }
    }

    @Test
    fun `validering feiler dersom brevet allerede er distribuert`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val brevinnholdService = BrevinnholdService.konstruer(connection)
            val journalføringService = JournalføringService.konstruer(connection)
            val distribusjonService = DistribusjonService.konstruer(connection)
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
            val journalpostId = randomJournalpostId()
            journalpostForBestilling(referanse, journalpostId)

            brevinnholdService.hentOgLagre(referanse)
            faktagrunnlagService.hentOgFyllInnFaktagrunnlag(referanse)
            journalføringService.journalførBrevbestilling(referanse)
            distribusjonService.distribuerBrev(referanse)

            val exception = assertThrows<IllegalStateException> {
                distribusjonService.distribuerBrev(referanse)
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
            val journalpostId = randomJournalpostId()
            journalpostForBestilling(referanse, journalpostId)

            val forventetDistribusjonBestillingId = randomDistribusjonBestillingId()
            distribusjonBestillingIdForJournalpost(
                journalpost = journalpostId,
                distribusjonBestillingId = forventetDistribusjonBestillingId,
                finnesAllerede = true
            )

            brevinnholdService.hentOgLagre(referanse)
            faktagrunnlagService.hentOgFyllInnFaktagrunnlag(referanse)
            journalføringService.journalførBrevbestilling(referanse)
            distribusjonService.distribuerBrev(referanse)

            val bestilling = brevbestillingService.hent(referanse)
            assertEquals(forventetDistribusjonBestillingId, bestilling.distribusjonBestillingId)
        }

    }
}