package no.nav.aap.brev.bestilling

interface PdfGatewayV2 {
    fun genererPdf(request: GenererPdfRequest): Pdf
}
