package no.nav.aap.brev.test.fakes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.journalføring.OpprettJournalpostRequest
import no.nav.aap.brev.journalføring.OpprettJournalpostResponse
import no.nav.aap.brev.journalføring.OpprettJournalpostResponse.Dokument
import no.nav.aap.brev.test.randomJournalpostId
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.util.*

private val feilJournalføringFor = mutableSetOf<String>()
fun feilJournalføringFor(eksternReferanse: String) {
    feilJournalføringFor.add(eksternReferanse)
}
private val referanseTilJournalpost = mutableMapOf<String, JournalpostId>()
private val referanseTilJournalpostFinnesAllerede = mutableMapOf<String, Boolean>()

fun journalpostForBestilling(
    referanse: String,
    journalpostId: JournalpostId,
    finnesAllerede: Boolean = false
) {
    referanseTilJournalpost[referanse] = journalpostId
    referanseTilJournalpostFinnesAllerede[referanse] = finnesAllerede
}

fun Application.dokarkivFake() {
    applicationFakeFelles("dokarkiv")
    routing {
        post("/rest/journalpostapi/v1/journalpost") {
            val request = DefaultJsonMapper.fromJson<OpprettJournalpostRequest>(call.receiveText())
            if (feilJournalføringFor.contains(request.eksternReferanseId)) {
                call.respond(HttpStatusCode.InternalServerError)
            }
            val journalpostId = referanseTilJournalpost.get(request.eksternReferanseId)
                ?: randomJournalpostId()
            val status =
                if (referanseTilJournalpostFinnesAllerede
                        .getOrDefault(request.eksternReferanseId, false)
                ) {
                    HttpStatusCode.Conflict
                } else {
                    HttpStatusCode.Created
                }

            call.respond(
                status,
                OpprettJournalpostResponse(
                    journalpostId = journalpostId,
                    journalpostferdigstilt = true,
                    dokumenter = listOf(Dokument(UUID.randomUUID().toString())),
                )
            )
        }
    }
}
