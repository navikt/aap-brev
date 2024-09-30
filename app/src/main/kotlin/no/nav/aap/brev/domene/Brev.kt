package no.nav.aap.brev.domene

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName

data class Brev(
    val overskrift: String?,
    val tekstbolker: List<Tekstbolk>,
)

data class Tekstbolk(
    val overskrift: String?,
    val innhold: List<Innhold>,
)

data class Innhold(
    val sprak: String?,
    val overskrift: String,
    val avsnitt: List<Avsnitt>,
    val kanRedigeres: Boolean,
    val erFullstendig: Boolean,
)

data class Avsnitt(
    val tekst: List<Tekst>,
    val listeInnrykk: Int?,
)


const val TEKST_TYPE_TEKST = "tekst"
const val TEKST_TYPE_FAKTAGRUNNLAG = "faktagrunnlag"

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
sealed abstract class Tekst(val type: String) {

    @JsonTypeName(TEKST_TYPE_TEKST)
    data class FormattertTekst(
        val tekst: String,
        val formattering: List<Formattering>,
    ) : Tekst(TEKST_TYPE_TEKST)

    @JsonTypeName(TEKST_TYPE_FAKTAGRUNNLAG)
    data class Faktagrunnlag(
        val visningsnavn: String,
        val tekniskNavn: String,
    ) : Tekst(TEKST_TYPE_FAKTAGRUNNLAG)
}

enum class Formattering {
    understrek, kursiv, fet
}