package no.nav.aap.brev.api

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.JournalpostRepository
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

data class SaksnummerParam(@param:PathParam("saksnummer") val saksnummer: String)
data class BehandlingReferanseParam(@param:PathParam("referanse") val referanse: UUID)

fun NormalOpenAPIRoute.driftApi(dataSource: DataSource) {
    route("/api/drift") {
        route("/bestillinger/sak/{saksnummer}").authorizedGet<SaksnummerParam, List<BrevbestillingDriftsinfoDto>>(
            AuthorizationParamPathConfig(
                sakPathParam = SakPathParam("saksnummer"),
                operasjon = Operasjon.DRIFT_LES
            )
        ) { params ->
            val bestillinger = dataSource.transaction(readOnly = true) { connection ->
                BrevbestillingRepository.konstruer(connection)
                    .hentAlleForSak(params.saksnummer)
                    .map {
                        val journalpostIder = JournalpostRepository.konstruer(connection)
                            .hentAlleFor(it.referanse)
                            .map { journalpost -> journalpost.journalpostId.id }

                        it.mapTilDriftsinfoDto(journalpostIder)
                    }
            }

            respond(bestillinger)
        }

        route("/bestillinger/behandling/{referanse}").authorizedGet<BehandlingReferanseParam, List<BrevbestillingDriftsinfoDto>>(
            AuthorizationParamPathConfig(
                behandlingPathParam = BehandlingPathParam("referanse"),
                operasjon = Operasjon.DRIFT_LES
            )
        ) { params ->
            val bestillinger = dataSource.transaction(readOnly = true) { connection ->
                BrevbestillingRepository.konstruer(connection)
                    .hentAlleForBehandling(params.referanse)
                    .map {
                        val journalpostIder = JournalpostRepository.konstruer(connection)
                            .hentAlleFor(it.referanse)
                            .map { journalpost -> journalpost.journalpostId.id }

                        it.mapTilDriftsinfoDto(journalpostIder)
                    }
            }

            respond(bestillinger)
        }
    }
}

private data class BrevbestillingDriftsinfoDto(
    val id: Long,
    val bestillingReferanse: String,
    val journalpostId: String?,
    val opprettet: LocalDateTime,
    val oppdatert: LocalDateTime,
    val behandlingReferanse: String,
    val brevtype: String,
    val språk: String,
    val status: Status?,
    val prosesseringStatus: ProsesseringStatus?,
)

private fun Brevbestilling.mapTilDriftsinfoDto(journalpostIder: List<String>) = BrevbestillingDriftsinfoDto(
    id = id.id,
    bestillingReferanse = referanse.referanse.toString(),
    journalpostId = journalpostIder.joinToString(),
    opprettet = opprettet,
    oppdatert = oppdatert,
    behandlingReferanse = behandlingReferanse.referanse.toString(),
    brevtype = brevtype.name,
    språk = språk.name,
    status = status,
    prosesseringStatus = prosesseringStatus,
)