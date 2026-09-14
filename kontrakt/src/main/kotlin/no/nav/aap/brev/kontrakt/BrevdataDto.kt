package no.nav.aap.brev.kontrakt

public data class BrevdataDto(
    val delmaler: List<Delmal>,
    val valg: List<Valg>,
    val betingetTekst: List<BetingetTekst>,
    val fritekster: List<Fritekst>
) {
    public data class Delmal(public val id: String)

    public data class Faktagrunnlag(
        public val tekniskNavn: String,
        public val verdi: String
    )

    public data class Valg(
        public val id: String,
        public val key: String,
    )

    public data class Fritekst(
        public val parentId: String,
        public val key: String,
        public val fritekst: String
    )

    public data class BetingetTekst(public val id: String)
}