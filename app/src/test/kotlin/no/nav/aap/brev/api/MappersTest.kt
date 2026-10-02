package no.nav.aap.brev.api

import no.nav.aap.brev.bestilling.Adresse
import no.nav.aap.brev.bestilling.Mottaker
import no.nav.aap.brev.bestilling.NavnOgAdresse
import no.nav.aap.brev.kontrakt.MottakerDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class MappersTest {

    @Test
    fun `tilMottaker mapper ident og identType for hovedmottaker`() {
        val bestillingReferanse = UUID.randomUUID()
        val dto = MottakerDto(
            ident = "12345678910",
            identType = no.nav.aap.brev.kontrakt.IdentType.FNR,
        )

        val mottaker = dto.tilMottaker(bestillingReferanse, Mottaker.Type.HOVED, 0)

        assertThat(mottaker.ident).isEqualTo("12345678910")
        assertThat(mottaker.identType).isEqualTo(no.nav.aap.brev.bestilling.IdentType.FNR)
        assertThat(mottaker.type).isEqualTo(Mottaker.Type.HOVED)
        assertThat(mottaker.bestillingMottakerReferanse).isEqualTo("$bestillingReferanse-1")
    }

    @Test
    fun `tilMottaker mapper navnOgAdresse for kopimottaker`() {
        val bestillingReferanse = UUID.randomUUID()
        val dto = MottakerDto(
            navnOgAdresse = no.nav.aap.brev.kontrakt.NavnOgAdresse(
                navn = "verge",
                adresse = no.nav.aap.brev.kontrakt.Adresse(
                    landkode = "NO",
                    adresselinje1 = "Gateveien 1",
                    postnummer = "1234",
                    poststed = "Oslo",
                )
            )
        )

        val mottaker = dto.tilMottaker(bestillingReferanse, Mottaker.Type.KOPI, 1)

        assertThat(mottaker.ident).isNull()
        assertThat(mottaker.identType).isNull()
        assertThat(mottaker.navnOgAdresse?.navn).isEqualTo("verge")
        assertThat(mottaker.type).isEqualTo(Mottaker.Type.KOPI)
        assertThat(mottaker.bestillingMottakerReferanse).isEqualTo("$bestillingReferanse-2")
    }

    @Test
    fun `tilMottaker feiler for en tom mottaker uten ident eller navnOgAdresse`() {
        val dto = MottakerDto()

        assertThrows<IllegalArgumentException> {
            dto.tilMottaker(UUID.randomUUID(), Mottaker.Type.HOVED, 0)
        }
    }

    @Test
    fun `tilMottakerOgKopimottaker deler opp mottakerliste basert på type`() {
        val bestillingReferanse = UUID.randomUUID().toString()
        val hovedmottaker = Mottaker(
            ident = "12345678910",
            identType = no.nav.aap.brev.bestilling.IdentType.FNR,
            bestillingMottakerReferanse = "$bestillingReferanse-1",
            type = Mottaker.Type.HOVED,
        )
        val kopimottaker = Mottaker(
            navnOgAdresse = NavnOgAdresse(
                navn = "verge",
                adresse = Adresse(
                    landkode = "NO",
                    adresselinje1 = "Gateveien 1",
                    postnummer = "1234",
                    poststed = "Oslo",
                )
            ),
            bestillingMottakerReferanse = "$bestillingReferanse-2",
            type = Mottaker.Type.KOPI,
        )

        val (mottaker, kopi) = listOf(hovedmottaker, kopimottaker).tilMottakerOgKopimottaker()

        assertThat(mottaker?.ident).isEqualTo("12345678910")
        assertThat(kopi?.navnOgAdresse?.navn).isEqualTo("verge")
    }

    @Test
    fun `tilMottakerOgKopimottaker returnerer null for manglende hoved- eller kopimottaker`() {
        val bestillingReferanse = UUID.randomUUID().toString()
        val hovedmottaker = Mottaker(
            ident = "12345678910",
            identType = no.nav.aap.brev.bestilling.IdentType.FNR,
            bestillingMottakerReferanse = "$bestillingReferanse-1",
            type = Mottaker.Type.HOVED,
        )

        val (mottaker, kopi) = listOf(hovedmottaker).tilMottakerOgKopimottaker()

        assertThat(mottaker).isNotNull()
        assertThat(kopi).isNull()
    }
}
