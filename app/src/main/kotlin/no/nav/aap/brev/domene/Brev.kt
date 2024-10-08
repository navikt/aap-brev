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
    val sprak: Språk?,
    val overskrift: String,
    val blokker: List<Blokk>,
    val kanRedigeres: Boolean,
    val erFullstendig: Boolean,
)

data class Blokk(
    val innhold: List<BlokkInnhold>,
    val type: BlokkType,
)

enum class BlokkType {
    avsnitt, liste
}

const val TEKST_TYPE_TEKST = "tekst"
const val TEKST_TYPE_FAKTAGRUNNLAG = "faktagrunnlag"

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
sealed class BlokkInnhold(val type: String) {

    @JsonTypeName(TEKST_TYPE_TEKST)
    data class FormattertTekst(
        val tekst: String,
        val formattering: List<Formattering>,
    ) : BlokkInnhold(TEKST_TYPE_TEKST)

    @JsonTypeName(TEKST_TYPE_FAKTAGRUNNLAG)
    data class Faktagrunnlag(
        val visningsnavn: String,
        val tekniskNavn: String,
    ) : BlokkInnhold(TEKST_TYPE_FAKTAGRUNNLAG)
}

enum class Formattering {
    understrek, kursiv, fet
}
