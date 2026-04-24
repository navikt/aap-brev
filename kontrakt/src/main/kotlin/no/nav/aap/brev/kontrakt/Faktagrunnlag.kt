package no.nav.aap.brev.kontrakt

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.annotation.JsonValue
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year

const val FAKTAGRUNNLAG_TYPE_AAP_FOM_DATO: String = "AAP_FOM_DATO"
const val FAKTAGRUNNLAG_TYPE_KRAVDATO_UFORETRYGD: String = "KRAVDATO_UFORETRYGD"
const val FAKTAGRUNNLAG_TYPE_SISTE_DAG_MED_YTELSE: String = "SISTE_DAG_MED_YTELSE"
const val FAKTAGRUNNLAG_TYPE_DATO_AVKLART_FOR_JOBBSOK: String = "DATO_AVKLART_FOR_JOBBSOK"
const val FAKTAGRUNNLAG_TYPE_UTVIDET_AAP_FOM_DATO: String = "UTVIDET_AAP_FOM_DATO"
const val FAKTAGRUNNLAG_TYPE_FRIST_DATO_11_7: String = "FRIST_DATO_11_7"
const val FAKTAGRUNNLAG_TYPE_GRUNNLAG_BEREGNING: String = "GRUNNLAG_BEREGNING"
const val FAKTAGRUNNLAG_TYPE_TILKJENT_YTELSE: String = "TILKJENT_YTELSE"
const val FAKTAGRUNNLAG_TYPE_SYKDOMSVURDERING: String = "SYKDOMSVURDERING"
const val FAKTAGRUNNLAG_TYPE_SAMORDNING_ANDRE_YTELSER: String = "SAMORDNING_ANDRE_YTELSER"
const val FAKTAGRUNNLAG_TYPE_SAMORDNING_UFORE: String = "SAMORDNING_UFORE"
const val FAKTAGRUNNLAG_TYPE_SAMORDNING_ARBEIDSGIVER: String = "SAMORDNING_ARBEIDSGIVER"
const val FAKTAGRUNNLAG_TYPE_SAMORDNING_TJENESTEPENSJON: String = "SAMORDNING_TJENESTEPENSJON"
const val FAKTAGRUNNLAG_TYPE_SAMORDNING_SYKESTIPEND: String = "SAMORDNING_SYKESTIPEND"
const val FAKTAGRUNNLAG_TYPE_SAMORDNING_BARNEPENSJON: String = "SAMORDNING_BARNEPENSJON"
const val FAKTAGRUNNLAG_TYPE_SAMORDNING_FRADRAG_ANDRE_YTELSER: String = "SAMORDNING_FRADRAG_ANDRE_YTELSER"

