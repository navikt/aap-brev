package no.nav.aap.brev.prosessering

import no.nav.aap.brev.IntegrationTest
import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.bestilling.JournalpostRepositoryImpl
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.test.fakes.feilJournalføringFor
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

class ProsesserStegServiceTest : IntegrationTest() {

    @Test
    fun `prosesserer gjennom stegene`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val prosesserStegService = ProsesserStegService.konstruer(connection)

            val behandlingReferanse = randomBehandlingReferanse()
            val bestilling = opprettBrevbestilling(
                brevtype = Brevtype.VARSEL_OM_BESTILLING, // automatisk brev
                ferdigstillAutomatisk = true,
                behandlingReferanse = behandlingReferanse,
            ).brevbestilling
            val referanse = bestilling.referanse

            prosesserStegService.prosesserBestilling(referanse)

            assertEquals(
                ProsesseringStatus.FERDIG,
                brevbestillingService.hent(referanse).prosesseringStatus
            )
        }
    }

    @Test
    fun `lagrer prosesserte steg frem til det feiler`() {
        val bestilling = opprettBrevbestilling(
            brevtype = Brevtype.VARSEL_OM_BESTILLING, // automatisk brev
            ferdigstillAutomatisk = true
        ).brevbestilling

        feilJournalføringFor("${bestilling.referanse.referanse}-1")

        try {
            dataSource.transaction { connection ->
                val prosesserStegService = ProsesserStegService.konstruer(connection)
                prosesserStegService.prosesserBestilling(bestilling.referanse)
            }
        } catch (_: Exception) {
        }

        assertEquals(
            ProsesseringStatus.BREV_FERDIGSTILT,
            dataSource.transaction { connection ->
                BrevbestillingService.konstruer(connection).hent(bestilling.referanse).prosesseringStatus
            }
        )
    }

    @Test
    fun `prosesserer ikke videre på en bestilling som er avbrutt`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val prosesserStegService = ProsesserStegService.konstruer(connection)

            val referanse = opprettBrevbestilling(
                brevtype = Brevtype.INNVILGELSE,
                ferdigstillAutomatisk = false
            ).brevbestilling.referanse

            assertNull(brevbestillingService.hent(referanse).prosesseringStatus)
            assertEquals(
                Status.UNDER_ARBEID,
                brevbestillingService.hent(referanse).status
            )

            brevbestillingService.avbryt(referanse)

            assertEquals(
                Status.AVBRUTT,
                brevbestillingService.hent(referanse).status
            )
            assertEquals(
                ProsesseringStatus.AVBRUTT,
                brevbestillingService.hent(referanse).prosesseringStatus
            )

            prosesserStegService.prosesserBestilling(referanse)

            assertEquals(
                Status.AVBRUTT,
                brevbestillingService.hent(referanse).status
            )
            assertEquals(
                ProsesseringStatus.AVBRUTT,
                brevbestillingService.hent(referanse).prosesseringStatus
            )

            assertThat(
                dataSource.transaction { connection ->
                    JournalpostRepositoryImpl(connection).hentAlleFor(referanse)
                }
            ).isEmpty()
        }
    }
}
