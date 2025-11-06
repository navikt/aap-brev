package no.nav.aap.brev.kontrakt

data class OppdaterBrevdataRequest(
    val delmaler: List<ValgtDelmal>,
    val faktagrunnlag: List<Faktagrunnlag>,
    val periodetekster: List<Periodetekst>,
    val valg: List<Valg>,
    val betingetTekst: List<BetingetTekst>,
    val fritekster: List<Fritekst>
) {
    data class ValgtDelmal(val id: String)

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

    data class Fritekst(
        val id: String,
        val key: String,
        val fritekstJson: String
    )

    data class BetingetTekst(val id: String)
}