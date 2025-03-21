package no.nav.aap.brev.person

data class PdlPersonData(
    val navn: List<Navn>,
    val adressebeskyttelse: List<Adressebeskyttelse>
)

data class Navn(val fornavn: String, val mellomnavn: String?, val etternavn: String)
data class Adressebeskyttelse(val gradering: Gradering, val metadata: Metadata)
enum class Gradering {
    FORTROLIG, STRENGT_FORTROLIG, STRENGT_FORTROLIG_UTLAND, UGRADERT
}

data class Metadata(val historisk: Boolean)
