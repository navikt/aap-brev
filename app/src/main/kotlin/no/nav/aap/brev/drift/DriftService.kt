package no.nav.aap.brev.drift

import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.JournalpostRepository
import no.nav.aap.brev.bestilling.SorterbarSignatur
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.prosessering.ProsesseringStatus
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class DriftService(private val dataSource: DataSource) {

    fun hentAlleForSak(saksnummer: String): List<BrevbestillingDriftsinfoDto> {
        return dataSource.transaction(readOnly = true) { connection ->
            BrevbestillingRepository.konstruer(connection)
                .hentAlleForSak(saksnummer)
                .map {
                    val journalpostIder = hentJournalpostIder(connection, it.referanse)

                    it.mapTilDriftsinfoDto(journalpostIder)
                }
        }
    }

    fun hentAlleForBehandling(referanse: UUID): List<BrevbestillingDriftsinfoDto> {
        return dataSource.transaction(readOnly = true) { connection ->
            BrevbestillingRepository.konstruer(connection)
                .hentAlleForBehandling(referanse)
                .map {
                    val journalpostIder = hentJournalpostIder(connection, it.referanse)

                    it.mapTilDriftsinfoDto(journalpostIder)
                }
        }
    }

    private fun hentJournalpostIder(
        connection: DBConnection,
        brevbestillingReferanse: BrevbestillingReferanse
    ): List<String> {
        return JournalpostRepository.konstruer(connection)
            .hentAlleFor(brevbestillingReferanse)
            .map { journalpost -> journalpost.journalpostId.id }
    }

    private fun Brevbestilling.mapTilDriftsinfoDto(journalpostIder: List<String>) = BrevbestillingDriftsinfoDto(
        id = id.id,
        bestillingReferanse = referanse.referanse.toString(),
        journalpostId = journalpostIder.joinToString(),
        opprettet = opprettet,
        oppdatert = oppdatert,
        behandlingReferanse = behandlingReferanse.referanse.toString(),
        brevtype = brevtype.name,
        språk = språk.name,
        status = status,
        prosesseringStatus = prosesseringStatus,
        signaturer = signaturer,
    )
}

data class BrevbestillingDriftsinfoDto(
    val id: Long,
    val bestillingReferanse: String,
    val journalpostId: String?,
    val opprettet: LocalDateTime,
    val oppdatert: LocalDateTime,
    val behandlingReferanse: String,
    val brevtype: String,
    val språk: String,
    val status: Status?,
    val prosesseringStatus: ProsesseringStatus?,
    val signaturer: List<SorterbarSignatur>,
)
