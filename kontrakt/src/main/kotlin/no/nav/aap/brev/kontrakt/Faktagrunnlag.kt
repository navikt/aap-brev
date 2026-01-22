package no.nav.aap.brev.kontrakt

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.annotation.JsonValue
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year

const val FAKTAGRUNNLAG_TYPE_AAP_FOM_DATO: String = "AAP_FOM_DATO"
const val FAKTAGRUNNLAG_TYPE_FRIST_DATO_11_7: String = "FRIST_DATO_11_7"
const val FAKTAGRUNNLAG_TYPE_GRUNNLAG_BEREGNING: String = "GRUNNLAG_BEREGNING"
const val FAKTAGRUNNLAG_TYPE_TILKJENT_YTELSE: String = "TILKJENT_YTELSE"
const val FAKTAGRUNNLAG_TYPE_SYKDOMSVURDERING: String = "SYKDOMSVURDERING"

enum class FaktagrunnlagType(@JsonValue val verdi: String) {
    AAP_FOM_DATO(FAKTAGRUNNLAG_TYPE_AAP_FOM_DATO),
    FRIST_DATO_11_7(FAKTAGRUNNLAG_TYPE_FRIST_DATO_11_7),
    GRUNNLAG_BEREGNING(FAKTAGRUNNLAG_TYPE_GRUNNLAG_BEREGNING),
    TILKJENT_YTELSE(FAKTAGRUNNLAG_TYPE_TILKJENT_YTELSE),
    SYKDOMSVURDERING(FAKTAGRUNNLAG_TYPE_SYKDOMSVURDERING)
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
sealed class Faktagrunnlag(val type: FaktagrunnlagType) {

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_AAP_FOM_DATO)
    data class AapFomDato(
        val dato: LocalDate
    ) : Faktagrunnlag(FaktagrunnlagType.AAP_FOM_DATO)

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
        val sisteDagMedYtelse: LocalDate?,
        val kravdatoUføretrygd: LocalDate?
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
}
