package no.nav.aap.brev.bestilling

import com.fasterxml.jackson.databind.node.ObjectNode

data class Brevdata(
    val delmaler: List<Delmal>,
    val faktagrunnlag: List<Faktagrunnlag>,
    val periodetekster: List<Periodetekst>,
    val valg: List<Valg>,
    val betingetTekst: List<BetingetTekst>,
    val fritekster: List<Fritekst>
) {
    data class Delmal(val id: String)

    data class Faktagrunnlag(
        val tekniskNavn: String,
        val verdi: String
    )

    data class Periodetekst(
        val id: String,
        val faktagrunnlag: List<Faktagrunnlag>
    )

    data class Valg(
        val id: String,
        val key: String,
    )

    @JvmInline
    value class FritekstJson(val json: ObjectNode)

    // Kan være fritekst for en delmal eller valg
    data class Fritekst(
        val parentId: String,
        val key: String,
        val fritekst: FritekstJson
    )

    data class BetingetTekst(val id: String)
}