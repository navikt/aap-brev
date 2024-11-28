package no.nav.aap.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.BrevbestillingLøsningStatus
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.LøsBrevbestillingDto
import no.nav.aap.brev.innhold.Faktagrunnlag
import no.nav.aap.brev.innhold.FaktagrunnlagType
import no.nav.aap.brev.innhold.HentFagtagrunnlagGateway
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

class BehandlingsflytGateway : BestillerGateway, PersoninfoGateway, HentFagtagrunnlagGateway {
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

        return checkNotNull(client.get(uri = uri, request = httpRequest, mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body)
        }))
    }

    override fun hent(
        behandlingReferanse: BehandlingReferanse,
        faktagrunnlag: Set<FaktagrunnlagType>
    ): Set<Faktagrunnlag> {
        val uri = baseUri.resolve("/api/brev/faktagrunnlag")
        val httpRequest = PostRequest(
            body = HentFaktaGrunnlagRequest(
                behandlingReferanse,
                faktagrunnlag
            ),
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        val response : HentFaktaGrunnlagResponse = checkNotNull(client.post(uri = uri, request = httpRequest))

        return response.faktagrunnlag
    }

    //TODO Flytt begge inn i kontrakten
    data class HentFaktaGrunnlagRequest(
        val behandlingReferanse: BehandlingReferanse,
        val faktagrunnlag: Set<FaktagrunnlagType>
    )

    data class HentFaktaGrunnlagResponse(
        val faktagrunnlag: Set<Faktagrunnlag>,
    )
}