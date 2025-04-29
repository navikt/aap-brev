package no.nav.aap.brev.person

import no.nav.aap.brev.bestilling.Personinfo
import no.nav.aap.brev.bestilling.PersoninfoGateway
import no.nav.aap.brev.prometheus
import no.nav.aap.brev.util.graphql.GraphQLResponse
import no.nav.aap.brev.util.graphql.GraphQLResponseHandler
import no.nav.aap.brev.util.graphql.GraphqlRequest
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.net.URI

class PdlGateway : PersoninfoGateway {
    private val graphqlUrl = URI.create(requiredConfigForKey("integrasjon.pdl.url"))
    private val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.pdl.scope"),
        additionalHeaders = listOf(Header("Behandlingsnummer", "B287")),
    )

    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = GraphQLResponseHandler(),
        prometheus = prometheus,
    )

    override fun hentPersoninfo(personIdent: String): Personinfo {
        val request = GraphqlRequest(personinfoQuery, HentPersonVariables(personIdent))
        val response = checkNotNull(query(request).data) {
            "Fant ikke person i PDL"
        }
        return mapResponse(personIdent, checkNotNull(response.hentPerson))
    }

    private fun query(request: GraphqlRequest<HentPersonVariables>): GraphQLResponse<PdlPersonData> {
        val httpRequest = PostRequest(body = request)
        return requireNotNull(client.post(uri = graphqlUrl, request = httpRequest))
    }

    private fun mapResponse(personIdent: String, pdlPerson: PdlPerson): Personinfo {
        return Personinfo(
            navn = pdlPerson.navn.single().navn(),
            personIdent = personIdent,
            harStrengtFortroligAdresse = harStrengtFortroligAdresse(pdlPerson),
        )
    }

    private fun harStrengtFortroligAdresse(pdlPerson: PdlPerson): Boolean {
        return when (pdlPerson.adressebeskyttelse.gjeldende()?.gradering) {
            Gradering.STRENGT_FORTROLIG, Gradering.STRENGT_FORTROLIG_UTLAND -> true
            else -> false
        }
    }

    fun Navn.navn(): String = mellomnavn?.let { "$fornavn $it $etternavn" } ?: "$fornavn $etternavn"

    fun List<Adressebeskyttelse>.gjeldende(): Adressebeskyttelse? = this.find { !it.metadata.historisk }

}

private const val ident = "\$ident"
val personinfoQuery = """
    query($ident: ID!) {
        hentPerson(ident: $ident) {
            navn(historikk: false) {
                fornavn, mellomnavn, etternavn,
            }
            adressebeskyttelse {
                gradering
                metadata {
                    historisk
                }
            }
        }
    }
""".trimIndent()
