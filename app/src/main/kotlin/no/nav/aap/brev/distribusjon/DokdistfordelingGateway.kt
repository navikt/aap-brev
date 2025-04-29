package no.nav.aap.brev.distribusjon

import no.nav.aap.brev.distribusjon.DistribuerJournalpostRequest.Distribusjonstidspunkt
import no.nav.aap.brev.distribusjon.DistribuerJournalpostRequest.Distribusjonstype
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.prometheus
import no.nav.aap.brev.util.HåndterConflictResponseHandler
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import java.net.URI

class DokdistfordelingGateway : DistribusjonGateway {
    private val baseUri = URI.create(requiredConfigForKey("integrasjon.dokdistfordeling.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.saf.scope"))
    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = HåndterConflictResponseHandler(),
        prometheus = prometheus,
    )

    override fun distribuerJournalpost(journalpostId: JournalpostId, brevtype: Brevtype): DistribusjonBestillingId {
        val request = DistribuerJournalpostRequest(
            journalpostId = journalpostId.id,
            bestillendeFagsystem = "KELVIN",
            dokumentProdApp = "KELVIN",
            distribusjonstype = utledDistribusjonstype(brevtype),
            distribusjonstidspunkt = Distribusjonstidspunkt.KJERNETID
        )
        val httpRequest = PostRequest(
            body = request
        )
        val uri = baseUri.resolve("/rest/v1/distribuerjournalpost")
        val response = checkNotNull(client.post<DistribuerJournalpostRequest, DistribuerJournalpostResponse>(uri, httpRequest))
        return DistribusjonBestillingId(response.bestillingsId)
    }

    private fun utledDistribusjonstype(brevtype: Brevtype): Distribusjonstype {
        return when (brevtype) {
            Brevtype.INNVILGELSE, Brevtype.AVSLAG, Brevtype.VEDTAK_ENDRING -> Distribusjonstype.VEDTAK
            Brevtype.VARSEL_OM_BESTILLING -> Distribusjonstype.ANNET
            Brevtype.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT -> Distribusjonstype.VIKTIG // TODO, er det riktig?
            Brevtype.FORVALTNINGSMELDING -> Distribusjonstype.ANNET // TODO, er det riktig?
        }
    }
}

data class DistribuerJournalpostRequest(
    val journalpostId: String,
    val bestillendeFagsystem: String,
    val dokumentProdApp: String,
    val distribusjonstype: Distribusjonstype,
    val distribusjonstidspunkt: Distribusjonstidspunkt
) {
    enum class Distribusjonstype {
        VEDTAK, VIKTIG, ANNET
    }
    enum class Distribusjonstidspunkt {
        UMIDDELBART, KJERNETID
    }
}

data class DistribuerJournalpostResponse(val bestillingsId: String)
