package no.nav.aap.brev.distribusjon

import no.nav.aap.brev.IntegrationTest
import no.nav.aap.brev.bestilling.Adresse
import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.bestilling.IdentType
import no.nav.aap.brev.bestilling.JournalpostRepositoryImpl
import no.nav.aap.brev.bestilling.Mottaker
import no.nav.aap.brev.bestilling.NavnOgAdresse
import no.nav.aap.brev.journalføring.JournalføringService
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.test.fakes.brukerForDistkanal
import no.nav.aap.brev.test.fakes.brukerForRegoppslag
import no.nav.aap.brev.test.fakes.distribusjonBestillingIdForJournalpost
import no.nav.aap.brev.test.fakes.journalpostForBestilling
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.brev.test.randomDistribusjonBestillingId
import no.nav.aap.brev.test.randomJournalpostId
import no.nav.aap.komponenter.dbconnect.transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DistribusjonServiceTest : IntegrationTest() {
    @Test
    fun `skal ikke distribuere journalpost for bruker med ukjent bostedsadresse og print som distribusjonskanal`() {
        val behandlingReferanse = randomBehandlingReferanse()
        val bestilling = opprettBrevbestilling(
            behandlingReferanse = behandlingReferanse,
            brevtype = Brevtype.INNVILGELSE,
            ferdigstillAutomatisk = false,
        ).brevbestilling
        val referanse = bestilling.referanse
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val journalføringService = JournalføringService.konstruer(connection)
            val distribusjonService = DistribusjonService.konstruer(connection)
            val journalpostRepository = JournalpostRepositoryImpl(connection)

            val bestillingMottakerReferanse = "${bestilling.referanse.referanse}"
            val mottaker = Mottaker(
                ident = bestilling.brukerIdent,
                identType = IdentType.FNR,
                bestillingMottakerReferanse = bestillingMottakerReferanse,
            )

            brevbestillingService.ferdigstill(referanse, emptyList(), listOf(mottaker))
            journalføringService.journalførBrevbestilling(referanse)
            brukerForRegoppslag(mottaker.ident, false)
            brukerForDistkanal(mottaker.ident, Distribusjonskanal.PRINT)
            distribusjonService.distribuerBrev(referanse)

            val oppdaterteJournalposter = journalpostRepository.hentAlleFor(bestilling.referanse)

         // TODO Kommenter inn når feature toggle i distribuerBrev er verifisert i prod og skrevet ut
         // assertThat(oppdaterteJournalposter.get(0).distribusjonBestillingId).isNull()
        }
    }

    @Test
    fun `distribuerer journalpost og lagrer distribusjon bestilling id`() {
        val behandlingReferanse = randomBehandlingReferanse()
        val bestilling = opprettBrevbestilling(
            behandlingReferanse = behandlingReferanse,
            brevtype = Brevtype.INNVILGELSE,
            ferdigstillAutomatisk = false,
        ).brevbestilling
        val referanse = bestilling.referanse
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val journalføringService = JournalføringService.konstruer(connection)
            val distribusjonService = DistribusjonService.konstruer(connection)
            val journalpostRepository = JournalpostRepositoryImpl(connection)

            val bestillingMottakerReferanse1 = "${bestilling.referanse.referanse}-1"
            val bestillingMottakerReferanse2 = "${bestilling.referanse.referanse}-2"
            val mottaker1 = Mottaker(
                ident = bestilling.brukerIdent,
                identType = IdentType.FNR,
                bestillingMottakerReferanse = bestillingMottakerReferanse1
            )
            val mottaker2 = Mottaker(
                navnOgAdresse = NavnOgAdresse(
                    navn = "verge", adresse = Adresse(
                        landkode = "NO",
                        adresselinje1 = "adresselinje1",
                        adresselinje2 = "adresselinje2",
                        adresselinje3 = "adresselinje3",
                        postnummer = "postnummer",
                        poststed = "poststed",
                    )
                ),
                bestillingMottakerReferanse = bestillingMottakerReferanse2
            )

            val forventetJournalpostId1 = randomJournalpostId()
            val forventetJournalpostId2 = randomJournalpostId()
            journalpostForBestilling(bestillingMottakerReferanse1, forventetJournalpostId1)
            journalpostForBestilling(bestillingMottakerReferanse2, forventetJournalpostId2)

            val forventetDistribusjonBestillingId1 = randomDistribusjonBestillingId()
            val forventetDistribusjonBestillingId2 = randomDistribusjonBestillingId()
            distribusjonBestillingIdForJournalpost(forventetJournalpostId1, forventetDistribusjonBestillingId1)
            distribusjonBestillingIdForJournalpost(forventetJournalpostId2, forventetDistribusjonBestillingId2)

            brevbestillingService.ferdigstill(referanse, emptyList(), listOf(mottaker1, mottaker2))
            journalføringService.journalførBrevbestilling(referanse)
            brukerForRegoppslag(mottaker1.ident, true)
            distribusjonService.distribuerBrev(referanse)

            val oppdaterteJournalposter = journalpostRepository.hentAlleFor(bestilling.referanse)
            assertThat(oppdaterteJournalposter).hasSize(2)
            assertThat(oppdaterteJournalposter).anySatisfy { opprettetJournalpost ->
                assertThat(opprettetJournalpost.journalpostId).isEqualTo(forventetJournalpostId1)
                assertThat(opprettetJournalpost.distribusjonBestillingId).isEqualTo(forventetDistribusjonBestillingId1)
            }
            assertThat(oppdaterteJournalposter).anySatisfy { opprettetJournalpost ->
                assertThat(opprettetJournalpost.journalpostId).isEqualTo(forventetJournalpostId2)
                assertThat(opprettetJournalpost.distribusjonBestillingId).isEqualTo(forventetDistribusjonBestillingId2)
            }
        }
    }

    @Test
    fun `validering feiler dersom brevet ikke er journalført`() {
        val behandlingReferanse = randomBehandlingReferanse()
        val referanse = opprettBrevbestilling(
            behandlingReferanse = behandlingReferanse,
            brevtype = Brevtype.INNVILGELSE,
            ferdigstillAutomatisk = false,
        ).brevbestilling.referanse
        dataSource.transaction { connection ->
            val distribusjonService = DistribusjonService.konstruer(connection)

            val journalpostId = randomJournalpostId()
            journalpostForBestilling(referanse.referanse.toString(), journalpostId)

            val exception = assertThrows<IllegalStateException> {
                distribusjonService.distribuerBrev(referanse)
            }
            assertEquals(exception.message, "Kan ikke distribuere en bestilling som ikke er journalført.")
        }
    }

    @Test
    fun `håndterer respons med http status 409 pga allerede distribuert`() {
        val behandlingReferanse = randomBehandlingReferanse()
        val bestilling = opprettBrevbestilling(
            behandlingReferanse = behandlingReferanse,
            brevtype = Brevtype.INNVILGELSE,
            ferdigstillAutomatisk = false,
        ).brevbestilling
        val referanse = bestilling.referanse
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val journalføringService = JournalføringService.konstruer(connection)
            val distribusjonService = DistribusjonService.konstruer(connection)
            val journalpostRepository = JournalpostRepositoryImpl(connection)

            val bestillingMottakerReferanse = "${bestilling.referanse.referanse}-1"
            val journalpostId = randomJournalpostId()
            journalpostForBestilling(bestillingMottakerReferanse, journalpostId)

            val forventetDistribusjonBestillingId = randomDistribusjonBestillingId()
            distribusjonBestillingIdForJournalpost(
                journalpost = journalpostId,
                distribusjonBestillingId = forventetDistribusjonBestillingId,
                finnesAllerede = true
            )

            brevbestillingService.ferdigstill(referanse, emptyList(), emptyList())
            journalføringService.journalførBrevbestilling(referanse)
            distribusjonService.distribuerBrev(referanse)

            val oppdaterteJournalposter = journalpostRepository.hentAlleFor(bestilling.referanse)
            assertThat(oppdaterteJournalposter).hasSize(1)
            assertThat(oppdaterteJournalposter).anySatisfy { opprettetJournalpost ->
                assertThat(opprettetJournalpost.journalpostId).isEqualTo(journalpostId)
                assertThat(opprettetJournalpost.distribusjonBestillingId).isEqualTo(forventetDistribusjonBestillingId)
            }
        }
    }
}
