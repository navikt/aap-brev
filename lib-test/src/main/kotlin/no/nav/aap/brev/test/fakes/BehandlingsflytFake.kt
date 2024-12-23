package no.nav.aap.brev.test.fakes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.receiveText
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.LøsBrevbestillingDto
import no.nav.aap.brev.bestilling.BehandlingReferanse
import no.nav.aap.brev.bestilling.BehandlingsflytGateway
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.Personinfo
import no.nav.aap.brev.bestilling.Saksnummer
import no.nav.aap.brev.innhold.Faktagrunnlag
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.util.UUID
import kotlin.random.Random
import kotlin.random.nextInt

private val feilForBestilling = mutableSetOf<UUID>()

private val behandlingsReferanseTilFaktagrunnlag = mutableMapOf<BehandlingReferanse, Set<Faktagrunnlag>>()

fun feilLøsBestillingFor(bestilling: BrevbestillingReferanse) {
    feilForBestilling.add(bestilling.referanse)
}

fun faktagrunnlagForBehandling(behandlingReferanse: BehandlingReferanse, faktagrunnlag: Set<Faktagrunnlag>) {
    behandlingsReferanseTilFaktagrunnlag.put(behandlingReferanse, faktagrunnlag)
}

fun randomBehandlingReferanse() = BehandlingReferanse(UUID.randomUUID())

fun randomSaksnummer() = Saksnummer(Random.nextInt(1000..9999).toString())

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
        post("/api/brev/faktagrunnlag") {
            val dto = DefaultJsonMapper.fromJson<BehandlingsflytGateway.HentFaktaGrunnlagRequest>(call.receiveText())

            val faktagrunnlagResult = behandlingsReferanseTilFaktagrunnlag[dto.behandlingReferanse] ?: emptySet()

            call.respond(BehandlingsflytGateway.HentFaktaGrunnlagResponse(faktagrunnlagResult))
        }
    }
}
