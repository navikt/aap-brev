package no.nav.aap.brev.prosessering

import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.Faktagrunnlag
import no.nav.aap.brev.bestilling.BehandlingReferanse
import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.bestilling.BrevbestillingService

import no.nav.aap.brev.bestilling.IdentType
import no.nav.aap.brev.bestilling.Mottaker
import no.nav.aap.brev.bestilling.MottakerRepositoryImpl
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.test.fakes.faktagrunnlagForBehandling
import no.nav.aap.brev.test.fakes.feilJournalføringFor
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.brev.test.randomBrukerIdent
import no.nav.aap.brev.test.randomSaksnummer
import no.nav.aap.brev.test.randomSpråk
import no.nav.aap.brev.test.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class ProsesserStegServiceTest {

    companion object {
        private val dataSource = InitTestDatabase.freshDatabase()

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
            val bestilling = opprettBestilling(
                brevtype = Brevtype.VARSEL_OM_BESTILLING, // automatisk brev
                behandlingReferanse = behandlingReferanse,
                ferdigstillAutomatisk = true
            )
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
        val bestilling = opprettBestilling(
            brevtype = Brevtype.VARSEL_OM_BESTILLING, // automatisk brev
            ferdigstillAutomatisk = true
        )

        val referanse = bestilling.referanse
        feilJournalføringFor(bestilling = referanse)

        try {
            dataSource.transaction { connection ->
                val prosesserStegService = ProsesserStegService.konstruer(connection)
                prosesserStegService.prosesserBestilling(referanse)
            }
        } catch (_: Exception) {
        }

        assertEquals(
            ProsesseringStatus.BREV_FERDIGSTILT,
            dataSource.transaction { connection ->
                BrevbestillingService.konstruer(connection).hent(referanse).prosesseringStatus
            }
        )
    }

    @Test
    fun `stopper prosessering på et steg med resultat stopp, og prosesserer videre fra neste steg ved ny prosessering prosessering etter ferdigstilling`() {
        val bestilling = dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)

            val bestilling = opprettBestilling(brevtype = Brevtype.INNVILGELSE, ferdigstillAutomatisk = false)
            val referanse = bestilling.referanse

            assertEquals(
                ProsesseringStatus.BREVBESTILLING_LØST,
                brevbestillingService.hent(referanse).prosesseringStatus
            )
            assertEquals(
                Status.UNDER_ARBEID,
                brevbestillingService.hent(referanse).status
            )
            bestilling
        }
        dataSource.transaction { connection ->
            val prosesserStegService = ProsesserStegService.konstruer(connection)
            val brevbestillingService = BrevbestillingService.konstruer(connection)

            brevbestillingService.ferdigstill(bestilling.referanse, emptyList(), mottakere = mottakereLikBrukerIdent(bestilling))
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

            val bestilling = opprettBestilling(brevtype = Brevtype.INNVILGELSE, ferdigstillAutomatisk = false)
            val referanse = bestilling.referanse

            assertEquals(
                ProsesseringStatus.BREVBESTILLING_LØST,
                brevbestillingService.hent(referanse).prosesseringStatus
            )
            assertEquals(
                Status.UNDER_ARBEID,
                brevbestillingService.hent(referanse).status
            )
            opprettMottakere(bestilling)

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

            val bestilling = opprettBestilling(brevtype = Brevtype.INNVILGELSE, ferdigstillAutomatisk = false)
            val referanse = bestilling.referanse

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

    private fun opprettBestilling(
        brevtype: Brevtype,
        ferdigstillAutomatisk: Boolean,
        behandlingReferanse: BehandlingReferanse = randomBehandlingReferanse(),
    ): Brevbestilling {
        return dataSource.transaction { connection ->
            BrevbestillingService.konstruer(connection)
                .opprettBestillingV2(
                    saksnummer = randomSaksnummer(),
                    brukerIdent = randomBrukerIdent(),
                    behandlingReferanse = behandlingReferanse,
                    unikReferanse = randomUnikReferanse(),
                    brevtype = brevtype,
                    språk = randomSpråk(),
                    faktagrunnlag = emptySet(),
                    vedlegg = emptySet(),
                    ferdigstillAutomatisk = ferdigstillAutomatisk,
                ).brevbestilling
        }
    }

    private fun opprettMottakere(
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
