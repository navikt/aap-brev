package no.nav.aap.brev.bestilling

import no.nav.aap.brev.distribusjon.DistribusjonBestillingId
import no.nav.aap.brev.exception.ValideringsfeilException
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.prosessering.ProsesseringStatus
import no.nav.aap.brev.journalføring.DokumentInfoId
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.time.LocalDateTime
import java.util.UUID

class BrevbestillingRepositoryImpl(private val connection: DBConnection) : BrevbestillingRepository {

    override fun opprettBestilling(
        saksnummer: Saksnummer,
        behandlingReferanse: BehandlingReferanse,
        unikReferanse: UnikReferanse,
        brevtype: Brevtype,
        språk: Språk,
        vedlegg: Set<Vedlegg>,
    ): Brevbestilling {
        val referanse: UUID = UUID.randomUUID()
        val query = """
            INSERT INTO BREVBESTILLING (SAKSNUMMER, REFERANSE, BEHANDLING_REFERANSE, SPRAK, BREVTYPE, UNIK_REFERANSE)
                VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        val id =
            connection.executeReturnKey(query) {
                setParams {
                    setString(1, saksnummer.nummer)
                    setUUID(2, referanse)
                    setUUID(3, behandlingReferanse.referanse)
                    setEnumName(4, språk)
                    setEnumName(5, brevtype)
                    setString(6, unikReferanse.referanse)
                }
            }

        if (vedlegg.isNotEmpty()) {
            insertVedlegg(id, vedlegg)
        }

        return hent(BrevbestillingReferanse(referanse))
    }

    private fun insertVedlegg(id: Long, vedlegg: Set<Vedlegg>) {

        val query = """
            INSERT INTO VEDLEGG (BREVBESTILLING_ID, JOURNALPOST_ID, DOKUMENT_INFO_ID) VALUES (?, ?, ?)
        """.trimIndent()

        connection.executeBatch(query, vedlegg) {
            setParams {
                setLong(1, id)
                setString(2, it.journalpostId.id)
                setString(3, it.dokumentInfoId.id)
            }
        }
    }

    override fun hent(unikReferanse: UnikReferanse): Brevbestilling? {
        return connection.queryFirstOrNull(
            "SELECT * FROM BREVBESTILLING WHERE UNIK_REFERANSE = ?"
        ) {
            setParams {
                setString(1, unikReferanse.referanse)
            }
            setRowMapper { row -> mapBestilling(row) }
        }
    }

    override fun hent(referanse: BrevbestillingReferanse): Brevbestilling {
        return connection.queryFirst(
            "SELECT * FROM BREVBESTILLING WHERE REFERANSE = ?"
        ) {
            setParams {
                setUUID(1, referanse.referanse)
            }
            setRowMapper { row -> mapBestilling(row) }
        }
    }

    private fun mapBestilling(row: Row): Brevbestilling {
        val id = BrevbestillingId(row.getLong("ID"))

        val vedleggQuery = """
            SELECT * FROM VEDLEGG WHERE BREVBESTILLING_ID = ?
        """.trimIndent()

        val vedlegg = connection.queryList(vedleggQuery) {
            setParams {
                setLong(1, id.id)
            }
            setRowMapper {
                Vedlegg(
                    journalpostId = JournalpostId(it.getString("JOURNALPOST_ID")),
                    dokumentInfoId = DokumentInfoId(it.getString("DOKUMENT_INFO_ID")),
                )
            }
        }.toList()

        return Brevbestilling(
            id = id,
            saksnummer = Saksnummer(row.getString("SAKSNUMMER")),
            referanse = BrevbestillingReferanse(row.getUUID("REFERANSE")),
            brev = row.getStringOrNull("BREV")?.let { DefaultJsonMapper.fromJson<Brev>(it) },
            opprettet = row.getLocalDateTime("OPPRETTET_TID"),
            oppdatert = row.getLocalDateTime("OPPDATERT_TID"),
            behandlingReferanse = BehandlingReferanse(row.getUUID("BEHANDLING_REFERANSE")),
            unikReferanse = UnikReferanse(row.getString("UNIK_REFERANSE")),
            brevtype = row.getEnum("BREVTYPE"),
            språk = row.getEnum("SPRAK"),
            prosesseringStatus = row.getEnumOrNull("PROSESSERING_STATUS"),
            journalpostId = row.getStringOrNull("JOURNALPOST_ID")?.let { JournalpostId(it) },
            journalpostFerdigstilt = row.getBooleanOrNull("JOURNALPOST_FERDIGSTILT"),
            distribusjonBestillingId = row.getStringOrNull("DISTRIBUSJON_BESTILLING_ID")
                ?.let { DistribusjonBestillingId(it) },
            vedlegg = vedlegg.toSet(),
        )
    }

    override fun oppdaterBrev(
        referanse: BrevbestillingReferanse,
        brev: Brev
    ) {
        connection.execute(
            "UPDATE BREVBESTILLING SET BREV = ?::jsonb, OPPDATERT_TID = ? WHERE REFERANSE = ?"
        ) {
            setParams {
                setString(1, DefaultJsonMapper.toJson(brev))
                setLocalDateTime(2, LocalDateTime.now())
                setUUID(3, referanse.referanse)
            }
            setResultValidator {
                if (it != 1) {
                    throw ValideringsfeilException("Forsøkte å oppdatere brevbestilling som ikke finnes.")
                }
            }
        }
    }

    override fun oppdaterProsesseringStatus(
        referanse: BrevbestillingReferanse,
        prosesseringStatus: ProsesseringStatus,
    ) {
        connection.execute(
            "UPDATE BREVBESTILLING SET PROSESSERING_STATUS = ?, OPPDATERT_TID = ? WHERE REFERANSE = ?"
        ) {
            setParams {
                setEnumName(1, prosesseringStatus)
                setLocalDateTime(2, LocalDateTime.now())
                setUUID(3, referanse.referanse)
            }
            setResultValidator {
                require(1 == it)
            }
        }
    }

    override fun lagreJournalpost(
        id: BrevbestillingId,
        journalpostId: JournalpostId,
        journalpostFerdigstilt: Boolean
    ) {
        connection.execute(
            "UPDATE BREVBESTILLING SET JOURNALPOST_ID = ?, JOURNALPOST_FERDIGSTILT = ?, OPPDATERT_TID = ? WHERE ID = ?"
        ) {
            setParams {
                setString(1, journalpostId.id)
                setBoolean(2, journalpostFerdigstilt)
                setLocalDateTime(3, LocalDateTime.now())
                setLong(4, id.id)
            }
            setResultValidator {
                require(1 == it)
            }
        }
    }

    override fun lagreDistribusjonBestilling(
        id: BrevbestillingId,
        distribusjonBestillingId: DistribusjonBestillingId
    ) {
        connection.execute(
            "UPDATE BREVBESTILLING SET DISTRIBUSJON_BESTILLING_ID = ?, OPPDATERT_TID = ? WHERE ID = ?"
        ) {
            setParams {
                setString(1, distribusjonBestillingId.id)
                setLocalDateTime(2, LocalDateTime.now())
                setLong(3, id.id)
            }
            setResultValidator {
                require(1 == it)
            }
        }
    }
}
