package no.nav.aap.brev.innhold

import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.Brevdata
import no.nav.aap.brev.kontrakt.BlokkInnhold
import no.nav.aap.brev.kontrakt.BlokkInnhold.FormattertTekst
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.util.NumberUtils.formater
import no.nav.aap.brev.util.TimeUtils.formaterFullLengde
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.LocalDate

class FaktagrunnlagService(
    private val brevbestillingRepository: BrevbestillingRepository,
) {
    companion object {
        fun konstruer(connection: DBConnection): FaktagrunnlagService {
            return FaktagrunnlagService(
                BrevbestillingRepositoryImpl(connection),
            )
        }
    }

    fun fyllInnFaktagrunnlag(brevbestillingReferanse: BrevbestillingReferanse, faktagrunnlag: Set<Faktagrunnlag>) {
        val bestilling = brevbestillingRepository.hent(brevbestillingReferanse)
        val brev = checkNotNull(bestilling.brev)

        val faktagrunnlagTekst = faktagrunnlagTilTekst(faktagrunnlag, bestilling.språk)
        val oppdatertBrev = brev.endreBlokkInnhold { erstattFaktagrunnlagMedTekst(it, faktagrunnlagTekst) }

        brevbestillingRepository.oppdaterBrev(bestilling.referanse, oppdatertBrev)
    }

    private fun erstattFaktagrunnlagMedTekst(
        blokkInnhold: BlokkInnhold,
        faktagrunnlagTekst: Map<KjentFaktagrunnlag, String>,
    ): BlokkInnhold = when (blokkInnhold) {
        is BlokkInnhold.FormattertTekst -> blokkInnhold
        is BlokkInnhold.Faktagrunnlag -> blokkInnhold.kjentFaktagrunnlag()?.let { faktagrunnlagTekst[it] }
            ?.let { blokkInnhold.tilFormattertTekst(it) } ?: blokkInnhold
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
                        if(samordningBarnepensjon.isNotEmpty()) {
                            add(tilTabell("SAMORDNING_BARNEPENSJON", samordningBarnepensjon))
                        }

                        val samordningAndreYtelser = faktagrunnlag.samordningAndreYtelser.map {
                            Brevdata.Tabell.Rad(tilCeller(it, språk))
                        }
                        if( samordningAndreYtelser.isNotEmpty()) {
                            add(tilTabell("SAMORDNING_ANDRE_YTELSER", samordningAndreYtelser))
                        }

                        val sykestipend = faktagrunnlag.sykestipend.map {
                            Brevdata.Tabell.Rad(tilCeller(it, språk))
                        }
                        if(sykestipend.isNotEmpty()){
                            add(tilTabell("SYKESTIPEND", sykestipend))
                        }

                        val fradragAndreYtelser = faktagrunnlag.fradragAndreYtelser.map {
                            Brevdata.Tabell.Rad(tilCeller(it, språk))
                        }
                        if(fradragAndreYtelser.isNotEmpty()){
                            add(tilTabell("FRADRAG_ANDRE_YTELSER", fradragAndreYtelser))
                        }

                    }

                    else -> {
                    }
                }
            }
        }
    }

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


    fun faktagrunnlagTilTekst(
        alleFaktagrunnlag: Set<Faktagrunnlag>, språk: Språk
    ): Map<KjentFaktagrunnlag, String> {
        return buildMap {
            alleFaktagrunnlag.forEach { faktagrunnlag ->
                when (faktagrunnlag) {

                    is Faktagrunnlag.AapFomDato -> put(
                        KjentFaktagrunnlag.AAP_FOM_DATO,
                        faktagrunnlag.dato.formaterFullLengde(språk)
                    )

                    is Faktagrunnlag.UtvidetAapFomDato -> put(
                        KjentFaktagrunnlag.UTVIDET_AAP_FOM_DATO,
                        faktagrunnlag.dato.formaterFullLengde(språk)
                    )

                    is Faktagrunnlag.KravdatoUføretrygd -> put(
                        KjentFaktagrunnlag.KRAVDATO_UFORETRYGD,
                        faktagrunnlag.dato.formaterFullLengde(språk)
                    )

                    is Faktagrunnlag.SisteDagMedYtelse -> put(
                        KjentFaktagrunnlag.SISTE_DAG_MED_YTELSE,
                        faktagrunnlag.dato.formaterFullLengde(språk)
                    )

                    is Faktagrunnlag.DatoAvklartForJobbsøk -> put(
                        KjentFaktagrunnlag.DATO_AVKLART_FOR_JOBBSOK,
                        faktagrunnlag.dato.formaterFullLengde(språk)
                    )

                    is Faktagrunnlag.FristDato11_7 -> put(
                        KjentFaktagrunnlag.FRIST_DATO_11_7,
                        faktagrunnlag.frist.formaterFullLengde(språk)
                    )

                    is Faktagrunnlag.GrunnlagBeregning -> {
                        faktagrunnlag.beregningstidspunkt?.let { beregningstidspunkt ->
                            put(KjentFaktagrunnlag.BEREGNINGSTIDSPUNKT, beregningstidspunkt.formaterFullLengde(språk))
                        }
                        faktagrunnlag.beregningsgrunnlag?.let { beregningsgrunnlag ->
                            put(KjentFaktagrunnlag.BEREGNINGSGRUNNLAG, beregningsgrunnlag.formater(språk))
                        }

                        val inntekterPerÅr = faktagrunnlag.inntekterPerÅr.sortedBy { it.år }
                        inntekterPerÅr.getOrNull(0)?.let {
                            put(KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_1_AARSTALL, it.år.toString())
                            put(KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_1_INNTEKT, it.inntekt.formater(språk))
                        }
                        inntekterPerÅr.getOrNull(1)?.let {
                            put(KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_2_AARSTALL, it.år.toString())
                            put(KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_2_INNTEKT, it.inntekt.formater(språk))
                        }
                        inntekterPerÅr.getOrNull(2)?.let {
                            put(KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_3_AARSTALL, it.år.toString())
                            put(KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_3_INNTEKT, it.inntekt.formater(språk))
                        }
                    }

                    is Faktagrunnlag.TilkjentYtelse -> {
                        faktagrunnlag.dagsats?.let { dagsats ->
                            put(KjentFaktagrunnlag.DAGSATS, dagsats.formater(språk))
                        }
                        faktagrunnlag.gradertDagsats?.let { gradertDagsats ->
                            put(
                                KjentFaktagrunnlag.GRADERT_DAGSATS, gradertDagsats.formater(språk)
                            )
                        }
                        faktagrunnlag.barnetilleggSats?.let { barnetilleggSats ->
                            put(
                                KjentFaktagrunnlag.BARNETILLEGG_SATS, barnetilleggSats.formater(språk)
                            )
                        }
                        faktagrunnlag.gradertBarnetillegg?.let { gradertBarnetillegg ->
                            put(
                                KjentFaktagrunnlag.GRADERT_BARNETILLEGG, gradertBarnetillegg.formater(språk)
                            )
                        }
                        faktagrunnlag.gradertDagsatsInkludertBarnetillegg?.let { gradertDagsatsInkludertBarnetillegg ->
                            put(
                                KjentFaktagrunnlag.GRADERT_DAGSATS_INKLUDERT_BARNETILLEGG,
                                gradertDagsatsInkludertBarnetillegg.formater(språk)
                            )
                        }
                        faktagrunnlag.barnetillegg?.let { barnetillegg ->
                            put(
                                KjentFaktagrunnlag.BARNETILLEGG, barnetillegg.formater(språk)
                            )
                        }
                        faktagrunnlag.antallBarn?.let { antallBarn ->
                            put(
                                KjentFaktagrunnlag.ANTALL_BARN, antallBarn.toString()
                            )
                        }
                        faktagrunnlag.minsteÅrligYtelse?.let { minsteÅrligYtelse ->
                            put(
                                KjentFaktagrunnlag.MINSTE_AARLIG_YTELSE, minsteÅrligYtelse.formater(språk)
                            )
                        }
                        faktagrunnlag.minsteÅrligYtelseUnder25?.let { minsteÅrligYtelseUnder25 ->
                            put(
                                KjentFaktagrunnlag.MINSTE_AARLIG_YTELSE_UNDER_25AAR,
                                minsteÅrligYtelseUnder25.formater(språk)
                            )
                        }
                        faktagrunnlag.årligYtelse?.let { årligYtelse ->
                            put(
                                KjentFaktagrunnlag.AARLIG_YTELSE, årligYtelse.formater(språk)
                            )
                        }
                    }

                    is Faktagrunnlag.Sykdomsvurdering -> {
                        put(KjentFaktagrunnlag.SYKDOMSVURDERING, faktagrunnlag.begrunnelse)
                    }

                    is Faktagrunnlag.ForholdTilAndreYtelser -> {
                        faktagrunnlag.refusjonskravTjenestepensjon?.let {
                            put(
                                KjentFaktagrunnlag.REFUSJONSKRAV_TJENESTEPENSJON,
                                refusjonskravTjenestepensjonTekst(it, språk)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun MutableMap<KjentFaktagrunnlag, String>.putHvisIkkeTom(
        key: KjentFaktagrunnlag, verdier: List<String>
    ) {
        if (verdier.isNotEmpty()) {
            put(key, verdier.joinToString(separator = "\n"))
        }
    }

    private fun periodeTilTekst(fraOgMed: LocalDate?, tilOgMed: LocalDate?, språk: Språk): String {
        return if (fraOgMed != null && tilOgMed != null) {
            "${fraOgMed.formaterFullLengde(språk)} - ${tilOgMed.formaterFullLengde(språk)}"
        } else if (fraOgMed != null) {
            "fra og med ${fraOgMed.formaterFullLengde(språk)}"
        } else if (tilOgMed != null) {
            "til og med ${tilOgMed.formaterFullLengde(språk)}"
        } else {
            ""
        }
    }

    private fun refusjonskravTjenestepensjonTekst(
        refusjonskravTjenestepensjon: Faktagrunnlag.ForholdTilAndreYtelser.RefusjonskravTjenestepensjon, språk: Språk
    ): String {
        return "Skal etterbetaling holdes igjen: ${if (refusjonskravTjenestepensjon.skalEtterbetalingHoldesIgjen) "Ja" else "Nei"}, " + periodeTilTekst(
            refusjonskravTjenestepensjon.fraOgMed,
            refusjonskravTjenestepensjon.tilOgMed,
            språk
        )
    }

    private fun BlokkInnhold.Faktagrunnlag.tilFormattertTekst(tekst: String): FormattertTekst {
        return FormattertTekst(
            id = id, tekst = tekst, formattering = emptyList()
        )
    }
}
