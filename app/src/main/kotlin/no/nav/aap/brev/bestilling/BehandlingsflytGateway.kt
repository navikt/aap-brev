package no.nav.aap.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.BrevbestillingLøsningStatus
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.LøsBrevbestillingDto
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import java.net.URI

class BehandlingsflytGateway : BestillerGateway, PersoninfoGateway {
    private val baseUri = URI.create(requiredConfigForKey("integrasjon.behandlingsflyt.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.behandlingsflyt.scope"))
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider
    )

    override fun oppdaterBrevStatus(brevbestilling: Brevbestilling, status: Status) {
        val brevbestillingLøsningStatus = when (status) {
            Status.REGISTRERT -> return
            Status.UNDER_ARBEID -> BrevbestillingLøsningStatus.KLAR_FOR_EDITERING
            Status.FERDIGSTILT -> BrevbestillingLøsningStatus.AUTOMATISK_FERDIGSTILT
        }

        val request = LøsBrevbestillingDto(
            behandlingReferanse = brevbestilling.behandlingReferanse.referanse,
            bestillingReferanse = brevbestilling.referanse.referanse,
            brevbestillingLøsningStatus,
        )

        val uri = baseUri.resolve("/api/brev/los-bestilling")
        val httpRequest = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        client.post<_, Unit>(uri = uri, request = httpRequest)
    }

    override fun hentPersoninfo(saksnummer: Saksnummer): Personinfo {
        val uri = baseUri.resolve("/api/sak/${saksnummer.nummer}/personinformasjon")
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