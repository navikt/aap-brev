package no.nav.aap.brev.innhold

import no.nav.aap.brev.bestilling.BrevmalJson
import no.nav.aap.brev.bestilling.GenererPdfRequest
import no.nav.aap.brev.bestilling.Pdf
import no.nav.aap.brev.bestilling.PdfGatewayV2
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.net.URI

class BrevSanityProxyGateway : BrevinnholdGateway, PdfGatewayV2 {

    private val baseUri = URI.create(requiredConfigForKey("integrasjon.brev_sanity_proxy.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.brev_sanity_proxy.scope"))
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    override fun hentBrev(
        brevtype: Brevtype,
        språk: Språk
    ): Brev {
        val uri = baseUri.resolve("/api/mal?brevtype=$brevtype&sprak=$språk")
        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        return checkNotNull(client.get(uri = uri, request = httpRequest, mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body)
        }))
    }

    override fun hentBrevmal(
        brevtype: Brevtype,
        språk: Språk
    ): BrevmalJson {
        val uri = baseUri.resolve("/api/brevmal?brevtype=$brevtype&sprak=$språk")
        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        return checkNotNull(client.get(uri = uri, request = httpRequest, mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body)
        }))
    }

    override fun genererPdf(request: GenererPdfRequest): Pdf {
        val uri = baseUri.resolve("/api/pdf")
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
