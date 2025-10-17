package no.nav.aap.brev.distribusjon

interface AdresseGateway {
    fun hentPostadresse(personident: String): HentPostadresseResponse?
}
