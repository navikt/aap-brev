package no.nav.aap.brev.test.fakes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.brev.person.Navn
import no.nav.aap.brev.person.PdlPersonData
import no.nav.aap.brev.util.graphql.GraphQLResponse

fun Application.pdlFake() {
    applicationFakeFelles("pdl")
    routing {
        post("/graphql") {
            val data = PdlPersonData(listOf(Navn("", "", "")), emptyList())
            val response = GraphQLResponse(
                data,
                emptyList()
            )

            call.respond(response)
        }
    }
}
