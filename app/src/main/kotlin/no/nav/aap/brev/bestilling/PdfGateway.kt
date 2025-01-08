package no.nav.aap.brev.bestilling

interface PdfGateway {
    fun genererPdf(brev: PdfBrev): Pdf
}
