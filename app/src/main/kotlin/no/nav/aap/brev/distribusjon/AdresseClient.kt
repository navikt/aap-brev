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

class AdresseClient(
    private val httpClient: HttpClient,
    private val azureAdTokenClient: AzureAdTokenClient,
) {
    private val scope = "POSTADRESSE_SCOPE"
    private val url = "/rest/postadresse"
    private val NAV_CALL_ID = "aap-brev"

    suspend fun hentPostadresse(personident: String): Postadresse {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post(url) {
            header(HttpHeaders.Authorization, token)
            header("Nav-Callid", NAV_CALL_ID)
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(BestemAdresseRequest(personident)))
        }
        if (!response.status.isSuccess()) {
            error("Kunne ikke hente postadresse, status: ${response.status} ${response.bodyAsText()}")
        }
        return response.body<BestemAdresseResponse>().adresse
    }
}

data class BestemAdresseRequest(
    val ident: String,
)

data class BestemAdresseResponse(
    val navn: String,
    val adresse: Postadresse
)

data class Postadresse(
    val adresseKilde: String,
    val type: String,
    val adresselinje1: String,
    val adresselinje2: String,
    val adresselinje3: String,
    val postnummer: String,
    val poststed: String,
    val landkode: String,
    val land: String,
)
