package no.nav.aap.brev.test.fakes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.brev.arkivoppslag.Journalpost
import no.nav.aap.brev.arkivoppslag.Journalpost.Dokument
import no.nav.aap.brev.arkivoppslag.Journalpost.Dokument.Variant
import no.nav.aap.brev.arkivoppslag.Journalpost.Sak
import no.nav.aap.brev.arkivoppslag.SafJournalpostData
import no.nav.aap.brev.arkivoppslag.SafJournalpostVariables
import no.nav.aap.brev.bestilling.Saksnummer
import no.nav.aap.brev.journalføring.DokumentInfoId
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.util.graphql.GraphQLResponse
import no.nav.aap.brev.util.graphql.GraphqlRequest

private val journalpostTilSafJournalpostData = mutableMapOf<JournalpostId, SafJournalpostData>()

fun gittJournalpostIArkivet(
    journalpostId: JournalpostId,
    saksnummer: Saksnummer,
    dokumentInfoId: DokumentInfoId,
    journalstatus: String = "FERDIGSTILT",
    brukerHarTilgangTilJournalpost: Boolean = true,
    fagsaksystem: String = "KELVIN",
    sakstype: String = "FAGSAK",
    tema: String = "AAP",
    brukerHarTilgangTilDokument: Boolean = true,
): Journalpost {
    val dokument = Dokument(
        dokumentInfoId = dokumentInfoId,
        dokumentvarianter = listOf(Variant(brukerHarTilgang = brukerHarTilgangTilDokument))
    )
    val journalpost = Journalpost(
        journalpostId = journalpostId,
        journalstatus = journalstatus,
        brukerHarTilgang = brukerHarTilgangTilJournalpost,
        sak = Sak(
            fagsakId = saksnummer.nummer,
            fagsaksystem = fagsaksystem,
            sakstype = sakstype,
            tema = tema
        ),
        dokumenter = listOf(dokument)
    )

    safJournalpost(journalpost)

    return journalpost
}

fun safJournalpost(journalpost: Journalpost) {
    journalpostTilSafJournalpostData[journalpost.journalpostId] = SafJournalpostData(journalpost)
}

fun Application.safFake() {
    applicationFakeFelles("saf")
    routing {
        post("/graphql") {
            val request = call.receive<GraphqlRequest<SafJournalpostVariables>>()
            val data = journalpostTilSafJournalpostData[request.variables.journalpostId]
            val response = GraphQLResponse(
                data,
                emptyList()
            )

            call.respond(response)
        }
    }
}
