package no.nav.aap.brev.test.fakes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.receiveText
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.LøsBrevbestillingDto
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.Personinfo
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import java.util.UUID

private val feilForBestilling = mutableSetOf<UUID>()
fun feilLøsBestillingFor(bestilling: BrevbestillingReferanse) {
    feilForBestilling.add(bestilling.referanse)
}

fun Application.behandlingsflytFake() {
    applicationFakeFelles("behandlingsflyt")
    routing {
        post("/api/brev/los-bestilling") {
            val dto = DefaultJsonMapper.fromJson<LøsBrevbestillingDto>(call.receiveText())
            if (feilForBestilling.contains(dto.bestillingReferanse)) {
                call.respond(HttpStatusCode.InternalServerError)
            }
            call.respond(HttpStatusCode.Accepted, "{}")
        }
        get("/api/sak/{saksnummer}/personinformasjon") {
            call.respond(Personinfo("", ""))
        }
    }
}
