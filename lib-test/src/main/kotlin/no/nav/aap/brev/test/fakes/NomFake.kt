package no.nav.aap.brev.test.fakes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.brev.organisasjon.NomData
import no.nav.aap.brev.organisasjon.NomDataRessurs
import no.nav.aap.brev.util.graphql.GraphQLResponse

fun Application.nomFake() {
    applicationFakeFelles("nom")
    routing {
        post("/graphql") {
            val data = NomData(NomDataRessurs(orgTilknytning = emptyList(), visningsnavn = ""))
            val response = GraphQLResponse(
                data,
                emptyList()
            )

            call.respond(response)
        }
    }
}
