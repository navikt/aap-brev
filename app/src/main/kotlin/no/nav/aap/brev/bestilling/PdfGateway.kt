package no.nav.aap.brev.bestilling

import no.nav.aap.brev.kontrakt.Brev

interface PdfGateway {
    fun genererPdf(personinfo: Personinfo,
                   saksnummer: Saksnummer,
                   brev: Brev): Pdf
}
