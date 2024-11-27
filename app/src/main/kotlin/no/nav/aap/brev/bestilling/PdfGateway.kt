package no.nav.aap.brev.bestilling

import no.nav.aap.brev.kontrakt.PdfBrev

interface PdfGateway {
    fun genererPdf(brev: PdfBrev): Pdf
}
