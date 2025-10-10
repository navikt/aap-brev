package no.nav.aap.brev.bestilling

import no.nav.aap.brev.IntegrationTest
import no.nav.aap.brev.feil.ValideringsfeilException
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.test.fakes.brev
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode

class OppdaterBestillingValideringTest : IntegrationTest() {
    @Test
    fun `oppdaterer brev i riktig status`() {
        val bestilling = opprettBrevbestilling(
            ferdigstillAutomatisk = false,
        ).brevbestilling
        assertThat(bestilling.status).isEqualTo(Status.UNDER_ARBEID)
        oppdaterBrev(bestilling.referanse, brev())
    }

    @ParameterizedTest
    @EnumSource(
        Status::class, mode = Mode.EXCLUDE, names = ["UNDER_ARBEID"]
    )
    fun `validering feiler ved forsøk på oppdatering av brev i feil status`(status: Status) {
        val bestilling = opprettBrevbestilling(
            ferdigstillAutomatisk = false,
        ).brevbestilling
        oppdaterStatus(bestilling.id, status)

        val exception = assertThrows<ValideringsfeilException> {
            oppdaterBrev(bestilling.referanse, brev())
        }
        assertThat(exception.message).endsWith(
            "Forsøkte å oppdatere brev i bestilling med status=$status"
        )
    }
}