package no.nav.aap.brev.prosessering

import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.domene.BehandlingReferanse
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

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

            val referanse = brevbestillingService.opprettBestilling(
                BehandlingReferanse(UUID.randomUUID()),
                Brevtype.INNVILGELSE,
                Språk.nb,
            )

            prosesserStegService.prosesserBestilling(referanse)

            assertEquals(
                ProsesseringStatus.FERDIG,
                brevbestillingService.hent(referanse).prosesseringStatus
            )
        }
    }

    @Test
    fun `lagrer prosesserte steg frem til det feiler`() {
        // TODO når det finnes logikk for noe kan feile for en spesifikk bestilling
    }

    @Test
    fun `stopper prosessering på et steg med resultat stopp, og prosesserer steget på nytt ved neste prosessering`() {
        // TODO når det finnes logikk i et steg for å stoppe
    }
}
