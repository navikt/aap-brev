package no.nav.aap.brev.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.SaksbehandlingPdfGenGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.authorizedGetWithApprovedList
import javax.sql.DataSource

fun NormalOpenAPIRoute.forhåndsvisApi(dataSource: DataSource) {

    val behandlingsflytAzp = requiredConfigForKey("integrasjon.behandlingsflyt.azp")

    route("/api/forhandsvis/{referanse}") {
        authorizedGetWithApprovedList<BrevbestillingReferansePathParam, ByteArray>(
            behandlingsflytAzp
        ) {
            val brevbestilling = dataSource.transaction { connection ->
                BrevbestillingRepositoryImpl(connection).hent(it.brevbestillingReferanse)
            }
            val pdf = SaksbehandlingPdfGenGateway().genererPdf(brevbestilling.brev!!)
            respond(pdf.bytes)
        }
    }
}
