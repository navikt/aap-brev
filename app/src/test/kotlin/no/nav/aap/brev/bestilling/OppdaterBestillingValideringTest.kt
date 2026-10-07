package no.nav.aap.brev.bestilling

import no.nav.aap.brev.IntegrationTest
import no.nav.aap.brev.api.tilBrevdataDto
import no.nav.aap.brev.feil.ValideringsfeilException
import no.nav.aap.brev.kontrakt.BrevdataDto
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.test.fakes.brev
import no.nav.aap.komponenter.dbconnect.transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode

class OppdaterBestillingValideringTest : IntegrationTest() {
    @Test
    fun `bevarer opprinnelige systemvalg ved fjerning og endring av valg`() {
        val bestilling = opprettBrevbestilling(
            brukV3 = true,
            ferdigstillAutomatisk = false,
        ).brevbestilling
        val brevdata = checkNotNull(hentBestilling(bestilling.referanse).brevdata).copy(
            delmaler = listOf(Brevdata.Delmal("delmal")),
            valg = listOf(Brevdata.Valg("valg", "opprinnelig")),
            automatiskValgteDelmalIder = listOf("delmal"),
            automatiskValgteValg = listOf(Brevdata.Valg("valg", "opprinnelig")),
        )
        dataSource.transaction { connection ->
            BrevbestillingRepositoryImpl(connection).oppdaterBrevdata(bestilling.id, brevdata)
        }

        oppdaterBrevdata(
            bestilling.referanse,
            BrevdataDto(
                delmaler = emptyList(),
                valg = emptyList(),
                betingetTekst = emptyList(),
                fritekster = emptyList(),
                automatiskValgteDelmalIder = listOf("klientverdi"),
                automatiskValgteValg = listOf(BrevdataDto.Valg("klientverdi", "klientverdi")),
            )
        )

        val etterFjerning = checkNotNull(hentBestilling(bestilling.referanse).brevdata)
        assertThat(etterFjerning.delmaler).isEmpty()
        assertThat(etterFjerning.valg).isEmpty()
        assertThat(etterFjerning.automatiskValgteDelmalIder).isEqualTo(brevdata.automatiskValgteDelmalIder)
        assertThat(etterFjerning.automatiskValgteValg).isEqualTo(brevdata.automatiskValgteValg)
        assertThat(etterFjerning.tilBrevdataDto().automatiskValgteDelmalIder).containsExactly("delmal")
        assertThat(etterFjerning.tilBrevdataDto().automatiskValgteValg)
            .containsExactly(BrevdataDto.Valg("valg", "opprinnelig"))

        oppdaterBrevdata(
            bestilling.referanse,
            BrevdataDto(
                delmaler = listOf(BrevdataDto.Delmal("delmal"), BrevdataDto.Delmal("manuell")),
                valg = listOf(BrevdataDto.Valg("valg", "endret")),
                betingetTekst = emptyList(),
                fritekster = emptyList(),
            )
        )

        val etterEndring = checkNotNull(hentBestilling(bestilling.referanse).brevdata)
        assertThat(etterEndring.delmaler).containsExactly(Brevdata.Delmal("delmal"), Brevdata.Delmal("manuell"))
        assertThat(etterEndring.valg).containsExactly(Brevdata.Valg("valg", "endret"))
        assertThat(etterEndring.automatiskValgteDelmalIder).isEqualTo(brevdata.automatiskValgteDelmalIder)
        assertThat(etterEndring.automatiskValgteValg).isEqualTo(brevdata.automatiskValgteValg)
    }

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
            bestilling.referanse, dto = BrevdataDto(
                delmaler = emptyList(),
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
                bestilling.referanse, dto = BrevdataDto(
                    delmaler = emptyList(),
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
}
