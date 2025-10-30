package no.nav.aap.brev.bestilling

import no.nav.aap.brev.kontrakt.Signatur
import no.nav.aap.brev.kontrakt.Språk
import java.time.LocalDate

data class GenererPdfRequest(
    val brukerIdent: String,
    val navn: String,
    val saksnummer: Saksnummer,
    val brevmal: BrevmalJson,
    val brevdata: Brevdata,
    val dato: LocalDate,
    val språk: Språk,
    val signaturer: List<Signatur>,
)