enum class FaktagrunnlagType(@JsonValue val verdi: String) {
    AAP_FOM_DATO(FAKTAGRUNNLAG_TYPE_AAP_FOM_DATO),
    KRAVDATO_UFORETRYGD(FAKTAGRUNNLAG_TYPE_KRAVDATO_UFORETRYGD),
    SISTE_DAG_MED_YTELSE(FAKTAGRUNNLAG_TYPE_SISTE_DAG_MED_YTELSE),
    DATO_AVKLART_FOR_JOBBSOK(FAKTAGRUNNLAG_TYPE_DATO_AVKLART_FOR_JOBBSOK),
    UTVIDET_AAP_FOM_DATO(FAKTAGRUNNLAG_TYPE_UTVIDET_AAP_FOM_DATO),
    FRIST_DATO_11_7(FAKTAGRUNNLAG_TYPE_FRIST_DATO_11_7),
    GRUNNLAG_BEREGNING(FAKTAGRUNNLAG_TYPE_GRUNNLAG_BEREGNING),
    TILKJENT_YTELSE(FAKTAGRUNNLAG_TYPE_TILKJENT_YTELSE),
    SYKDOMSVURDERING(FAKTAGRUNNLAG_TYPE_SYKDOMSVURDERING),
    SAMORDNING_ANDRE_YTELSER(FAKTAGRUNNLAG_TYPE_SAMORDNING_ANDRE_YTELSER),
    SAMORDNING_UFORE(FAKTAGRUNNLAG_TYPE_SAMORDNING_UFORE),
    SAMORDNING_ARBEIDSGIVER(FAKTAGRUNNLAG_TYPE_SAMORDNING_ARBEIDSGIVER),
    SAMORDNING_TJENESTEPENSJON(FAKTAGRUNNLAG_TYPE_SAMORDNING_TJENESTEPENSJON),
    SAMORDNING_SYKESTIPEND(FAKTAGRUNNLAG_TYPE_SAMORDNING_SYKESTIPEND),
    SAMORDNING_BARNEPENSJON(FAKTAGRUNNLAG_TYPE_SAMORDNING_BARNEPENSJON),
    SAMORDNING_FRADRAG_ANDRE_YTELSER(FAKTAGRUNNLAG_TYPE_SAMORDNING_FRADRAG_ANDRE_YTELSER),
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
sealed class Faktagrunnlag(val type: FaktagrunnlagType) {
    @JsonTypeName(FAKTAGRUNNLAG_TYPE_AAP_FOM_DATO)
    data class AapFomDato(
        val dato: LocalDate
    ) : Faktagrunnlag(FaktagrunnlagType.AAP_FOM_DATO)

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_KRAVDATO_UFORETRYGD)
    data class KravdatoUføretrygd(
        val dato: LocalDate
    ) : Faktagrunnlag(FaktagrunnlagType.KRAVDATO_UFORETRYGD)

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_SISTE_DAG_MED_YTELSE)
    data class SisteDagMedYtelse(
        val dato: LocalDate
    ) : Faktagrunnlag(FaktagrunnlagType.SISTE_DAG_MED_YTELSE)

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_DATO_AVKLART_FOR_JOBBSOK)
    data class DatoAvklartForJobbsøk(
        val dato: LocalDate
    ) : Faktagrunnlag(FaktagrunnlagType.DATO_AVKLART_FOR_JOBBSOK)

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_UTVIDET_AAP_FOM_DATO)
    data class UtvidetAapFomDato(
        val dato: LocalDate
    ) : Faktagrunnlag(FaktagrunnlagType.UTVIDET_AAP_FOM_DATO)

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_FRIST_DATO_11_7)
    data class FristDato11_7(
        val frist: LocalDate
    ) : Faktagrunnlag(FaktagrunnlagType.FRIST_DATO_11_7)

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_TILKJENT_YTELSE)
    data class TilkjentYtelse(
        val dagsats: BigDecimal?,
        val gradertDagsats: BigDecimal?,
        val barnetilleggSats: BigDecimal?,
        val gradertBarnetillegg: BigDecimal?,
        val gradertDagsatsInkludertBarnetillegg: BigDecimal?,
        val barnetillegg: BigDecimal?,
        val antallBarn: Int?,
        val minsteÅrligYtelse: BigDecimal?,
        val minsteÅrligYtelseUnder25: BigDecimal?,
        val årligYtelse: BigDecimal?,
    ) : Faktagrunnlag(FaktagrunnlagType.TILKJENT_YTELSE)

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_GRUNNLAG_BEREGNING)
    data class GrunnlagBeregning(
        val beregningstidspunkt: LocalDate?,
        val beregningsgrunnlag: BigDecimal?,
        val inntekterPerÅr: List<InntektPerÅr>
    ) : Faktagrunnlag(FaktagrunnlagType.GRUNNLAG_BEREGNING) {
        data class InntektPerÅr(val år: Year, val inntekt: BigDecimal)
    }

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_SYKDOMSVURDERING)
    data class Sykdomsvurdering(
        val begrunnelse: String,
    ) : Faktagrunnlag(FaktagrunnlagType.SYKDOMSVURDERING)

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_SAMORDNING_ANDRE_YTELSER)
    data class SamordningerAndreYtelser(
        val samordninger: List<SamordningAnnenYtelse>,
    ) : Faktagrunnlag(FaktagrunnlagType.SAMORDNING_ANDRE_YTELSER) {

        data class SamordningAnnenYtelse(
            val ytelseNavn: String,
            val gradering: BigDecimal?,
            val fraOgMed: LocalDate,
            val tilOgMed: LocalDate,
        )
    }

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_SAMORDNING_UFORE)
    data class SamordningerUføre(
        val samordninger: List<SamordningUføre>,
    ) : Faktagrunnlag(FaktagrunnlagType.SAMORDNING_UFORE) {

        data class SamordningUføre(
            val virkningstidspunkt: LocalDate,
            val uføregradTilSamordning: BigDecimal,
        )
    }

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_SAMORDNING_ARBEIDSGIVER)
    data class SamordningerArbeidsgiver(
        val samordninger: List<SamordningArbeidsgiver>,
    ) : Faktagrunnlag(FaktagrunnlagType.SAMORDNING_ARBEIDSGIVER) {

        data class SamordningArbeidsgiver(
            val fraOgMed: LocalDate,
            val tilOgMed: LocalDate,
        )
    }

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_SAMORDNING_TJENESTEPENSJON)
    data class SamordningTjenestepensjon(
        val skalEtterbetalingHoldesIgjen: Boolean,
        val fraOgMed: LocalDate?,
        val tilOgMed: LocalDate?,
    ) : Faktagrunnlag(FaktagrunnlagType.SAMORDNING_TJENESTEPENSJON)

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_SAMORDNING_SYKESTIPEND)
    data class SamordningerSykestipend(
        val samordninger: List<SamordningSykestipend>,
    ) : Faktagrunnlag(FaktagrunnlagType.SAMORDNING_SYKESTIPEND) {

        data class SamordningSykestipend(
            val fraOgMed: LocalDate,
            val tilOgMed: LocalDate,
        )
    }

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_SAMORDNING_BARNEPENSJON)
    data class SamordningerBarnepensjon(
        val samordninger: List<SamordningBarnepensjon>,
    ) : Faktagrunnlag(FaktagrunnlagType.SAMORDNING_BARNEPENSJON) {

        data class SamordningBarnepensjon(
            val fraOgMed: LocalDate,
            val tilOgMed: LocalDate?,
            val månedsats: BigDecimal,
        )
    }

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_SAMORDNING_FRADRAG_ANDRE_YTELSER)
    data class SamordningerFradragAndreYtelser(
        val perioder: List<SamordningFradragAnnenYtelse>,
    ) : Faktagrunnlag(FaktagrunnlagType.SAMORDNING_FRADRAG_ANDRE_YTELSER) {

        data class SamordningFradragAnnenYtelse(
            val ytelseNavn: String,
            val fraOgMed: LocalDate,
            val tilOgMed: LocalDate,
        )
    }
}
