package no.nav.aap.brev.bestilling

import no.nav.aap.brev.exception.ValideringsfeilException
import no.nav.aap.brev.innhold.BrevinnholdService
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.prosessering.ProsesseringStatus
import no.nav.aap.brev.test.fakes.brev
import no.nav.aap.brev.test.fakes.randomBehandlingReferanse
import no.nav.aap.brev.test.fakes.randomSaksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.*

class FerdigstillValideringTest {

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
    fun `ferdigstilling går igjennom dersom ingen valideringsfeil`() {
        val referanse = gittBrevMed(brev = brev(), status = ProsesseringStatus.BREVBESTILLING_LØST)
        ferdigstill(referanse)
        // TODO sjekk at fortsett prosessering blir gjort
    }

    @ParameterizedTest
    @EnumSource(
        ProsesseringStatus::class, names = ["STARTET", "INNHOLD_HENTET", "FAKTAGRUNNLAG_HENTET"]
    )
    fun `ferdigstill med status før BREVBESTILLING_LØST feiler`(status : ProsesseringStatus) {
        val referanse = gittBrevMed(brev = brev(), status = status)
        val exception = assertThrows<ValideringsfeilException> {
            ferdigstill(referanse)
        }
        assertThat(exception.message).endsWith(
            "Bestillingen er i feil status for ferdigstilling, prosesseringStatus=$status"
        )
    }

    @ParameterizedTest
    @EnumSource(
        ProsesseringStatus::class, names = [
            "BREV_FERDIGSTILT",
            "JOURNALFORT",
            "JOURNALPOST_VEDLEGG_TILKNYTTET",
            "JOURNALPOST_FERDIGSTILT",
            "DISTRIBUERT",
            "FERDIG"
        ]
    )
    fun `ferdigstill feiler ikke dersom status er etter BREVBESTILLING_LØST, men gjør ingen endring`(status : ProsesseringStatus) {
        val referanse = gittBrevMed(brev = brev(), status = status)
        ferdigstill(referanse)
        // TODO sjekk at fortsett prosessering ikke blir gjort
    }

    private fun gittBrevMed(brev: Brev, status: ProsesseringStatus): BrevbestillingReferanse {
        return dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)
            val brevinnholdService = BrevinnholdService.konstruer(connection)

            val referanse = brevbestillingService.opprettBestilling(
                saksnummer = randomSaksnummer(),
                behandlingReferanse = randomBehandlingReferanse(),
                unikReferanse = UUID.randomUUID().toString(),
                brevtype = Brevtype.INNVILGELSE,
                språk = Språk.NB,
                vedlegg = emptySet(),
            )

            brevinnholdService.hentOgLagre(referanse)
            brevbestillingService.oppdaterBrev(referanse, brev)
            brevbestillingRepository.oppdaterProsesseringStatus(referanse, status)

            referanse
        }
    }

    private fun ferdigstill(referanse: BrevbestillingReferanse) {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)

            brevbestillingService.ferdigstill(referanse)
        }
    }
}
