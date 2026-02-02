package no.nav.aap.brev.innhold

import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.kontrakt.BlokkInnhold
import no.nav.aap.brev.kontrakt.BlokkInnhold.FormattertTekst
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.util.NumberUtils.formater
import no.nav.aap.brev.util.TimeUtils.formaterFullLengde
import no.nav.aap.komponenter.dbconnect.DBConnection

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
    ): BlokkInnhold =
        when (blokkInnhold) {
            is BlokkInnhold.FormattertTekst -> blokkInnhold
            is BlokkInnhold.Faktagrunnlag ->
                blokkInnhold.kjentFaktagrunnlag()?.let { faktagrunnlagTekst[it] }
                    ?.let { blokkInnhold.tilFormattertTekst(it) }
                    ?: blokkInnhold
        }

    fun faktagrunnlagTilTekst(
        alleFaktagrunnlag: Set<Faktagrunnlag>,
        språk: Språk
    ): Map<KjentFaktagrunnlag, String> {
        return buildMap {
            alleFaktagrunnlag.forEach { faktagrunnlag ->
                when (faktagrunnlag) {

                    is Faktagrunnlag.AapFomDato ->
                        put(KjentFaktagrunnlag.AAP_FOM_DATO, faktagrunnlag.dato.formaterFullLengde(språk))

                    is Faktagrunnlag.KravdatoUføretrygd->
                        put(KjentFaktagrunnlag.KRAVDATO_UFORETRYGD, faktagrunnlag.dato.formaterFullLengde(språk))

                    is Faktagrunnlag.SisteDagMedYtelse ->
                        put(KjentFaktagrunnlag.SISTE_DAG_MED_YTELSE, faktagrunnlag.dato.formaterFullLengde(språk))

                    is Faktagrunnlag.DatoAvklartForJobbsøk->
                        put(KjentFaktagrunnlag.DATO_AVKLART_FOR_JOBBSOK, faktagrunnlag.dato.formaterFullLengde(språk))

                    is Faktagrunnlag.FristDato11_7 ->
                        put(KjentFaktagrunnlag.FRIST_DATO_11_7, faktagrunnlag.frist.formaterFullLengde(språk))

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
                                KjentFaktagrunnlag.GRADERT_DAGSATS,
                                gradertDagsats.formater(språk)
                            )
                        }
                        faktagrunnlag.barnetilleggSats?.let { barnetilleggSats ->
                            put(
                                KjentFaktagrunnlag.BARNETILLEGG_SATS,
                                barnetilleggSats.formater(språk)
                            )
                        }
                        faktagrunnlag.gradertBarnetillegg?.let { gradertBarnetillegg ->
                            put(
                                KjentFaktagrunnlag.GRADERT_BARNETILLEGG,
                                gradertBarnetillegg.formater(språk)
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
                                KjentFaktagrunnlag.BARNETILLEGG,
                                barnetillegg.formater(språk)
                            )
                        }
                        faktagrunnlag.antallBarn?.let { antallBarn ->
                            put(
                                KjentFaktagrunnlag.ANTALL_BARN,
                                antallBarn.toString()
                            )
                        }
                        faktagrunnlag.minsteÅrligYtelse?.let { minsteÅrligYtelse ->
                            put(
                                KjentFaktagrunnlag.MINSTE_AARLIG_YTELSE,
                                minsteÅrligYtelse.formater(språk)
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
                                KjentFaktagrunnlag.AARLIG_YTELSE,
                                årligYtelse.formater(språk)
                            )
                        }
                    }

                    is Faktagrunnlag.Sykdomsvurdering -> {
                        put(KjentFaktagrunnlag.SYKDOMSVURDERING, faktagrunnlag.begrunnelse)
                    }

                }
            }
        }
    }

    private fun BlokkInnhold.Faktagrunnlag.tilFormattertTekst(tekst: String): FormattertTekst {
        return FormattertTekst(
            id = id,
            tekst = tekst,
            formattering = emptyList()
        )
    }
}
