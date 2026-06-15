package no.nav.aap.brev.innhold

import no.nav.aap.brev.bestilling.Brevdata
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.util.NumberUtils.formater
import no.nav.aap.brev.util.TimeUtils.formaterFullLengde
import no.nav.aap.komponenter.miljo.Miljø
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.random.Random

/**
 * Bygger [no.nav.aap.brev.bestilling.Brevdata.Tabell]-strukturer fra faktagrunnlag-input.
 *
 * For faktagrunnlag som skal vises som inline tekst, se [FaktagrunnlagService].
 */
class TabellerService {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        fun konstruer(): TabellerService = TabellerService()
    }

    fun faktagrunnlagTilTabeller(alleFaktagrunnlag: Set<Faktagrunnlag>, språk: Språk): List<Brevdata.Tabell> {
        return buildList {
            alleFaktagrunnlag.forEach { faktagrunnlag ->
                when (faktagrunnlag) {
                    is Faktagrunnlag.ForholdTilAndreYtelser -> {
                        val samordningUføre = faktagrunnlag.samordningUføre.map {
                            Brevdata.Tabell.Rad(tilCeller(it, språk))
                        }
                        if (samordningUføre.isNotEmpty()) {
                            add(tilTabell("SAMORDNING_UFØRE", samordningUføre))
                        }

                        val reduksjonArbeidsgiver = faktagrunnlag.reduksjonArbeidsgiver.map {
                            Brevdata.Tabell.Rad(tilCeller(it, språk))
                        }
                        if (reduksjonArbeidsgiver.isNotEmpty()) {
                            add(tilTabell("REDUKSJON_ARBEIDSGIVER", reduksjonArbeidsgiver))
                        }

                        val samordningBarnepensjon = faktagrunnlag.samordningBarnepensjon.map {
                            Brevdata.Tabell.Rad(tilCeller(it, språk))
                        }
                        if (samordningBarnepensjon.isNotEmpty()) {
                            add(tilTabell("SAMORDNING_BARNEPENSJON", samordningBarnepensjon))
                        }

                        val samordningAndreYtelser = faktagrunnlag.samordningAndreYtelser.map {
                            Brevdata.Tabell.Rad(tilCeller(it, språk))
                        }
                        if (samordningAndreYtelser.isNotEmpty()) {
                            add(tilTabell("SAMORDNING_ANDRE_YTELSER", samordningAndreYtelser))
                        }

                        val sykestipend = faktagrunnlag.sykestipend.map {
                            Brevdata.Tabell.Rad(tilCeller(it, språk))
                        }
                        if (sykestipend.isNotEmpty()) {
                            add(tilTabell("SYKESTIPEND", sykestipend))
                        }

                        val fradragAndreYtelser = faktagrunnlag.fradragAndreYtelser.map {
                            Brevdata.Tabell.Rad(tilCeller(it, språk))
                        }
                        if (fradragAndreYtelser.isNotEmpty()) {
                            add(tilTabell("FRADRAG_ANDRE_YTELSER", fradragAndreYtelser))
                        }
                    }

                    is Faktagrunnlag.YrkesskadeBeregning -> {
                        val sortert = sorterEtterRelevansOgDato(faktagrunnlag)
                        val yrkesskader = sortert.map {
                            Brevdata.Tabell.Rad(tilCeller(it, språk))
                        }
                        if (yrkesskader.isNotEmpty()) {
                            add(tilTabell("ALLE_YRKESSKADER", yrkesskader))
                        }
                    }

                    else -> {}
                }
            }

            if (alleFaktagrunnlag.none { it is Faktagrunnlag.YrkesskadeBeregning } && Miljø.erDev()) {
                add(tilTabell(
                    "ALLE_YRKESSKADER",
                    sorterEtterRelevansOgDato(fakeYrkesskader).map {
                        Brevdata.Tabell.Rad(tilCeller(it, språk))
                    }
                )
                )
            }
        }
    }

    private fun sorterEtterRelevansOgDato(faktagrunnlag: Faktagrunnlag.YrkesskadeBeregning): List<Faktagrunnlag.YrkesskadeBeregning.Yrkesskade> =
        faktagrunnlag.yrkesskader.partition { it.relevantForArbeidsevne }.let {
            it.first.sortedBy { it.yrkesskadedato } + it.second.sortedBy { it.yrkesskadedato }
        }

    /**
     * TODO
     * Dette er _kun_ for å teste yrkesskade i brevbygger i dev.
     * Legges bak env-sjekk og fjernes når ferdig utviklet.
     */
    val fakeYrkesskader =
        Faktagrunnlag.YrkesskadeBeregning(
            yrkesskader =
                listOf(
                    Faktagrunnlag.YrkesskadeBeregning.Yrkesskade(
                        yrkesskadedato = LocalDate.of(2026, 1, 1),
                        arbeidsinntektPaaSkadetidspunktet = (Random.nextInt(300000) + 200000).toBigDecimal(),
                        diagnose = "Personlighetsforstyrrelse",
                        relevantForArbeidsevne = true,
                    ),
                    Faktagrunnlag.YrkesskadeBeregning.Yrkesskade(
                        yrkesskadedato = LocalDate.of(2025, 1, 1),
                        arbeidsinntektPaaSkadetidspunktet = (Random.nextInt(300000) + 200000).toBigDecimal(),
                        diagnose = "sleten",
                        relevantForArbeidsevne = false,
                    ),
                    Faktagrunnlag.YrkesskadeBeregning.Yrkesskade(
                        yrkesskadedato = LocalDate.of(2024, 1, 1),
                        arbeidsinntektPaaSkadetidspunktet = (Random.nextInt(300000) + 200000).toBigDecimal(),
                        diagnose = "vondt i bcg",
                        relevantForArbeidsevne = true,
                    )
                ),
            andelAvNedsettelseSomSkyldesYrkesskade = (Random.nextInt(40) + 60)

        )

    private fun tilTabell(tekniskNavn: String, rader: List<Brevdata.Tabell.Rad>) =
        Brevdata.Tabell(
            tekniskNavn = tekniskNavn,
            rader = rader
        )

    private fun tilCeller(
        samordningUføre: Faktagrunnlag.ForholdTilAndreYtelser.SamordningUføre,
        språk: Språk
    ): List<Brevdata.Tabell.Rad.Celle> = listOf(
        Brevdata.Tabell.Rad.Celle(
            kolonne = "VIRKNINGSTIDSPUNKT",
            verdi = samordningUføre.virkningstidspunkt.formaterFullLengde(språk)
        ),
        Brevdata.Tabell.Rad.Celle(
            kolonne = "UFØREGRAD",
            verdi = "${samordningUføre.uføregradProsent}%"
        )
    )

    private fun tilCeller(
        reduksjonArbeidsgiver: Faktagrunnlag.ForholdTilAndreYtelser.ReduksjonArbeidsgiver,
        språk: Språk
    ): List<Brevdata.Tabell.Rad.Celle> = listOf(
        Brevdata.Tabell.Rad.Celle(
            kolonne = "FRA_OG_MED",
            verdi = reduksjonArbeidsgiver.fraOgMed.formaterFullLengde(språk)
        ),
        Brevdata.Tabell.Rad.Celle(
            kolonne = "TIL_OG_MED",
            verdi = reduksjonArbeidsgiver.tilOgMed.formaterFullLengde(språk)
        )
    )

    private fun tilCeller(
        samordningBarnepensjon: Faktagrunnlag.ForholdTilAndreYtelser.SamordningBarnepensjon,
        språk: Språk
    ): List<Brevdata.Tabell.Rad.Celle> = listOf(
        Brevdata.Tabell.Rad.Celle(
            kolonne = "FRA_OG_MED",
            verdi = samordningBarnepensjon.fraOgMed.formaterFullLengde(språk)
        ),
        Brevdata.Tabell.Rad.Celle(
            kolonne = "TIL_OG_MED",
            verdi = samordningBarnepensjon.tilOgMed?.formaterFullLengde(språk) ?: ""
        ),
        Brevdata.Tabell.Rad.Celle(
            kolonne = "MÅNEDSATS",
            verdi = "${samordningBarnepensjon.månedsats.formater(språk)} Kroner per måned"
        )
    )

    private fun tilCeller(
        samordningYtelse: Faktagrunnlag.ForholdTilAndreYtelser.SamordningYtelse,
        språk: Språk
    ): List<Brevdata.Tabell.Rad.Celle> = listOf(
        Brevdata.Tabell.Rad.Celle(
            kolonne = "YTELSE_NAVN",
            verdi = samordningYtelse.ytelseNavn
        ),
        Brevdata.Tabell.Rad.Celle(
            kolonne = "FRA_OG_MED",
            verdi = samordningYtelse.fraOgMed.formaterFullLengde(språk)
        ),
        Brevdata.Tabell.Rad.Celle(
            kolonne = "TIL_OG_MED",
            verdi = samordningYtelse.tilOgMed.formaterFullLengde(språk)
        ),
        Brevdata.Tabell.Rad.Celle(
            kolonne = "GRADERING",
            verdi = "${samordningYtelse.gradering}%"
        )
    )

    private fun tilCeller(
        sykestipend: Faktagrunnlag.ForholdTilAndreYtelser.Sykestipend,
        språk: Språk
    ): List<Brevdata.Tabell.Rad.Celle> = listOf(
        Brevdata.Tabell.Rad.Celle(
            kolonne = "FRA_OG_MED",
            verdi = sykestipend.fraOgMed.formaterFullLengde(språk)
        ),
        Brevdata.Tabell.Rad.Celle(
            kolonne = "TIL_OG_MED",
            verdi = sykestipend.tilOgMed.formaterFullLengde(språk)
        )
    )

    private fun tilCeller(
        yrkesskade: Faktagrunnlag.YrkesskadeBeregning.Yrkesskade,
        språk: Språk,
    ): List<Brevdata.Tabell.Rad.Celle> = listOfNotNull<Brevdata.Tabell.Rad.Celle>(
        Brevdata.Tabell.Rad.Celle(
            kolonne = "YRKESSKADEDATO",
            verdi = yrkesskade.yrkesskadedato.formaterFullLengde(språk)
        ),
        yrkesskade.arbeidsinntektPaaSkadetidspunktet?.let {
            Brevdata.Tabell.Rad.Celle(
                kolonne = "ARBEIDSINNTEKT",
                verdi = it.formater(språk)
            )
        },
        Brevdata.Tabell.Rad.Celle(
            kolonne = "RELEVANT_FOR_ARBEIDSEVNE",
            verdi = if (yrkesskade.relevantForArbeidsevne) "Ja" else "Nei"
        ),
        yrkesskade.diagnose?.let {
            Brevdata.Tabell.Rad.Celle(kolonne = "DIAGNOSE", verdi = it)
        },
    )

    private fun tilCeller(
        fradragYtelse: Faktagrunnlag.ForholdTilAndreYtelser.FradragYtelse,
        språk: Språk
    ): List<Brevdata.Tabell.Rad.Celle> = listOf(
        Brevdata.Tabell.Rad.Celle(
            kolonne = "YTELSE_NAVN",
            verdi = fradragYtelse.ytelseNavn
        ),
        Brevdata.Tabell.Rad.Celle(
            kolonne = "FRA_OG_MED",
            verdi = fradragYtelse.fraOgMed.formaterFullLengde(språk)
        ),
        Brevdata.Tabell.Rad.Celle(
            kolonne = "TIL_OG_MED",
            verdi = fradragYtelse.tilOgMed.formaterFullLengde(språk)
        )
    )
}
