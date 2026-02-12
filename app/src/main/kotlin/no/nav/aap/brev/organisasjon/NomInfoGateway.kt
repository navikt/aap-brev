package no.nav.aap.brev.organisasjon

import no.nav.aap.brev.prometheus
import no.nav.aap.brev.util.graphql.GraphQLResponse
import no.nav.aap.brev.util.graphql.GraphQLResponseHandler
import no.nav.aap.brev.util.graphql.GraphqlRequest
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate

class NomInfoGateway : AnsattInfoGateway {
    private val log = LoggerFactory.getLogger(javaClass)
    private val graphqlUrl = URI.create(requiredConfigForKey("integrasjon.nom.url"))
    private val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.nom.scope"),
    )

    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = GraphQLResponseHandler(),
        prometheus = prometheus,
    )

    override fun hentAnsattInfo(navIdent: String): AnsattInfo {
        val request = GraphqlRequest(ressursQuery, NomRessursVariables(navIdent))
        val response = checkNotNull(query(request).data) {
            "Fant ikke ansatt i NOM"
        }
        return mapResponse(navIdent, checkNotNull(response.ressurs))
    }

    private fun query(request: GraphqlRequest<NomRessursVariables>): GraphQLResponse<NomData> {
        val httpRequest = PostRequest(body = request)
        return requireNotNull(client.post(uri = graphqlUrl, request = httpRequest))
    }

    private fun mapResponse(navIdent: String, nomDataRessurs: NomDataRessurs): AnsattInfo {
        return AnsattInfo(
            navIdent = navIdent,
            navn = nomDataRessurs.visningsnavn,
            enhetsnummer = finnAnsattEnhetsnummer(nomDataRessurs)
        )
    }

    private fun finnAnsattEnhetsnummer(nomDataRessurs: NomDataRessurs): String {
        val orgTilknytningMedDagligOppfolging = nomDataRessurs.orgTilknytning.filter { it.erDagligOppfolging }
        val orgTilknytning = orgTilknytningMedDagligOppfolging.singleOrNull {
            it.erAktiv()
        } ?: orgTilknytningMedDagligOppfolging.maxByOrNull { it.gyldigTom ?: LocalDate.MAX }?.also {
            log.info("Finner ikke aktiv OrgTilknytning med daglig oppfølging for ansatt, bruker siste OrgTilknytning med daglig oppfølging for å hente enhet til signatur.")
        } ?: nomDataRessurs.orgTilknytning.maxByOrNull { it.gyldigTom ?: LocalDate.MAX }?.also {
            log.info("Finner ikke OrgTilknytning med daglig oppfølging for ansatt, bruker siste OrgTilknytning for å hente enhet til signatur.")
        }

        checkNotNull(orgTilknytning) {
            "Fant ikke OrgTilknytning for ansatt. Klarer ikke utlede enhet for signatur."
        }

        return checkNotNull(orgTilknytning.orgEnhet.remedyEnhetId) {
            "Klarer ikke utlede enhet for signatur. OrgEnhet til OrgTilknytning mangler RemedyEnhetId."
        }
    }

    private fun OrgTilknytning.erAktiv(): Boolean {
        val iDag = LocalDate.now()
        return gyldigFom <= iDag && (gyldigTom == null || gyldigTom >= iDag)
    }
}

private const val navIdent = "\$navIdent"
val ressursQuery = """
    query($navIdent: String!) {
      ressurs(where: {navident: $navIdent}) {
        orgTilknytninger(utvalg: ALLE) {
          orgEnhet {
            remedyEnhetId
          }
          erDagligOppfolging
          gyldigFom
          gyldigTom
        }
        visningsnavn
      }
    }
""".trimIndent()
