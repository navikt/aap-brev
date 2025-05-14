package no.nav.aap.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.BrevbestillingLøsningStatus
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.Faktagrunnlag as BehandlingsflytFaktagrunnlag
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.FaktagrunnlagDto
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.FaktagrunnlagType as BehandlingsflytFaktagrunnlagType
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.HentFaktaGrunnlagRequest
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.LøsBrevbestillingDto
import no.nav.aap.brev.innhold.HentFagtagrunnlagGateway
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.kontrakt.FaktagrunnlagType
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.net.URI

class BehandlingsflytGateway : BestillerGateway, HentFagtagrunnlagGateway {
    private val baseUri = URI.create(requiredConfigForKey("integrasjon.behandlingsflyt.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.behandlingsflyt.scope"))
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus,
    )

    override fun oppdaterBrevStatus(brevbestilling: Brevbestilling, status: Status) {
        val brevbestillingLøsningStatus = when (status) {
            Status.UNDER_ARBEID -> BrevbestillingLøsningStatus.KLAR_FOR_EDITERING
            Status.FERDIGSTILT -> BrevbestillingLøsningStatus.AUTOMATISK_FERDIGSTILT
            Status.REGISTRERT, Status.AVBRUTT -> throw IllegalStateException("Uforventet status $status")
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

    override fun hent(
        behandlingReferanse: BehandlingReferanse,
        faktagrunnlag: Set<FaktagrunnlagType>
    ): Set<Faktagrunnlag> {
        val uri = baseUri.resolve("/api/brev/faktagrunnlag")
        val httpRequest = PostRequest(
            body = HentFaktaGrunnlagRequest(
                no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse(behandlingReferanse.referanse),
                faktagrunnlag.map { it.tilBehandlingsflytType() }.toSet()
            ),
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        val response: FaktagrunnlagDto = checkNotNull(client.post(uri = uri, request = httpRequest))

        return response.faktagrunnlag.map { it.fraBehandlingsflytType() }.toSet()
    }

    private fun FaktagrunnlagType.tilBehandlingsflytType(): BehandlingsflytFaktagrunnlagType {
        return when (this) {
            FaktagrunnlagType.FRIST_DATO_11_7 -> BehandlingsflytFaktagrunnlagType.FRIST_DATO_11_7
            FaktagrunnlagType.GRUNNLAG_BEREGNING -> BehandlingsflytFaktagrunnlagType.GRUNNLAG_BEREGNING
        }
    }

    private fun BehandlingsflytFaktagrunnlag.fraBehandlingsflytType(): Faktagrunnlag {
        return when (this) {
            is BehandlingsflytFaktagrunnlag.FristDato11_7 -> Faktagrunnlag.FristDato11_7(
                frist
            )

            is BehandlingsflytFaktagrunnlag.GrunnlagBeregning -> Faktagrunnlag.GrunnlagBeregning(
                inntekterPerÅr.map { it.fraBehandlingsflytType() }
            )
        }
    }

    private fun BehandlingsflytFaktagrunnlag.GrunnlagBeregning.InntektPerÅr.fraBehandlingsflytType(): Faktagrunnlag.GrunnlagBeregning.InntektPerÅr {
        return Faktagrunnlag.GrunnlagBeregning.InntektPerÅr(år = år, inntekt = inntekt)
    }
}
