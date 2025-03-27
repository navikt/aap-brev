package no.nav.aap.brev.bestilling

interface PersoninfoGateway {
    fun hentPersoninfo(personIdent: String): Personinfo
}

data class Personinfo(
    val personIdent: String,
    val navn: String,
    val harStrengtFortroligAdresse: Boolean
)
