package no.nav.aap.brev.test.fakes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.brev.distribusjon.DistribuerJournalpostRequest
import no.nav.aap.brev.distribusjon.DistribuerJournalpostResponse
import no.nav.aap.brev.distribusjon.DistribusjonBestillingId
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import java.util.*

private val journalpostTilDistribusjonBestillingId = mutableMapOf<JournalpostId, DistribusjonBestillingId>()
fun distribusjonBestillingIdForJournalpost(
    journalpost: JournalpostId,
    distribusjonBestillingId: DistribusjonBestillingId
) {
    journalpostTilDistribusjonBestillingId.set(journalpost, distribusjonBestillingId)
}

fun randomDistribusjonBestillingId(): DistribusjonBestillingId {
    return DistribusjonBestillingId(UUID.randomUUID().toString())
}

fun Application.dokdistfordelingFake() {
    applicationFakeFelles("dokdistfordeling")
    routing {
        post("/rest/v1/distribuerjournalpost") {
            val request = DefaultJsonMapper.fromJson<DistribuerJournalpostRequest>(call.receiveText())
            val distribusjonBestillingId =
                journalpostTilDistribusjonBestillingId.get(JournalpostId(request.journalpostId))
                    ?: randomDistribusjonBestillingId()
            call.respond(
                DistribuerJournalpostResponse(distribusjonBestillingId.id)
            )
        }
    }
}