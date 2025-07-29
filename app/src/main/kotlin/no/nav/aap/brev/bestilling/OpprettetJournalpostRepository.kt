package no.nav.aap.brev.bestilling

import no.nav.aap.brev.distribusjon.DistribusjonBestillingId
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.time.LocalDateTime

data class OpprettetJournalpost(
    val journalpostId: JournalpostId,
    val mottaker: Mottaker,
    val brevbestillingId: BrevbestillingId,
    val ferdigstilt: Boolean,
    val distribusjonBestillingId: DistribusjonBestillingId?,
)

interface JournalpostRepository {
    fun hentHvisEksisterer(mottakerId: Long): OpprettetJournalpost?
    fun hentAlleFor(bestillingsreferanse: BrevbestillingReferanse): List<OpprettetJournalpost>
    fun lagreJournalpostFerdigstilt(journalpostId: JournalpostId, ferdigstilt: Boolean)
    fun lagreJournalpost(
        journalpostId: JournalpostId,
        journalpostFerdigstilt: Boolean,
        mottakerId: Long
    )
    fun lagreDistribusjonBestilling(journalpostId: JournalpostId, distribusjonBestillingId: DistribusjonBestillingId)
}

class JournalpostRepositoryImpl(private val connection: DBConnection) : JournalpostRepository {
    override fun hentHvisEksisterer(mottakerId: Long): OpprettetJournalpost? {
        val query = """
            SELECT JOURNALPOST_ID, MOTTAKER.ID AS MOTTAKER_ID, BESTILLING_MOTTAKER_REFERANSE, IDENT_TYPE, IDENT, FERDIGSTILT, NAVN_OG_ADRESSE, BREVBESTILLING_ID, DISTRIBUSJON_BESTILLING_ID FROM OPPRETTET_JOURNALPOST
            INNER JOIN MOTTAKER ON OPPRETTET_JOURNALPOST.MOTTAKER_ID = MOTTAKER.ID
            WHERE MOTTAKER_ID = ?
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, mottakerId)
            }
            setRowMapper { row -> mapOpprettetJournalpost(row) }
        }
    }

    override fun hentAlleFor(bestillingsreferanse: BrevbestillingReferanse): List<OpprettetJournalpost> {
        val query = """
            SELECT OJ.*, M.ID as MOTTAKER_ID, M.BREVBESTILLING_ID, M.BESTILLING_MOTTAKER_REFERANSE, M.IDENT_TYPE, M.IDENT, M.NAVN_OG_ADRESSE, OJ.DISTRIBUSJON_BESTILLING_ID
            FROM OPPRETTET_JOURNALPOST OJ
            INNER JOIN MOTTAKER M ON OJ.MOTTAKER_ID = M.ID
            INNER JOIN BREVBESTILLING B ON M.BREVBESTILLING_ID = B.ID
            WHERE B.REFERANSE = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setUUID(1, bestillingsreferanse.referanse)
            }
            setRowMapper { row -> mapOpprettetJournalpost(row) }
        }
    }

    override fun lagreJournalpostFerdigstilt(
        journalpostId: JournalpostId,
        ferdigstilt: Boolean
    ) {
        val query = """
            UPDATE OPPRETTET_JOURNALPOST
            SET FERDIGSTILT = ?, OPPDATERT_TID = ?
            WHERE JOURNALPOST_ID = ?
        """.trimIndent()
        connection.executeReturnKey(query) {
            setParams {
                setBoolean(1, ferdigstilt)
                setLocalDateTime(2, LocalDateTime.now())
                setString(3, journalpostId.id)
            }
            setResultValidator {
                require(1 == it)
            }
        }
    }

    override fun lagreJournalpost(
        journalpostId: JournalpostId,
        journalpostFerdigstilt: Boolean,
        mottakerId: Long
    ) {
        val query = """
            INSERT INTO OPPRETTET_JOURNALPOST (JOURNALPOST_ID, FERDIGSTILT, MOTTAKER_ID)
            VALUES (?, ?, ?)
        """.trimIndent()
        connection.execute(query) {
            setParams {
                setString(1, journalpostId.id)
                setBoolean(2, journalpostFerdigstilt)
                setLong(3, mottakerId)
            }
            setResultValidator {
                require(1 == it) { "Kunne ikke lagre journalpost for mottaker med ID ${mottakerId}" }
            }
        }
    }

    override fun lagreDistribusjonBestilling(
        journalpostId: JournalpostId,
        distribusjonBestillingId: DistribusjonBestillingId
    ) {
        connection.execute(
            "UPDATE OPPRETTET_JOURNALPOST SET DISTRIBUSJON_BESTILLING_ID = ? WHERE JOURNALPOST_ID = ?"
        ) {
            setParams {
                setString(1, distribusjonBestillingId.id)
                setString(2, journalpostId.id)
            }
            setResultValidator {
                require(1 == it) { "Kunne ikke oppdatere distribusjon bestilling for journalpost med id ${journalpostId.id}" }
            }
        }
    }


    private fun mapOpprettetJournalpost(row: Row): OpprettetJournalpost {
        return OpprettetJournalpost(
            journalpostId = JournalpostId(row.getString("JOURNALPOST_ID")),
            mottaker = Mottaker(
                id = row.getLong("MOTTAKER_ID"),
                identType = row.getEnumOrNull("IDENT_TYPE"),
                ident = row.getStringOrNull("IDENT"),
                navnOgAdresse = row.getStringOrNull("NAVN_OG_ADRESSE")
                    ?.let { DefaultJsonMapper.fromJson<NavnOgAdresse>(it) },
                bestillingMottakerReferanse = row.getString("BESTILLING_MOTTAKER_REFERANSE"),
            ),
            brevbestillingId = BrevbestillingId(row.getLong("BREVBESTILLING_ID")),
            ferdigstilt = row.getBoolean("FERDIGSTILT"),
            distribusjonBestillingId = row.getStringOrNull("DISTRIBUSJON_BESTILLING_ID")
                ?.let { DistribusjonBestillingId(it) }
        )
    }
}