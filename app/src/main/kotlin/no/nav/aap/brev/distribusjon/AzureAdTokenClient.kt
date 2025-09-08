package no.nav.aap.brev.distribusjon

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import org.slf4j.LoggerFactory

class AzureAdTokenClient(
    private val httpClient: HttpClient,
) {
    private val azureAdTokenUrl = "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"
    private val clientId = "AZURE_APP_CLIENT_ID"
    private val clientSecret = "AZURE_APP_CLIENT_SECRET"

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun getMachineToMachineToken(scope: String): String {
        val token = createMachineToMachineToken(scope)
        return "${token.tokenType} ${token.accessToken}"
    }

    private suspend fun createMachineToMachineToken(scope: String): AzureAdToken {
        val response = httpClient.post(azureAdTokenUrl) {
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("grant_type", "client_credentials")
                        append("client_id", clientId)
                        append("client_secret", clientSecret)
                        append("scope", scope)
                    },
                ),
            )
        }

        if (!response.status.isSuccess()) {
            log.error("Kunne ikke hente AAD-token: ${response.status.value} ${response.bodyAsText()}")
            error("Kunne ikke hente AAD-token")
        }

        return response.body<AzureAdToken>()
    }
}

@JsonNaming(SnakeCaseStrategy::class)
data class AzureAdToken(
    val tokenType: String,
    val accessToken: String,
)
