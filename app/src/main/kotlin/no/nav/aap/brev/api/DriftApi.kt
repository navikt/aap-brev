package no.nav.aap.brev.api

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.brev.drift.BrevbestillingDriftsinfoDto
import no.nav.aap.brev.drift.DriftService
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import java.util.UUID

data class SaksnummerParam(@param:PathParam("saksnummer") val saksnummer: String)
data class BehandlingReferanseParam(@param:PathParam("referanse") val referanse: UUID)

fun NormalOpenAPIRoute.driftApi(service: DriftService) {
    route("/api/drift") {
        route("/bestillinger/sak/{saksnummer}").authorizedGet<SaksnummerParam, List<BrevbestillingDriftsinfoDto>>(
            AuthorizationParamPathConfig(
                sakPathParam = SakPathParam("saksnummer"),
                operasjon = Operasjon.DRIFT_LES
            )
        ) { params ->
            val bestillinger = service.hentAlleForSak(params.saksnummer)
            respond(bestillinger)
        }

        route("/bestillinger/behandling/{referanse}").authorizedGet<BehandlingReferanseParam, List<BrevbestillingDriftsinfoDto>>(
            AuthorizationParamPathConfig(
                behandlingPathParam = BehandlingPathParam("referanse"),
                operasjon = Operasjon.DRIFT_LES
            )
        ) { params ->
            val bestillinger = service.hentAlleForBehandling(params.referanse)
            respond(bestillinger)
        }
    }
}
