package no.nav.aap.brev.distribusjon

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

val JOURNALPOST_TEMA_OPPFOLGING = "OPP"

class DokdistkanalClient(
    private val httpClient: HttpClient,
    private val azureAdTokenClient: AzureAdTokenClient,
) {
    private val scope = "DOKDISTKANAL_SCOPE"
    private val url = "/rest/bestemDistribusjonskanal"
    private val NAV_CALL_ID = "aap-brev"

    suspend fun bestemDistribusjonskanal(personident: String): Distribusjonskanal {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post(url) {
            header(HttpHeaders.Authorization, token)
            header("Nav-Callid", NAV_CALL_ID)
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(BestemDistribusjonskanalRequest(personident)))
        }
        if (!response.status.isSuccess()) {
            error("Kunne ikke hente distribusjonskanal, status: ${response.status} ${response.bodyAsText()}")
        }
        return response.body<BestemDistribusjonskanalResponse>().distribusjonskanal
    }
}

data class BestemDistribusjonskanalRequest(
    val brukerId: String,
    val mottakerId: String = brukerId,
    val tema: String = JOURNALPOST_TEMA_OPPFOLGING,
    val erArkivert: Boolean = true,
)

data class BestemDistribusjonskanalResponse(
    val distribusjonskanal: Distribusjonskanal,
)
