package no.nav.aap.brev.kontrakt


import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

public data class Brev(
    val kanSendesAutomatisk: Boolean?,
    val overskrift: String?, // H1
    val kanOverstyreBrevtittel: Boolean?,
    val journalpostTittel: String?,
    val tekstbolker: List<Tekstbolk>, // tekster med valgfri overskrift (H2)
)

public data class Tekstbolk(
    val id: UUID,
    val overskrift: String?, // H2
    val innhold: List<Innhold>, // tekster med valgfri overskrift (H3)
)

public data class Innhold(
    val id: UUID,
    val overskrift: String?,  // H3
    val blokker: List<Blokk>, // avsnitt eller punktliste
    val kanRedigeres: Boolean,
    val erFullstendig: Boolean,
)

public data class Blokk(
    val id: UUID,
    val innhold: List<BlokkInnhold>, // formattert tekst og faktagrunnlag
    val type: BlokkType,
)

public enum class BlokkType {
    AVSNITT, LISTE
}

public const val BLOKK_INNHOLD_TYPE_TEKST: String = "TEKST"
public const val BLOKK_INNHOLD_TYPE_FAKTAGRUNNLAG: String = "FAKTAGRUNNLAG"

public enum class BlokkInnholdType(@JsonValue public val verdi: String) {

    TEKST(BLOKK_INNHOLD_TYPE_TEKST), FAKTAGRUNNLAG(BLOKK_INNHOLD_TYPE_FAKTAGRUNNLAG)
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
public sealed class BlokkInnhold(public val type: BlokkInnholdType) {

    @JsonTypeName(BLOKK_INNHOLD_TYPE_TEKST)
    public data class FormattertTekst(
        public val id: UUID,
        public val tekst: String,
        public val formattering: List<Formattering>,
    ) : BlokkInnhold(BlokkInnholdType.TEKST)

    @JsonTypeName(BLOKK_INNHOLD_TYPE_FAKTAGRUNNLAG)
    public data class Faktagrunnlag(
        public val id: UUID,
        public val visningsnavn: String,
        public val tekniskNavn: String,
    ) : BlokkInnhold(BlokkInnholdType.FAKTAGRUNNLAG)
}

public enum class Formattering {
    UNDERSTREK, KURSIV, FET
}
