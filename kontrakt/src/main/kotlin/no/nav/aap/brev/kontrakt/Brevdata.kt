import com.fasterxml.jackson.databind.node.ObjectNode

data class Brevdata(
    val delmaler: List<Delmal>,
    val faktagrunnlag: List<FaktagrunnlagMedVerdi>,
    val periodetekster: List<Periodetekst>,
    val valg: List<Valg>,
    val betingetTekst: List<BetingetTekst>
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
        val valgtId: String?,
        val fritekst: ObjectNode?,
    ) {
        init {
            require(valgtId != null || fritekst != null) {
                "Må ha verdi for enten valgtId eller fritekst"
            }
            require(!(valgtId != null && fritekst != null)) {
                "Kan ikke ha verdi for både valgt og fritekst"
            }
        }
    }

    data class BetingetTekst(val id: String)
}