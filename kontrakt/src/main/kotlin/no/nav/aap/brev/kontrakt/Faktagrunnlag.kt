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
const val FAKTAGRUNNLAG_TYPE_SAMORDNING_GRADERING: String = "SAMORDNING_GRADERING"
const val FAKTAGRUNNLAG_TYPE_SAMORDNING_UFORE: String = "SAMORDNING_UFORE"
const val FAKTAGRUNNLAG_TYPE_SAMORDNING_ARBEIDSGIVER: String = "SAMORDNING_ARBEIDSGIVER"
const val FAKTAGRUNNLAG_TYPE_TJENESTEPENSJON_REFUSJONSKRAV: String = "TJENESTEPENSJON_REFUSJONSKRAV"
const val FAKTAGRUNNLAG_TYPE_SYKESTIPEND: String = "SYKESTIPEND"
const val FAKTAGRUNNLAG_TYPE_BARNEPENSJON: String = "BARNEPENSJON"
const val FAKTAGRUNNLAG_TYPE_ANDRE_STATLIGE_YTELSER: String = "ANDRE_STATLIGE_YTELSER"

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
    SAMORDNING_GRADERING(FAKTAGRUNNLAG_TYPE_SAMORDNING_GRADERING),
    SAMORDNING_UFORE(FAKTAGRUNNLAG_TYPE_SAMORDNING_UFORE),
    SAMORDNING_ARBEIDSGIVER(FAKTAGRUNNLAG_TYPE_SAMORDNING_ARBEIDSGIVER),
    TJENESTEPENSJON_REFUSJONSKRAV(FAKTAGRUNNLAG_TYPE_TJENESTEPENSJON_REFUSJONSKRAV),
    SYKESTIPEND(FAKTAGRUNNLAG_TYPE_SYKESTIPEND),
    BARNEPENSJON(FAKTAGRUNNLAG_TYPE_BARNEPENSJON),
    ANDRE_STATLIGE_YTELSER(FAKTAGRUNNLAG_TYPE_ANDRE_STATLIGE_YTELSER),
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

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_SAMORDNING_GRADERING)
    data class SamordningGradering(
        val vurderinger: List<GraderingVurdering>,
    ) : Faktagrunnlag(FaktagrunnlagType.SAMORDNING_GRADERING) {
        data class GraderingVurdering(
            val ytelseType: String,
            val perioder: List<GraderingPeriode>,
        )

        data class GraderingPeriode(
            val fom: LocalDate,
            val tom: LocalDate,
            val gradering: Int?,
        )
    }

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_SAMORDNING_UFORE)
    data class SamordningUfore(
        val perioder: List<UførePeriode>,
    ) : Faktagrunnlag(FaktagrunnlagType.SAMORDNING_UFORE) {
        data class UførePeriode(
            val virkningstidspunkt: LocalDate,
            val uføregradTilSamordning: Int,
        )
    }

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_SAMORDNING_ARBEIDSGIVER)
    data class SamordningArbeidsgiver(
        val perioder: List<ArbeidsgiverPeriode>,
    ) : Faktagrunnlag(FaktagrunnlagType.SAMORDNING_ARBEIDSGIVER) {
        data class ArbeidsgiverPeriode(
            val fom: LocalDate,
            val tom: LocalDate,
        )
    }

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_TJENESTEPENSJON_REFUSJONSKRAV)
    data class TjenestepensjonRefusjonskrav(
        val harKrav: Boolean,
        val fom: LocalDate?,
        val tom: LocalDate?,
    ) : Faktagrunnlag(FaktagrunnlagType.TJENESTEPENSJON_REFUSJONSKRAV)

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_SYKESTIPEND)
    data class Sykestipend(
        val perioder: List<SykestipendPeriode>,
    ) : Faktagrunnlag(FaktagrunnlagType.SYKESTIPEND) {
        data class SykestipendPeriode(
            val fom: LocalDate,
            val tom: LocalDate,
        )
    }

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_BARNEPENSJON)
    data class Barnepensjon(
        val perioder: List<BarnepensjonPeriode>,
    ) : Faktagrunnlag(FaktagrunnlagType.BARNEPENSJON) {
        data class BarnepensjonPeriode(
            val fom: LocalDate,
            val tom: LocalDate?,
            val månedsats: BigDecimal,
        )
    }

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_ANDRE_STATLIGE_YTELSER)
    data class AndreStatligeYtelser(
        val perioder: List<AndreStatligeYtelserPeriode>,
    ) : Faktagrunnlag(FaktagrunnlagType.ANDRE_STATLIGE_YTELSER) {
        data class AndreStatligeYtelserPeriode(
            val ytelse: String,
            val fom: LocalDate,
            val tom: LocalDate,
        )
    }
}
