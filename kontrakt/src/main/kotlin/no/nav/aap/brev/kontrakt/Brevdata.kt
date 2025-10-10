import com.fasterxml.jackson.databind.node.ObjectNode

data class Brevdata(
    val delmaler: List<Delmal>,
    val faktagrunnlag: List<FaktagrunnlagMedVerdi>,
    val periodetekster: List<Periodetekst>,
    val valg: List<Valg>,
    val betingetTekst: List<BetingetTekst>,
    val fritekster: List<FritekstMedKey>
) {
    data class Delmal(val id: String)

    data class FaktagrunnlagMedVerdi(
        val tekniskNavn: String,
        val verdi: String
    )

    data class Periodetekst(
        val id: String,
        val faktagrunnlagMedVerdi: List<FaktagrunnlagMedVerdi>
    )

    data class Valg(
        val id: String,
        val valgt: String, // key
        val fritekst: Fritekst?,
    )

    @JvmInline
    value class Fritekst(val json: ObjectNode)

    data class FritekstMedKey(val key: String, val fritekst: Fritekst)

    data class BetingetTekst(val id: String)
}