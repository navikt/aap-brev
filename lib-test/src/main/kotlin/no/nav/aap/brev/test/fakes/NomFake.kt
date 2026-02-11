package no.nav.aap.brev.test.fakes

import io.ktor.server.application.Application
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.nav.aap.brev.organisasjon.NomData
import no.nav.aap.brev.organisasjon.NomRessursVariables
import no.nav.aap.brev.util.graphql.GraphQLResponse
import no.nav.aap.brev.util.graphql.GraphqlRequest
import no.nav.aap.komponenter.json.DefaultJsonMapper

private val navIdentTilNomData = mutableMapOf<String, NomData>()

fun nomDataForNavIdent(navIdent: String, nomData: NomData) {
    navIdentTilNomData.put(navIdent, nomData)
}

fun Application.nomFake() {
    applicationFakeFelles("nom")
    routing {
        post("/graphql") {
            val request = DefaultJsonMapper.fromJson<GraphqlRequest<NomRessursVariables>>(call.receiveText())
            val response = GraphQLResponse(
                navIdentTilNomData[request.variables.navIdent],
                emptyList()
            )

            call.respond(response)
        }
    }
}
