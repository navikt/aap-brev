package no.nav.aap.brev.api

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.prosessering.ProsesseringStatus
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

data class SaksnummerParam(@PathParam("saksnummer") val saksnummer: String)
data class BehandlingReferanseParam(@PathParam("referanse") val referanse: UUID)

fun NormalOpenAPIRoute.driftApi(dataSource: DataSource) {
    route("/api/drift") {
        route("/bestillinger/sak/{saksnummer}").authorizedGet<SaksnummerParam, List<BrevbestillingDriftsinfoDto>>(
            AuthorizationParamPathConfig(
                sakPathParam = SakPathParam("saksnummer"),
                operasjon = Operasjon.DRIFTE
            )
        ) { params ->
            val bestillinger = dataSource.transaction { connection ->
                BrevbestillingRepository.konstruer(connection)
                    .hentAlleForSak(params.saksnummer)
                    .map(Brevbestilling::mapTilDriftsinfoDto)
            }

            respond(bestillinger)
        }

        route("/bestillinger/behandling/{referanse}").authorizedGet<BehandlingReferanseParam, List<BrevbestillingDriftsinfoDto>>(
            AuthorizationParamPathConfig(
                behandlingPathParam = BehandlingPathParam("referanse"),
                operasjon = Operasjon.DRIFTE
            )
        ) { params ->
            val bestillinger = dataSource.transaction { connection ->
                BrevbestillingRepository.konstruer(connection)
                    .hentAlleForBehandling(params.referanse)
                    .map(Brevbestilling::mapTilDriftsinfoDto)
            }

            respond(bestillinger)
        }
    }
}

private data class BrevbestillingDriftsinfoDto(
    val id: Long,
    val bestillingReferanse: String,
    val opprettet: LocalDateTime,
    val oppdatert: LocalDateTime,
    val behandlingReferanse: String,
    val brevtype: String,
    val språk: String,
    val status: Status?,
    val prosesseringStatus: ProsesseringStatus?,
)

private fun Brevbestilling.mapTilDriftsinfoDto() = BrevbestillingDriftsinfoDto(
    id = id.id,
    bestillingReferanse = referanse.referanse.toString(),
    opprettet = opprettet,
    oppdatert = oppdatert,
    behandlingReferanse = behandlingReferanse.referanse.toString(),
    brevtype = brevtype.name,
    språk = språk.name,
    status = status,
    prosesseringStatus = prosesseringStatus,
)