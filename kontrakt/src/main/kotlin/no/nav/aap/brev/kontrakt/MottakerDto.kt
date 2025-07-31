package no.nav.aap.brev.kontrakt

data class MottakerDto(
    val ident: String? = null,
    val identType: IdentType? = null,
    val navnOgAdresse: NavnOgAdresse? = null,
)

data class NavnOgAdresse(
    val navn: String,
    val adresse: Adresse,
)

data class Adresse(
    val landkode: String,
    val adresselinje1: String,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
) {
    init {
        require(
            (landkode == "NO" && postnummer != null && poststed != null) ||
                    (landkode != "NO" && postnummer == null && poststed == null)
        ) {
            "Postnummer og poststed må være satt for norsk"
        }
    }
}

enum class IdentType {
    FNR, HPRNR, ORGNR, UTL_ORG
}