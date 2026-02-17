package no.nav.aap.brev.bestilling

import no.nav.aap.brev.IntegrationTest
import no.nav.aap.brev.bestilling.Brevdata.Faktagrunnlag
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

    @Test
    fun `oppdaterer brevdata i riktig status`() {
        val bestilling = opprettBrevbestilling(
            brukV3 = true,
            ferdigstillAutomatisk = false,
        ).brevbestilling
        assertThat(bestilling.status).isEqualTo(Status.UNDER_ARBEID)
        oppdaterBrevdata(
            bestilling.referanse, brevdata = Brevdata(
                delmaler = emptyList(),
                faktagrunnlag = emptyList(),
                periodetekster = emptyList(),
                valg = emptyList(),
                betingetTekst = emptyList(),
                fritekster = emptyList(),
            )
        )
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

    @ParameterizedTest
    @EnumSource(
        Status::class, mode = Mode.EXCLUDE, names = ["UNDER_ARBEID"]
    )
    fun `validering feiler ved forsøk på oppdatering av brevdata i feil status`(status: Status) {
        val bestilling = opprettBrevbestilling(
            brukV3 = true,
            ferdigstillAutomatisk = false,
        ).brevbestilling
        oppdaterStatus(bestilling.id, status)

        val exception = assertThrows<ValideringsfeilException> {
            oppdaterBrevdata(
                bestilling.referanse, brevdata = Brevdata(
                    delmaler = emptyList(),
                    faktagrunnlag = emptyList(),
                    periodetekster = emptyList(),
                    valg = emptyList(),
                    betingetTekst = emptyList(),
                    fritekster = emptyList(),
                )
            )
        }
        assertThat(exception.message).endsWith(
            "Forsøkte å oppdatere brev i bestilling med status=$status"
        )
    }

    @Test
    fun `validering feiler ved forsøk på å oppdatere faktagrunnlag i brevdata`() {
        val bestilling = opprettBrevbestilling(
            brukV3 = true,
            ferdigstillAutomatisk = false,
        ).brevbestilling

        val exception = assertThrows<ValideringsfeilException> {
            oppdaterBrevdata(
                bestilling.referanse, brevdata = Brevdata(
                    delmaler = emptyList(),
                    faktagrunnlag = listOf(Faktagrunnlag("tekniskNavn", "verdi")),
                    periodetekster = emptyList(),
                    valg = emptyList(),
                    betingetTekst = emptyList(),
                    fritekster = emptyList(),
                )
            )
        }
        assertThat(exception.message).isEqualTo("Kan ikke oppdatere faktagrunnlag")
    }
}
