package no.nav.aap.brev.distribusjon

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.JournalpostRepository
import no.nav.aap.brev.bestilling.JournalpostRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection


class DistribusjonService(
    private val brevbestillingRepository: BrevbestillingRepository,
    private val journalpostRepository: JournalpostRepository,
    private val distribusjonGateway: DistribusjonGateway,
) {
    companion object {
        fun konstruer(connection: DBConnection): DistribusjonService {
            return DistribusjonService(
                BrevbestillingRepositoryImpl(connection),
                JournalpostRepositoryImpl(connection),
                DokdistfordelingGateway()
            )
        }
    }

    suspend fun kanBrevDistribueres(personIndent: String): Boolean {
        val kanal = hentDistribusjonskanal(personIndent)
        return (kanal != Distribusjonskanal.PRINT) || hentPostadresse(personIndent) != null
    }

    suspend fun hentPostadresse(personident: String): Postadresse {
        val httpClient = HttpClient()
        val adresseClient = AdresseClient(httpClient, AzureAdTokenClient(httpClient))
        return adresseClient.hentPostadresse(personident)
    }

    suspend fun hentDistribusjonskanal(brukerId: String): Distribusjonskanal {
        val httpClient = HttpClient()
        var dokdistkanalClient = DokdistkanalClient(httpClient, AzureAdTokenClient(httpClient))
        return dokdistkanalClient.bestemDistribusjonskanal(brukerId)
    }

    fun distribuerBrev(referanse: BrevbestillingReferanse) {
        val brevbestilling = brevbestillingRepository.hent(referanse)
        val journalposter = journalpostRepository.hentAlleFor(referanse)

        check(journalposter.isNotEmpty()) {
            "Kan ikke distribuere en bestilling som ikke er journalført."
        }

        check(journalposter.all { it.ferdigstilt }) {
            "Feiltilstand: Det finnes journalposter for bestillingen som ikke er ferdigstilt."
        }

        journalposter
            .filter { it.distribusjonBestillingId == null }
            .forEach { journalpost ->
                val distribusjonBestillingId = distribusjonGateway.distribuerJournalpost(
                    journalpost.journalpostId,
                    brevbestilling.brevtype,
                    journalpost.mottaker
                )
                // TODO Midlertidig for bakoverkompabilitet
                if (journalpost.mottaker.ident == brevbestilling.brukerIdent) {
                    brevbestillingRepository.lagreDistribusjonBestilling(brevbestilling.id, distribusjonBestillingId)
                }
                journalpostRepository.lagreDistribusjonBestilling(journalpost.journalpostId, distribusjonBestillingId)
            }
    }
}
