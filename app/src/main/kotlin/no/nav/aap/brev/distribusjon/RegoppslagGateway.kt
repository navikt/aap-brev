package no.nav.aap.brev.distribusjon

import no.nav.aap.brev.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.error.RestResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class RegoppslagGateway : AdresseGateway {
    private val baseUri = URI.create(requiredConfigForKey("integrasjon.regoppslag.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.regoppslag.scope"))

    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = RegoppslagResponseHandler(),
        prometheus = prometheus
    )

    override fun hentPostadresse(personident: String): HentPostadresseResponse? {
        val httpRequest = PostRequest(
            body = HentPostadresseRequest(
                personident
            )
        )
        val uri = baseUri.resolve("/rest/postadresse")
        return client.post<HentPostadresseRequest, HentPostadresseResponse>(uri, httpRequest)
    }
}

data class RegoppslagAdresse(
    val adresseKilde: String,
    val type: String,
    val adresselinje1: String,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val postnummer: String?,
    val poststed: String?,
    val landkode: String,
    val land: String
)

data class HentPostadresseRequest(
    val ident: String,
)

data class HentPostadresseResponse(
    val navn: String?,
    val adresse: RegoppslagAdresse?
)

class RegoppslagResponseHandler : RestResponseHandler<InputStream> {
    private val defaultResponseHandler = DefaultResponseHandler()

    override fun bodyHandler(): HttpResponse.BodyHandler<InputStream> {
        return defaultResponseHandler.bodyHandler()
    }

    override fun <R> håndter(
        request: HttpRequest,
        response: HttpResponse<InputStream>,
        mapper: (InputStream, HttpHeaders) -> R
    ): R? {
        if (response.statusCode() == HttpURLConnection.HTTP_GONE || response.statusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            return null
        }
        return defaultResponseHandler.håndter(request, response, mapper)
    }
}
