package no.nav.aap.brev.bestilling

interface PersoninfoGateway {
    fun hentPersoninfo(saksnummer: Saksnummer): Personinfo
}

data class Personinfo(val fnr: String, val navn: String)
