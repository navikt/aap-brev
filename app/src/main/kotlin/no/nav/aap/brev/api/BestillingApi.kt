package no.nav.aap.brev.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.put
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.brev.bestilling.BehandlingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.kontrakt.BestillBrevRequest
import no.nav.aap.brev.kontrakt.BestillBrevResponse
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.FerdigstillBrevRequest
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.authorizedGetWithApprovedList
import no.nav.aap.tilgang.authorizedPostWithApprovedList
import no.nav.aap.tilgang.installerTilgangPluginWithApprovedList
import javax.sql.DataSource


fun NormalOpenAPIRoute.bestillingApi(dataSource: DataSource) {

    val behandlingsflytAzp = requiredConfigForKey("integrasjon.behandlingsflyt.azp")

    route("/api") {
        route("/bestill") {
            authorizedPostWithApprovedList<Unit, BestillBrevResponse, BestillBrevRequest>(
                behandlingsflytAzp
            ) { _, request ->
                val referanse = dataSource.transaction { connection ->
                    BrevbestillingService.konstruer(connection).opprettBestilling(
                        behandlingReferanse = BehandlingReferanse(request.behandlingReferanse),
                        brevtype = request.brevtype,
                        språk = request.sprak,
                    )
                }
                respond(BestillBrevResponse(referanse.referanse), HttpStatusCode.Created)
            }
        }
        route("/bestilling") {
            route("/{referanse}") {
                authorizedGetWithApprovedList<BrevbestillingReferansePathParam, BrevbestillingResponse>(
                    behandlingsflytAzp
                ) {
                    val brevbestilling = dataSource.transaction { connection ->
                        BrevbestillingService.konstruer(connection).hent(it.brevbestillingReferanse)
                    }
                    respond(brevbestilling.tilResponse())
                }
                route("/oppdater") {
                    authorizedPutWithApprovedList<BrevbestillingReferansePathParam, Unit, Brev>(
                        behandlingsflytAzp
                    ) { referanse, brev ->
                        dataSource.transaction { connection ->
                            BrevbestillingService.konstruer(connection)
                                .oppdaterBrev(referanse.brevbestillingReferanse, brev)
                        }
                        respondWithStatus(HttpStatusCode.NoContent)
                    }
                }
            }
        }
        route("/ferdigstill") {
            authorizedPostWithApprovedList<Unit, Unit, FerdigstillBrevRequest>(
                behandlingsflytAzp
            ) { _, request ->
                // valider request
                // fortsett prosessering
                respondWithStatus(HttpStatusCode.Accepted)
            }
        }
    }
}

inline fun <reified TParams : Any, reified TResponse : Any, reified TRequest : Any> NormalOpenAPIRoute.authorizedPutWithApprovedList(
    vararg approvedList: String,
    noinline body: suspend OpenAPIPipelineResponseContext<TResponse>.(TParams, TRequest) -> Unit
) {
    ktorRoute.installerTilgangPluginWithApprovedList(approvedList.toList())
    put<TParams, TResponse, TRequest> { params, request -> body(params, request) }
}