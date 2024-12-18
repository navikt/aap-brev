package no.nav.aap.brev.arkivoppslag

import dokumentinnhenting.util.graphql.asQuery
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.util.graphql.GraphQLResponse
import no.nav.aap.brev.util.graphql.GraphQLResponseHandler
import no.nav.aap.brev.util.graphql.GraphqlRequest
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.net.URI

class SafGateway : ArkivoppslagGateway {
    private val graphqlUrl = URI.create(requiredConfigForKey("integrasjon.saf.url.graphql"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.saf.scope"))

    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = GraphQLResponseHandler()
    )

    override fun hentJournalpost(journalpostId: JournalpostId): Journalpost {
        val request = GraphqlRequest(journalpostSakQuery.asQuery(), SafJournalpostVariables(journalpostId.id))
        val response = query(request)
        return checkNotNull(response.data?.journalpost)
    }

    private fun query(request: GraphqlRequest<SafJournalpostVariables>): GraphQLResponse<SafJournalpostData> {
        val httpRequest = PostRequest(body = request)
        return requireNotNull(client.post(uri = graphqlUrl, request = httpRequest))
    }
}

private const val journalpostId = "\$journalpostId"

private val journalpostSakQuery = """
    query($journalpostId: String!) {
        journalpost(journalpostId: $journalpostId) {
            journalstatus
            brukerHarTilgang
            sak {
                fagsakId
                fagsaksystem
                sakstype
                tema
            }
            dokumenter {
                dokumentInfoId
                dokumentvarianter {
                    brukerHarTilgang
                }
            }
        }
    }
""".trimIndent()
