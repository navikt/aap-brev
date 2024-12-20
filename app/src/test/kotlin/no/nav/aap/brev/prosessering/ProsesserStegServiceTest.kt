package no.nav.aap.brev.prosessering

import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.innhold.Faktagrunnlag
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.test.fakes.faktagrunnlagForBehandling
import no.nav.aap.brev.test.fakes.feilLøsBestillingFor
import no.nav.aap.brev.test.fakes.randomBehandlingReferanse
import no.nav.aap.brev.test.fakes.randomSaksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ProsesserStegServiceTest {

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
    fun `prosesserer gjennom stegene`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val prosesserStegService = ProsesserStegService.konstruer(connection)

            val behandlingReferanse = randomBehandlingReferanse()
            val referanse = brevbestillingService.opprettBestilling(
                randomSaksnummer(),
                behandlingReferanse,
                Brevtype.INNVILGELSE,
                Språk.NB,
                emptySet(),
            )
            faktagrunnlagForBehandling(behandlingReferanse, setOf(Faktagrunnlag.Startdato(LocalDate.now())))

            prosesserStegService.prosesserBestilling(referanse)

            assertEquals(
                ProsesseringStatus.FERDIG,
                brevbestillingService.hent(referanse).prosesseringStatus
            )
        }
    }

    @Test
    fun `lagrer prosesserte steg frem til det feiler`() {
        val referanse = dataSource.transaction { connection ->
            BrevbestillingService.konstruer(connection)
                .opprettBestilling(
                    randomSaksnummer(),
                    randomBehandlingReferanse(),
                    Brevtype.INNVILGELSE,
                    Språk.NB,
                    emptySet(),
                )
        }

        feilLøsBestillingFor(bestilling = referanse)

        try {
            dataSource.transaction { connection ->
                val prosesserStegService = ProsesserStegService.konstruer(connection)
                prosesserStegService.prosesserBestilling(referanse)
            }
        } catch (_: Exception) {
        }

        assertEquals(
            ProsesseringStatus.FAKTAGRUNNLAG_HENTET,
            dataSource.transaction { connection ->
                BrevbestillingService.konstruer(connection).hent(referanse).prosesseringStatus
            }
        )
    }

    @Test
    fun `stopper prosessering på et steg med resultat stopp, og prosesserer steget på nytt ved neste prosessering`() {
        // TODO når det finnes logikk i et steg for å stoppe
    }
}
