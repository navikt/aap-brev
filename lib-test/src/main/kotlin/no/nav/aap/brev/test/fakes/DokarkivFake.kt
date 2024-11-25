package no.nav.aap.brev.test.fakes

import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.nav.aap.brev.journalføring.OpprettJournalpostResponse
import no.nav.aap.brev.journalføring.OpprettJournalpostResponse.DokumentInfoId
import java.util.UUID

fun Application.dokarkivFake() {
    applicationFakeFelles("dokarkiv")
    routing {
        post("/rest/journalpostapi/v1/journalpost") {
            call.respond(
                OpprettJournalpostResponse(
                    journalpostId = UUID.randomUUID().toString(),
                    journalpostferdigstilt = true,
                    dokumenter = listOf(DokumentInfoId(UUID.randomUUID().toString())),
                )
            )
        }
    }
}
