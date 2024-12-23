package no.nav.aap.brev.test.fakes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.journalføring.DokumentInfoId
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.journalføring.OpprettJournalpostRequest
import no.nav.aap.brev.journalføring.OpprettJournalpostResponse
import no.nav.aap.brev.journalføring.OpprettJournalpostResponse.Dokument
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.util.*
import kotlin.random.Random
import kotlin.random.nextULong

private val referanseTilJournalpost = mutableMapOf<BrevbestillingReferanse, JournalpostId>()
private val referanseTilJournalpostFinnesAllerede = mutableMapOf<BrevbestillingReferanse, Boolean>()

fun journalpostForBestilling(
    referanse: BrevbestillingReferanse,
    journalpostId: JournalpostId,
    finnesAllerede: Boolean = false
) {
    referanseTilJournalpost[referanse] = journalpostId
    referanseTilJournalpostFinnesAllerede[referanse] = finnesAllerede
}

fun randomJournalpostId(): JournalpostId {
    return JournalpostId(Random.nextULong().toString())
}

fun randomDokumentInfoId(): DokumentInfoId {
    return DokumentInfoId(Random.nextULong().toString())
}

fun Application.dokarkivFake() {
    applicationFakeFelles("dokarkiv")
    routing {
        post("/rest/journalpostapi/v1/journalpost") {
            val request = DefaultJsonMapper.fromJson<OpprettJournalpostRequest>(call.receiveText())
            val journalpostId = referanseTilJournalpost.get(BrevbestillingReferanse(request.eksternReferanseId))
                ?: randomJournalpostId()
            val status =
                if (referanseTilJournalpostFinnesAllerede
                        .getOrDefault(BrevbestillingReferanse(request.eksternReferanseId), false)
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
