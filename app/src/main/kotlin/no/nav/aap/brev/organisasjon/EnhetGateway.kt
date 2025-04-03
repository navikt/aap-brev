package no.nav.aap.brev.organisasjon

interface EnhetGateway {

    fun hentEnheter(enhetsnummer: List<String>): List<Enhet>

    fun hentOverordnetFylkesenhet(enhetsnummer: String): Enhet
}
