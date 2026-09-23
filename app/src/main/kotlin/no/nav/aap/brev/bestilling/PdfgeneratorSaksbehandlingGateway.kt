package no.nav.aap.brev.bestilling

import no.nav.aap.brev.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import java.net.URI

class PdfgeneratorSaksbehandlingGateway : PdfGateway {

    private val baseUri = URI.create(requiredConfigForKey("integrasjon.pdfgenerator_saksbehandling.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.pdfgenerator_saksbehandling.scope"))
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = NoTokenTokenProvider(),
        prometheus = prometheus,
    )

    override fun genererPdf(brev: PdfBrev): Pdf {
        val uri = baseUri.resolve("/api/v1/genpdf/saksbehandling/brev")
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
            "Fikk tom respons fra pdfgenerator"
        }

        return Pdf(bytes)
    }
}