package no.nav.aap.brev.test.fakes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.Faktagrunnlag
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.FaktagrunnlagDto
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.HentFaktaGrunnlagRequest
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.LøsBrevbestillingDto
import no.nav.aap.brev.bestilling.BehandlingReferanse
import no.nav.aap.komponenter.json.DefaultJsonMapper

private val behandlingsReferanseTilFaktagrunnlag = mutableMapOf<no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse, Set<Faktagrunnlag>>()

fun faktagrunnlagForBehandling(behandlingReferanse: BehandlingReferanse, faktagrunnlag: Set<Faktagrunnlag>) {
    val nyBehandlingsReferanse = no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse(behandlingReferanse.referanse)
    behandlingsReferanseTilFaktagrunnlag.put(nyBehandlingsReferanse, faktagrunnlag)
}

fun Application.behandlingsflytFake() {
    applicationFakeFelles("behandlingsflyt")
    routing {
        post("/api/brev/los-bestilling") {
            DefaultJsonMapper.fromJson<LøsBrevbestillingDto>(call.receiveText())
            call.respond(HttpStatusCode.Accepted, "{}")
        }
        post("/api/brev/faktagrunnlag") {
            val dto = DefaultJsonMapper.fromJson<HentFaktaGrunnlagRequest>(call.receiveText())

            val faktagrunnlagResult = behandlingsReferanseTilFaktagrunnlag[dto.behandlingReferanse] ?: emptySet()

            call.respond(FaktagrunnlagDto(faktagrunnlagResult.toList()))
        }
    }
}
