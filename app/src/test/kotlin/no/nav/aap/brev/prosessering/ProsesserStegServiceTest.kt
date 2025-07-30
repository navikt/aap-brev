package no.nav.aap.brev.prosessering

import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.Faktagrunnlag
import no.nav.aap.brev.IntegrationTest
import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.test.fakes.faktagrunnlagForBehandling
import no.nav.aap.brev.test.fakes.feilJournalføringFor
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

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
            faktagrunnlagForBehandling(behandlingReferanse, setOf(Faktagrunnlag.FristDato11_7(LocalDate.now())))

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
    fun `stopper prosessering på et steg med resultat stopp, og prosesserer videre fra neste steg ved ny prosessering prosessering etter ferdigstilling`() {
        val bestilling = dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)

            val bestilling =
                opprettBrevbestilling(brevtype = Brevtype.INNVILGELSE, ferdigstillAutomatisk = false).brevbestilling

            assertEquals(
                ProsesseringStatus.BREVBESTILLING_LØST,
                brevbestillingService.hent(bestilling.referanse).prosesseringStatus
            )
            assertEquals(
                Status.UNDER_ARBEID,
                brevbestillingService.hent(bestilling.referanse).status
            )
            bestilling
        }
        dataSource.transaction { connection ->
            val prosesserStegService = ProsesserStegService.konstruer(connection)
            val brevbestillingService = BrevbestillingService.konstruer(connection)

            brevbestillingService.ferdigstill(
                referanse = bestilling.referanse,
                signaturer = emptyList(),
                mottakere = emptyList()
            )
            prosesserStegService.prosesserBestilling(bestilling.referanse)

            assertEquals(
                ProsesseringStatus.FERDIG,
                brevbestillingService.hent(bestilling.referanse).prosesseringStatus
            )
            assertEquals(
                Status.FERDIGSTILT,
                brevbestillingService.hent(bestilling.referanse).status
            )
        }

    }

    @Test
    fun `stopper prosessering på et steg med resultat stopp, og feiler ved prosesserer videre fra neste steg dersom bestilling ikke er ferdigstilt`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val prosesserStegService = ProsesserStegService.konstruer(connection)

            val referanse = opprettBrevbestilling(
                brevtype = Brevtype.INNVILGELSE,
                ferdigstillAutomatisk = false
            ).brevbestilling.referanse

            assertEquals(
                ProsesseringStatus.BREVBESTILLING_LØST,
                brevbestillingService.hent(referanse).prosesseringStatus
            )
            assertEquals(
                Status.UNDER_ARBEID,
                brevbestillingService.hent(referanse).status
            )

            val exception = assertThrows<IllegalStateException> {
                prosesserStegService.prosesserBestilling(referanse)
            }
            assertThat(exception.message).isEqualTo(
                "Kan ikke fortsette ferdigstilling av bestilling med referanse: ${referanse.referanse} i status ${Status.UNDER_ARBEID}"
            )
        }
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

            assertEquals(
                ProsesseringStatus.BREVBESTILLING_LØST,
                brevbestillingService.hent(referanse).prosesseringStatus
            )
            assertEquals(
                Status.UNDER_ARBEID,
                brevbestillingService.hent(referanse).status
            )

            brevbestillingService.avbryt(referanse)

            assertEquals(
                ProsesseringStatus.AVBRUTT,
                brevbestillingService.hent(referanse).prosesseringStatus
            )

            prosesserStegService.prosesserBestilling(referanse)

            assertEquals(
                ProsesseringStatus.AVBRUTT,
                brevbestillingService.hent(referanse).prosesseringStatus
            )

            assertThat(
                dataSource.transaction { connection ->
                    BrevbestillingService.konstruer(connection).hent(referanse).journalpostId
                }
            ).isNull()
        }
    }
}
