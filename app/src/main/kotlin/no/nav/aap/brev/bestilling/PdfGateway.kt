package no.nav.aap.brev.bestilling

import no.nav.aap.brev.kontrakt.Brev

interface PdfGateway {
    fun genererPdf(navn: String,
                   ident: String,
                   saksnummer: String,
                   brev: Brev): Pdf
}
