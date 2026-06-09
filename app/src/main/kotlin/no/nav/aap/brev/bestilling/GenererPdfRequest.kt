package no.nav.aap.brev.bestilling

import no.nav.aap.brev.kontrakt.Signatur
import no.nav.aap.brev.kontrakt.Språk

data class GenererPdfRequest(
    val brukerIdent: String,
    val navn: String,
    val saksnummer: Saksnummer,
    val brevmal: BrevmalJson,
    val brevdata: Brevdata,
    val dato: String,
    val språk: Språk,
    val signaturer: List<Signatur>,
    val mottaker: Mottaker,
) {
    data class Mottaker(
        val navn: String,
        val ident: String,
        val identType: IdentType
    ) {
        enum class IdentType {
            FNR, HPRNR
        }
    }

}
