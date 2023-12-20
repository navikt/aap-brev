package brev.routes

import brev.sanity.SanityClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.sanity(sanity: SanityClient) {
    route("/brev") {

        get("/{id}") {
            val id = requireNotNull(call.parameters["id"]) { "parameter 'id' mangler." }

            sanity.brevmal(id)
                .onSuccess { call.respond(HttpStatusCode.OK, it) }
                .onFailure { call.respond(HttpStatusCode.NotFound) }
        }

        get {
            sanity.brevmaler()
                .onSuccess { call.respond(HttpStatusCode.OK, it) }
                .onFailure { call.respond(HttpStatusCode.NotFound) }
        }
    }
}

