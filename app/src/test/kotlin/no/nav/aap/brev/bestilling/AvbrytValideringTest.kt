package no.nav.aap.brev.bestilling

import no.nav.aap.brev.exception.ValideringsfeilException
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.prosessering.ProsesseringStatus
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.brev.test.randomBrevtype
import no.nav.aap.brev.test.randomBrukerIdent
import no.nav.aap.brev.test.randomSaksnummer
import no.nav.aap.brev.test.randomSpråk
import no.nav.aap.brev.test.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode

class AvbrytValideringTest {

    companion object {

        private val dataSource = InitTestDatabase.freshDatabase()

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            Fakes.start()
        }
    }

    @Test
    fun `avbryt går igjennom fra gydlig status`() {
        val referanse =
            gittBrevMed(status = Status.UNDER_ARBEID, prosesseringStatus = ProsesseringStatus.BREVBESTILLING_LØST)
        avbryt(referanse)
        assertStatus(referanse, Status.AVBRUTT, ProsesseringStatus.AVBRUTT)
    }

    @ParameterizedTest
    @EnumSource(ProsesseringStatus::class, mode = Mode.EXCLUDE, names = ["BREVBESTILLING_LØST"])
    fun `avbryt feiler fra andre statuser enn BREVBESTILLING_LØST`(status: ProsesseringStatus) {
        val referanse = gittBrevMed(status = Status.FERDIGSTILT, prosesseringStatus = status)
        val exception = assertThrows<ValideringsfeilException> {
            avbryt(referanse)
        }
        assertThat(exception.message).endsWith(
            "Kan ikke avbryte brevbestilling med status $status"
        )
        assertStatus(referanse, Status.FERDIGSTILT, status)
    }

    private fun gittBrevMed(
        status: Status,
        prosesseringStatus: ProsesseringStatus
    ): BrevbestillingReferanse {
        return dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)

            val bestilling = brevbestillingService.opprettBestillingV2(
                saksnummer = randomSaksnummer(),
                brukerIdent = randomBrukerIdent(),
                behandlingReferanse = randomBehandlingReferanse(),
                unikReferanse = randomUnikReferanse(),
                brevtype = randomBrevtype(),
                språk = randomSpråk(),
                faktagrunnlag = emptySet(),
                vedlegg = emptySet(),
                ferdigstillAutomatisk = false,
            ).brevbestilling

            brevbestillingRepository.oppdaterProsesseringStatus(bestilling.referanse, prosesseringStatus)
            brevbestillingRepository.oppdaterStatus(bestilling.id, status)

            bestilling.referanse
        }
    }

    private fun avbryt(referanse: BrevbestillingReferanse) {
        dataSource.transaction { connection ->
            BrevbestillingService.konstruer(connection).avbryt(referanse)
        }
    }

    private fun assertStatus(
        referanse: BrevbestillingReferanse,
        forventetStatus: Status,
        forventetProsesseringStatus: ProsesseringStatus
    ) {
        return dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val bestilling = brevbestillingService.hent(referanse)

            assertThat(bestilling.status).isEqualTo(forventetStatus)
            assertThat(bestilling.prosesseringStatus).isEqualTo(forventetProsesseringStatus)
        }
    }
}
