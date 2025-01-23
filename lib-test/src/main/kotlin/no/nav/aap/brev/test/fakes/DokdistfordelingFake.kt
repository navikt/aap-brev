package no.nav.aap.brev.test.fakes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.brev.distribusjon.DistribuerJournalpostRequest
import no.nav.aap.brev.distribusjon.DistribuerJournalpostResponse
import no.nav.aap.brev.distribusjon.DistribusjonBestillingId
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.test.randomDistribusjonBestillingId
import no.nav.aap.komponenter.json.DefaultJsonMapper

private val journalpostTilDistribusjonBestillingId = mutableMapOf<JournalpostId, DistribusjonBestillingId>()
private val journalpostTilDistribusjonBestillingFinnesAllerede = mutableMapOf<JournalpostId, Boolean>()
fun distribusjonBestillingIdForJournalpost(
    journalpost: JournalpostId,
    distribusjonBestillingId: DistribusjonBestillingId,
    finnesAllerede: Boolean = false
) {
    journalpostTilDistribusjonBestillingId.set(journalpost, distribusjonBestillingId)
    journalpostTilDistribusjonBestillingFinnesAllerede.set(journalpost, finnesAllerede)
}

fun Application.dokdistfordelingFake() {
    applicationFakeFelles("dokdistfordeling")
    routing {
        post("/rest/v1/distribuerjournalpost") {
            val request = DefaultJsonMapper.fromJson<DistribuerJournalpostRequest>(call.receiveText())
            val status =
                if (journalpostTilDistribusjonBestillingFinnesAllerede
                        .getOrDefault(JournalpostId(request.journalpostId), false)
                ) {
                    HttpStatusCode.Conflict
                } else {
                    HttpStatusCode.Created
                }
            val distribusjonBestillingId =
                journalpostTilDistribusjonBestillingId.get(JournalpostId(request.journalpostId))
                    ?: randomDistribusjonBestillingId()
            call.respond(
                status, DistribuerJournalpostResponse(distribusjonBestillingId.id)
            )
        }
    }
}