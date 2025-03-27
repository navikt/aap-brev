package no.nav.aap.brev.prosessering

import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.Faktagrunnlag
import no.nav.aap.brev.bestilling.BehandlingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.test.fakes.faktagrunnlagForBehandling
import no.nav.aap.brev.test.fakes.feilLøsBestillingFor
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.brev.test.randomBrevtype
import no.nav.aap.brev.test.randomBrukerIdent
import no.nav.aap.brev.test.randomSaksnummer
import no.nav.aap.brev.test.randomSpråk
import no.nav.aap.brev.test.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ProsesserStegServiceTest {

    companion object {
        private val dataSource = InitTestDatabase.dataSource

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            Fakes.start()
        }
    }

    @Test
    fun `prosesserer gjennom stegene`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val prosesserStegService = ProsesserStegService.konstruer(connection)

            val behandlingReferanse = randomBehandlingReferanse()
            val referanse = opprettBestilling(
                behandlingReferanse = behandlingReferanse,
                brevtype = Brevtype.VARSEL_OM_BESTILLING
            )
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
        val referanse = opprettBestilling()
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
    fun `stopper prosessering på et steg med resultat stopp, og prosesserer videre fra neste steg ved ny prosessering prosessering`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val prosesserStegService = ProsesserStegService.konstruer(connection)

            val referanse = opprettBestilling(brevtype = Brevtype.INNVILGELSE)

            prosesserStegService.prosesserBestilling(referanse)

            assertEquals(
                ProsesseringStatus.BREVBESTILLING_LØST,
                brevbestillingService.hent(referanse).prosesseringStatus
            )

            prosesserStegService.prosesserBestilling(referanse)

            assertEquals(
                ProsesseringStatus.FERDIG,
                brevbestillingService.hent(referanse).prosesseringStatus
            )
        }
    }

    @Test
    fun `prosesserer ikke videre på en bestilling som er avbrutt`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val prosesserStegService = ProsesserStegService.konstruer(connection)

            val referanse = opprettBestilling(brevtype = Brevtype.INNVILGELSE)
            prosesserStegService.prosesserBestilling(referanse)

            assertEquals(
                ProsesseringStatus.BREVBESTILLING_LØST,
                brevbestillingService.hent(referanse).prosesseringStatus
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
        }
    }

    private fun opprettBestilling(behandlingReferanse: BehandlingReferanse = randomBehandlingReferanse(),
                                  brevtype: Brevtype = randomBrevtype()
    ): BrevbestillingReferanse {
        return dataSource.transaction { connection ->
            BrevbestillingService.konstruer(connection)
                .opprettBestilling(
                    saksnummer = randomSaksnummer(),
                    brukerIdent = randomBrukerIdent(),
                    behandlingReferanse = behandlingReferanse,
                    unikReferanse = randomUnikReferanse(),
                    brevtype = brevtype,
                    språk = randomSpråk(),
                    vedlegg = emptySet(),
                ).referanse
        }
    }
}
