package no.nav.aap.brev.distribusjon

import no.nav.aap.brev.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.net.URI

val JOURNALPOST_TEMA_OPPFOLGING = "AAP"

class DokdistkanalGateway : DistribusjonskanalGateway {
    private val baseUri = URI.create(requiredConfigForKey("integrasjon.dokdistkanal.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.dokdistkanal.scope"))

    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = DefaultResponseHandler(),
        prometheus = prometheus
    )

    override fun bestemDistribusjonskanal(brukerId: String, mottakerId: String): Distribusjonskanal? {
        val httpRequest = PostRequest(
            body = BestemDistribusjonskanalRequest(
                brukerId,
                mottakerId
            )
        )
        val uri = baseUri.resolve("/rest/bestemDistribusjonskanal")
        val response = client.post<BestemDistribusjonskanalRequest, BestemDistribusjonskanalResponse>(uri, httpRequest)
        return response?.distribusjonskanal
    }
}

data class BestemDistribusjonskanalRequest(
    val brukerId: String,
    val mottakerId: String,
    val tema: String = JOURNALPOST_TEMA_OPPFOLGING,
)

data class BestemDistribusjonskanalResponse(
    val distribusjonskanal: Distribusjonskanal?
)
