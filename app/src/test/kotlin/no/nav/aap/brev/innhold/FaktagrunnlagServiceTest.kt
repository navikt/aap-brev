package no.nav.aap.brev.innhold

import io.mockk.mockk
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.kontrakt.Språk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FaktagrunnlagServiceTest {

    private val service = FaktagrunnlagService(mockk<BrevbestillingRepository>(relaxed = true))

    @Test
    fun `YrkesskadeBeregning - andel av nedsettelsen formateres med prosenttegn`() {
        val faktagrunnlag = setOf(
            Faktagrunnlag.YrkesskadeBeregning(
                yrkesskader = emptyList(),
                andelAvNedsettelseSomSkyldesYrkesskade = 70,
            )
        )

        val resultat = service.faktagrunnlagTilTekst(faktagrunnlag, Språk.NB)

        assertThat(resultat[KjentFaktagrunnlag.YRKESSKADE_ANDEL_AV_NEDSETTELSEN]).isEqualTo("70")
    }

    @Test
    fun `YrkesskadeBeregning - null andel gir ingen innslag i map`() {
        val faktagrunnlag = setOf(
            Faktagrunnlag.YrkesskadeBeregning(
                yrkesskader = emptyList(),
                andelAvNedsettelseSomSkyldesYrkesskade = null,
            )
        )

        val resultat = service.faktagrunnlagTilTekst(faktagrunnlag, Språk.NB)

        assertThat(resultat).doesNotContainKey(KjentFaktagrunnlag.YRKESSKADE_ANDEL_AV_NEDSETTELSEN)
    }
}
