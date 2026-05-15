package no.nav.aap.brev.innhold

import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.util.NumberUtils.formater
import no.nav.aap.brev.util.TimeUtils.formaterFullLengde
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class TabellerServiceTest {

    private val service = TabellerService()

    @Test
    fun `YrkesskadeBeregning - yrkesskader mappes til tabell med korrekte kolonner`() {
        val faktagrunnlag = setOf(
            Faktagrunnlag.YrkesskadeBeregning(
                yrkesskader = listOf(
                    Faktagrunnlag.YrkesskadeBeregning.Yrkesskade(
                        yrkesskadedato = LocalDate.of(2020, 3, 15),
                        arbeidsinntektPaaSkadetidspunktet = BigDecimal("450000"),
                        relevantForArbeidsevne = true,
                        diagnose = "M54",
                    )
                ),
                andelAvNedsettelseSomSkyldesYrkesskade = 70,
            )
        )

        val tabeller = service.faktagrunnlagTilTabeller(faktagrunnlag, Språk.NB)

        assertThat(tabeller).hasSize(1)
        val tabell = tabeller.single()
        assertThat(tabell.tekniskNavn).isEqualTo("ALLE_YRKESSKADER")
        assertThat(tabell.rader).hasSize(1)
        val celler = tabell.rader.single().celler
        assertThat(celler.first { it.kolonne == "YRKESSKADEDATO" }.verdi)
            .isEqualTo(LocalDate.of(2020, 3, 15).formaterFullLengde(Språk.NB))
        assertThat(celler.first { it.kolonne == "ARBEIDSINNTEKT" }.verdi)
            .isEqualTo(BigDecimal("450000").formater(Språk.NB))
        assertThat(celler.first { it.kolonne == "RELEVANT_FOR_ARBEIDSEVNE" }.verdi)
            .isEqualTo("Ja")
        assertThat(celler.first { it.kolonne == "DIAGNOSE" }.verdi).isEqualTo("M54")
    }

    @Test
    fun `YrkesskadeBeregning - to yrkesskader gir to rader`() {
        val faktagrunnlag = setOf(
            Faktagrunnlag.YrkesskadeBeregning(
                yrkesskader = listOf(
                    Faktagrunnlag.YrkesskadeBeregning.Yrkesskade(
                        yrkesskadedato = LocalDate.of(2018, 6, 1),
                        arbeidsinntektPaaSkadetidspunktet = BigDecimal("300000"),
                        relevantForArbeidsevne = true,
                        diagnose = null,
                    ),
                    Faktagrunnlag.YrkesskadeBeregning.Yrkesskade(
                        yrkesskadedato = LocalDate.of(2020, 3, 15),
                        arbeidsinntektPaaSkadetidspunktet = BigDecimal("450000"),
                        relevantForArbeidsevne = false,
                        diagnose = null,
                    ),
                ),
                andelAvNedsettelseSomSkyldesYrkesskade = 70,
            )
        )

        val tabeller = service.faktagrunnlagTilTabeller(faktagrunnlag, Språk.NB)

        assertThat(tabeller).hasSize(1)
        assertThat(tabeller.single().rader).hasSize(2)
    }

    @Test
    fun `YrkesskadeBeregning - tom liste gir ingen tabell`() {
        val faktagrunnlag = setOf(
            Faktagrunnlag.YrkesskadeBeregning(
                yrkesskader = emptyList(),
                andelAvNedsettelseSomSkyldesYrkesskade = 70,
            )
        )

        val tabeller = service.faktagrunnlagTilTabeller(faktagrunnlag, Språk.NB)

        assertThat(tabeller).isEmpty()
    }
}
