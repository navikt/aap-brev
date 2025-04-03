package no.nav.aap.brev.organisasjon

interface EnhetGateway {

    fun hentEnhetsnavn(enhetsNummer: List<String>): List<Enhet>
}