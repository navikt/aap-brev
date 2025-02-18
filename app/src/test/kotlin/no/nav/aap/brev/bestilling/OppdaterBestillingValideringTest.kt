package no.nav.aap.brev.bestilling

import no.nav.aap.brev.exception.ValideringsfeilException
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.prosessering.ProsesseringStatus
import no.nav.aap.brev.test.fakes.brev
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.brev.test.randomBrevtype
import no.nav.aap.brev.test.randomSaksnummer
import no.nav.aap.brev.test.randomSpråk
import no.nav.aap.brev.test.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode

class OppdaterBestillingValideringTest {
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
    fun `oppdaterer brev i riktig status`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)

            val referanse = brevbestillingService.opprettBestilling(
                saksnummer = randomSaksnummer(),
                behandlingReferanse = randomBehandlingReferanse(),
                unikReferanse = randomUnikReferanse(),
                brevtype = randomBrevtype(),
                språk = randomSpråk(),
                vedlegg = emptySet(),
            ).referanse

            brevbestillingRepository.oppdaterProsesseringStatus(referanse, ProsesseringStatus.BREVBESTILLING_LØST)

            brevbestillingService.oppdaterBrev(referanse, brev())
        }
    }

    @ParameterizedTest
    @EnumSource(
        ProsesseringStatus::class, mode = Mode.EXCLUDE, names = ["BREVBESTILLING_LØST"]
    )
    fun `validering feiler ved forsøk på oppdatering av brev i feil status`(status: ProsesseringStatus) {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)

            val referanse = brevbestillingService.opprettBestilling(
                saksnummer = randomSaksnummer(),
                behandlingReferanse = randomBehandlingReferanse(),
                unikReferanse = randomUnikReferanse(),
                brevtype = randomBrevtype(),
                språk = randomSpråk(),
                vedlegg = emptySet(),
            ).referanse

            brevbestillingRepository.oppdaterProsesseringStatus(referanse, status)

            val exception = assertThrows<ValideringsfeilException> {
                brevbestillingService.oppdaterBrev(referanse, brev())
            }
            assertThat(exception.message).endsWith(
                "Forsøkte å oppdatere brev i bestilling med prosesseringStatus=$status"
            )

        }
    }
}