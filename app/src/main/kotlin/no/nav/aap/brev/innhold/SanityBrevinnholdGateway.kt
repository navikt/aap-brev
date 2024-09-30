package no.nav.aap.brev.innhold

import no.nav.aap.brev.domene.Brev
import no.nav.aap.brev.domene.Brevtype
import no.nav.aap.brev.domene.Språk
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import java.net.URI

class SanityBrevinnholdGateway : BrevinnholdGateway {

    private val baseUri = URI.create(requiredConfigForKey("integrasjon.brev_sanity_proxy.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.brev_sanity_proxy.scope"))
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider
    )

    override fun hentBrevmal(
        brevtype: Brevtype,
        språk: Språk
    ): Brev {
        val uri = baseUri.resolve("/api/mal?brevtype=$brevtype&sprak=$språk")
        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        return requireNotNull(client.get(uri = uri, request = httpRequest, mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body)
        }))
    }
}
