package no.nav.aap.brev.bestilling

import no.nav.aap.brev.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import java.net.URI

class SaksbehandlingPdfGenGateway : PdfGateway {

    private val baseUri = URI.create(requiredConfigForKey("integrasjon.saksbehandling_pdfgen.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.saksbehandling_pdfgen.scope"))
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = NoTokenTokenProvider(),
        prometheus = prometheus,
    )

    override fun genererPdf(brev: PdfBrev): Pdf {
        val uri = baseUri.resolve("/api/v1/genpdf/aap-saksbehandling-pdfgen/brev")
        val httpRequest = PostRequest(
            body = brev,
            additionalHeaders = listOf(
                Header("Accept", "application/pdf")
            )
        )
        val bytes = client.post(uri, httpRequest, { body, _ ->
            body.readAllBytes()
        })

        require(bytes != null) {
            "Fikk tom respons fra pdfgen"
        }

        return Pdf(bytes)
    }

    override fun genererPdfV2(request: GenererPdfRequest): Pdf {
        // TODO midlertidig for testing. Dette blir kall mot en annen app
        val uri = baseUri.resolve("/api/v1/genpdf/aap-saksbehandling-pdfgen/brev")
        val httpRequest = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Accept", "application/pdf")
            )
        )
        val bytes = client.post(uri, httpRequest, { body, _ ->
            body.readAllBytes()
        })

        require(bytes != null) {
            "Fikk tom respons fra pdfgen"
        }

        return Pdf(bytes)
    }
}