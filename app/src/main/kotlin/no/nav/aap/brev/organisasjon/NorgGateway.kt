package no.nav.aap.brev.organisasjon

import io.ktor.http.URLBuilder
import io.ktor.http.toURI
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.net.URI

class NorgGateway : EnhetGateway {
    private val baseUri = URI.create(requiredConfigForKey("integrasjon.norg.url"))
    private val client = RestClient.withDefaultResponseHandler(config = ClientConfig(), tokenProvider = NoTokenTokenProvider())

    override fun hentEnhetsnavn(enhetsNummer: List<String>): List<Enhet> {
        val uri = baseUri.resolve("/norg2/api/v1/enhet")
        val uriWithParams = URLBuilder(uri.toString()).apply {
            parameters.appendAll("enhetsnummerListe", enhetsNummer)
        }.build().toURI()
        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        val response = checkNotNull(client.get(uri = uriWithParams, request = httpRequest, mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body)
        }))
        return TODO("Provide the return value")
    }

    private fun toEnhet(enhet: NorgEnhet): Enhet {
        TODO()
    }
}