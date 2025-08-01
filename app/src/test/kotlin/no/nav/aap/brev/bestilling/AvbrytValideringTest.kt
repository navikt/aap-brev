package no.nav.aap.brev.bestilling

import no.nav.aap.brev.IntegrationTest
import no.nav.aap.brev.exception.ValideringsfeilException
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.prosessering.ProsesseringStatus
import no.nav.aap.komponenter.dbconnect.transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode

class AvbrytValideringTest : IntegrationTest() {

    @Test
    fun `avbryt går igjennom fra gydlig status`() {
        val referanse =
            gittBrevMed(status = Status.UNDER_ARBEID)
        avbryt(referanse)
        assertStatus(referanse, Status.AVBRUTT, ProsesseringStatus.AVBRUTT)
    }

    @ParameterizedTest
    @EnumSource(Status::class, mode = Mode.EXCLUDE, names = ["UNDER_ARBEID"])
    fun `avbryt feiler fra andre statuser enn UNDER_ARBEID`(status: Status) {
        val referanse = gittBrevMed(status = status)
        val exception = assertThrows<ValideringsfeilException> {
            avbryt(referanse)
        }
        assertThat(exception.message).endsWith(
            "Kan ikke avbryte brevbestilling med status $status"
        )
        assertStatus(referanse, status, null)
    }

    private fun gittBrevMed(
        status: Status,
    ): BrevbestillingReferanse {
        return dataSource.transaction { connection ->
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)

            val bestilling = opprettBrevbestilling(
                ferdigstillAutomatisk = false,
            ).brevbestilling

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
        forventetProsesseringStatus: ProsesseringStatus?
    ) {
        return dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val bestilling = brevbestillingService.hent(referanse)

            assertThat(bestilling.status).isEqualTo(forventetStatus)
            assertThat(bestilling.prosesseringStatus).isEqualTo(forventetProsesseringStatus)
        }
    }
}
