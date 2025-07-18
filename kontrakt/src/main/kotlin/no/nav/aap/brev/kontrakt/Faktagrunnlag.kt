package no.nav.aap.brev.kontrakt

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.annotation.JsonValue
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year

const val FAKTAGRUNNLAG_TYPE_FRIST_DATO_11_7: String = "FRIST_DATO_11_7"
const val FAKTAGRUNNLAG_TYPE_GRUNNLAG_BEREGNING: String = "GRUNNLAG_BEREGNING"

enum class FaktagrunnlagType(@JsonValue val verdi: String) {
    FRIST_DATO_11_7(FAKTAGRUNNLAG_TYPE_FRIST_DATO_11_7),
    GRUNNLAG_BEREGNING(FAKTAGRUNNLAG_TYPE_GRUNNLAG_BEREGNING),
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
sealed class Faktagrunnlag(val type: FaktagrunnlagType) {

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_FRIST_DATO_11_7)
    data class FristDato11_7(
        val frist: LocalDate
    ) : Faktagrunnlag(FaktagrunnlagType.FRIST_DATO_11_7)

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_GRUNNLAG_BEREGNING)
    data class GrunnlagBeregning(
        val inntekterPerÅr: List<InntektPerÅr>
    ) : Faktagrunnlag(FaktagrunnlagType.GRUNNLAG_BEREGNING) {
        data class InntektPerÅr(val år: Year, val inntekt: BigDecimal)
    }
}
