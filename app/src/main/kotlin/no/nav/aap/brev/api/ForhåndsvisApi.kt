package no.nav.aap.brev.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.Personinfo
import no.nav.aap.brev.bestilling.SaksbehandlingPdfGenGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.authorizedGet
import java.time.LocalDate
import javax.sql.DataSource

fun NormalOpenAPIRoute.forhåndsvisApi(dataSource: DataSource) {

    val behandlingsflytAzp = requiredConfigForKey("integrasjon.behandlingsflyt.azp")

    route("/api/forhandsvis/{referanse}") {
        authorizedGet<BrevbestillingReferansePathParam, ByteArray>(
            AuthorizationParamPathConfig(
                approvedApplications = setOf(behandlingsflytAzp),
                applicationsOnly = true
            )
        ) {
            val brevbestilling = dataSource.transaction { connection ->
                BrevbestillingRepositoryImpl(connection).hent(it.brevbestillingReferanse)
            }
            val pdf = SaksbehandlingPdfGenGateway().genererPdf(
                personinfo = Personinfo(fnr = "", navn = ""),
                saksnummer = brevbestilling.saksnummer,
                brevbestilling.brev!!,
                LocalDate.now()
            )
            respond(pdf.bytes)
        }
    }
}
