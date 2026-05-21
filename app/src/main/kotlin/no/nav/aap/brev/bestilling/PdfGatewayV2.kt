package no.nav.aap.brev.bestilling

interface PdfGatewayV2 {
    fun genererPdf(request: GenererPdfRequest): Pdf
    fun genererHtml(request: GenererPdfRequest): String
    fun genererJsonForForhåndsvisning(request: GenererPdfRequest): String
}
