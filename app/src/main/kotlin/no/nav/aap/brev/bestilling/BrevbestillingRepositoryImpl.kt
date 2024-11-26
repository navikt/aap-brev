package no.nav.aap.brev.bestilling

import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.prosessering.ProsesseringStatus
import no.nav.aap.brev.exception.BestillingForBehandlingEksistererException
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import org.postgresql.util.PSQLException
import java.util.UUID

class BrevbestillingRepositoryImpl(private val connection: DBConnection) : BrevbestillingRepository {

    private val UNIQUE_VIOATION = "23505"

    override fun opprettBestilling(
        saksnummer: Saksnummer,
        behandlingReferanse: BehandlingReferanse,
        brevtype: Brevtype,
        språk: Språk,
    ): BrevbestillingReferanse {
        val referanse: UUID = UUID.randomUUID()
        val query = """
            INSERT INTO BREVBESTILLING (SAKSNUMMER, REFERANSE, BEHANDLING_REFERANSE, SPRAK, BREVTYPE)
                VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        try {
            connection.execute(query) {
                setParams {
                    setString(1, saksnummer.nummer)
                    setUUID(2, referanse)
                    setUUID(3, behandlingReferanse.referanse)
                    setEnumName(4, språk)
                    setEnumName(5, brevtype)
                }
            }
        } catch (e: PSQLException) {
            if (e.sqlState == UNIQUE_VIOATION) {
                throw BestillingForBehandlingEksistererException()
            }
            throw e
        }
        return BrevbestillingReferanse(referanse)
    }

    override fun hent(referanse: BrevbestillingReferanse): Brevbestilling {
        return connection.queryFirst(
            "SELECT * FROM BREVBESTILLING WHERE REFERANSE = ?"
        ) {
            setParams {
                setUUID(1, referanse.referanse)
            }
            setRowMapper { row ->
                Brevbestilling(
                    id = BrevbestillingId(row.getLong("ID")),
                    saksnummer = Saksnummer(row.getString("SAKSNUMMER")),
                    referanse = BrevbestillingReferanse(row.getUUID("REFERANSE")),
                    brev = row.getStringOrNull("BREV")?.let { DefaultJsonMapper.fromJson<Brev>(it) },
                    opprettet = row.getLocalDateTime("OPPRETTET_TID"),
                    oppdatert = row.getLocalDateTime("OPPDATERT_TID"),
                    behandlingReferanse = BehandlingReferanse(row.getUUID("BEHANDLING_REFERANSE")),
                    brevtype = row.getEnum("BREVTYPE"),
                    språk = row.getEnum("SPRAK"),
                    prosesseringStatus = row.getEnumOrNull("PROSESSERING_STATUS"),
                    journalpostId = row.getStringOrNull("JOURNALPOST_ID")?.let { JournalpostId(it) },
                )
            }
        }
    }

    override fun oppdaterBrev(
        referanse: BrevbestillingReferanse,
        brev: Brev
    ) {
        connection.execute(
            "UPDATE BREVBESTILLING SET BREV = ?::jsonb WHERE REFERANSE = ?"
        ) {
            setParams {
                setString(1, DefaultJsonMapper.toJson(brev))
                setUUID(2, referanse.referanse)
            }
            setResultValidator {
                require(1 == it)
            }
        }
    }

    override fun oppdaterProsesseringStatus(
        referanse: BrevbestillingReferanse,
        prosesseringStatus: ProsesseringStatus,
    ) {
        connection.execute(
            "UPDATE BREVBESTILLING SET PROSESSERING_STATUS = ? WHERE REFERANSE = ?"
        ) {
            setParams {
                setEnumName(1, prosesseringStatus)
                setUUID(2, referanse.referanse)
            }
            setResultValidator {
                require(1 == it)
            }
        }
    }

    override fun lagreJournalpost(
        id: BrevbestillingId,
        journalpostId: JournalpostId
    ) {
        connection.execute(
            "UPDATE BREVBESTILLING SET JOURNALPOST_ID = ? WHERE ID = ?"
        ) {
            setParams {
                setString(1, journalpostId.id)
                setLong(2, id.id)
            }
            setResultValidator {
                require(1 == it)
            }
        }
    }
}
