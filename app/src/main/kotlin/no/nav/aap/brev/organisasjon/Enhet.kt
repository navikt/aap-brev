package no.nav.aap.brev.organisasjon

data class Enhet(val enhetsNummer: String, val navn: String, val type: EnhetsType) {
    val enhetstypeNavn: String =
        if (type == EnhetsType.ARBEID_OG_YTELSE) "Nav arbeid og ytelser"
        else navn
}

enum class EnhetsType {
    LOKAL, ARBEID_OG_YTELSE, ANNET
}
