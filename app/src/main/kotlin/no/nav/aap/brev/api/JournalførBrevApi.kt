package no.nav.aap.brev.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.brev.bestilling.PdfBrev
import no.nav.aap.brev.bestilling.SaksbehandlingPdfGenGateway
import no.nav.aap.brev.journalføring.DokarkivGateway
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.journalføring.JournalpostInfo
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.authorizedPost
import tilgang.Operasjon


fun NormalOpenAPIRoute.journalførBrevApi() {

    val dokumentinnhentingAzp = requiredConfigForKey("integrasjon.dokumentinnhenting.azp")

    val authorizationBodyPathConfig = AuthorizationBodyPathConfig(
        operasjon = Operasjon.SAKSBEHANDLE,
        approvedApplications = setOf(dokumentinnhentingAzp),
        applicationsOnly = true
    )

    route("/api") {
        route("/journalforbrev") {
            authorizedPost<Unit, JournalpostIdResponse, JournalførBrevRequest>(authorizationBodyPathConfig) { _, request ->
                val pdfGateway = SaksbehandlingPdfGenGateway()
                val arkivGateway = DokarkivGateway()

                val pdf = pdfGateway.genererPdf(request.brev)
                val journalpostId = arkivGateway.journalførBrev(request.journalpostInfo, pdf)

                respond(JournalpostIdResponse(journalpostId), HttpStatusCode.Created)
            }
        }
    }
}

data class JournalførBrevRequest(val journalpostInfo: JournalpostInfo, val brev: PdfBrev)
data class JournalpostIdResponse(val journalpostId: JournalpostId)
