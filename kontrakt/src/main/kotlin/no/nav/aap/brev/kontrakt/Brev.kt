package no.nav.aap.brev.kontrakt


import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

data class Brev(
    val kanSendesAutomatisk: Boolean?,
    val overskrift: String?, // H1
    val kanOverstyreBrevtittel: Boolean?,
    val journalpostTittel: String?,
    val tekstbolker: List<Tekstbolk>, // tekster med valgfri overskrift (H2)
)

data class Tekstbolk(
    val id: UUID,
    val overskrift: String?, // H2
    val innhold: List<Innhold>, // tekster med valgfri overskrift (H3)
)

data class Innhold(
    val id: UUID,
    val overskrift: String?,  // H3
    val blokker: List<Blokk>, // avsnitt eller punktliste
    val kanRedigeres: Boolean,
    val erFullstendig: Boolean,
)

data class Blokk(
    val id: UUID,
    val innhold: List<BlokkInnhold>, // formattert tekst og faktagrunnlag
    val type: BlokkType,
)

enum class BlokkType {
    AVSNITT, LISTE
}

const val BLOKK_INNHOLD_TYPE_TEKST = "TEKST"
const val BLOKK_INNHOLD_TYPE_FAKTAGRUNNLAG = "FAKTAGRUNNLAG"

enum class BlokkInnholdType(@JsonValue val verdi: String) {

    TEKST(BLOKK_INNHOLD_TYPE_TEKST), FAKTAGRUNNLAG(BLOKK_INNHOLD_TYPE_FAKTAGRUNNLAG)
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
sealed class BlokkInnhold(val type: BlokkInnholdType) {

    @JsonTypeName(BLOKK_INNHOLD_TYPE_TEKST)
    data class FormattertTekst(
        val id: UUID,
        val tekst: String,
        val formattering: List<Formattering>,
    ) : BlokkInnhold(BlokkInnholdType.TEKST)

    @JsonTypeName(BLOKK_INNHOLD_TYPE_FAKTAGRUNNLAG)
    data class Faktagrunnlag(
        val id: UUID,
        val visningsnavn: String,
        val tekniskNavn: String,
    ) : BlokkInnhold(BlokkInnholdType.FAKTAGRUNNLAG)
}

enum class Formattering {
    UNDERSTREK, KURSIV, FET
}
