package no.nav.aap.brev.bestilling

interface PersoninfoV2Gateway {
    fun hentPersoninfo(personIdent: String): PersoninfoV2
}

data class PersoninfoV2(
    val personIdent: String,
    val navn: String,
    val harStrengtFortroligAdresse: Boolean
)
