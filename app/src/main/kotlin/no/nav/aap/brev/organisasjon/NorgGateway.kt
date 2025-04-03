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
    private val client =
        RestClient.withDefaultResponseHandler(config = ClientConfig(), tokenProvider = NoTokenTokenProvider())

    override fun hentEnheter(enhetsnummer: List<String>): List<Enhet> {
        val uri = baseUri.resolve("/norg2/api/v1/enhet")
        val uriWithParams = URLBuilder(uri.toString()).apply {
            parameters.appendAll("enhetsnummerListe", enhetsnummer)
        }.build().toURI()
        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        val response: List<NorgEnhet> =
            checkNotNull(client.get(uri = uriWithParams, request = httpRequest, mapper = { body, _ ->
                DefaultJsonMapper.fromJson(body)
            }))

        return response.map { it.tilEnhet() }
    }

    override fun hentOverordnetFylkesenhet(enhetsnummer: String): Enhet {
        val uri = baseUri.resolve("/norg2/api/v1/enhet/$enhetsnummer/overordnet?organiseringsType=${EnhetsType.FYLKE}")

        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        val response: List<NorgEnhet> =
            checkNotNull(client.get(uri = uri, request = httpRequest, mapper = { body, _ ->
                DefaultJsonMapper.fromJson(body)
            }))

        return response.single().tilEnhet()
    }

    private fun NorgEnhet.tilEnhet(): Enhet {
        return Enhet(
            enhetsNummer = enhetNr,
            navn = navn,
            type = when (type) {
                "LOKAL" -> EnhetsType.LOKAL
                "YTA" -> EnhetsType.ARBEID_OG_YTELSE
                "FYLKE" -> EnhetsType.FYLKE
                else -> EnhetsType.ANNET
            }
        )
    }
}