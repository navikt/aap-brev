package no.nav.aap.brev.test.fakes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.nav.aap.brev.bestilling.Personinfo

fun Application.behandlingsflytFake() {
    applicationFakeFelles("behandlingsflyt")
    routing {
        post("/api/brev/los-bestilling") {
            call.respond(HttpStatusCode.Accepted, "{}")
        }
        get("/api/sak/{saksnummer}/personinformasjon") {
            call.respond(Personinfo("", ""))
        }
    }
}
