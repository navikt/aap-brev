package no.nav.aap.brev.bestilling

import no.nav.aap.brev.distribusjon.DistribusjonBestillingId
import no.nav.aap.brev.feil.ValideringsfeilException
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.prosessering.ProsesseringStatus
import no.nav.aap.brev.journalføring.DokumentInfoId
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.SignaturGrunnlag
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.time.LocalDateTime
import java.util.UUID

class BrevbestillingRepositoryImpl(private val connection: DBConnection) : BrevbestillingRepository {

    override fun opprettBestilling(
        saksnummer: Saksnummer,
        brukerIdent: String?,
        behandlingReferanse: BehandlingReferanse,
        unikReferanse: UnikReferanse,
        brevtype: Brevtype,
        språk: Språk,
        vedlegg: Set<Vedlegg>,
    ): Brevbestilling {
        val referanse: UUID = UUID.randomUUID()
        val query = """
            INSERT INTO BREVBESTILLING (SAKSNUMMER, BRUKER_IDENT, REFERANSE, BEHANDLING_REFERANSE, SPRAK, BREVTYPE, UNIK_REFERANSE)
                VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        val id =
            connection.executeReturnKey(query) {
                setParams {
                    setString(1, saksnummer.nummer)
                    setString(2, brukerIdent)
                    setUUID(3, referanse)
                    setUUID(4, behandlingReferanse.referanse)
                    setEnumName(5, språk)
                    setEnumName(6, brevtype)
                    setString(7, unikReferanse.referanse)
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

    override fun hentForOppdatering(referanse: BrevbestillingReferanse): Brevbestilling {
        return connection.queryFirst(
            "SELECT * FROM BREVBESTILLING WHERE REFERANSE = ? FOR UPDATE"
        ) {
            setParams {
                setUUID(1, referanse.referanse)
            }
            setRowMapper { row -> mapBestilling(row) }
        }
    }

    private fun mapBestilling(row: Row): Brevbestilling {
        val id = BrevbestillingId(row.getLong("ID"))

        return Brevbestilling(
            id = id,
            saksnummer = Saksnummer(row.getString("SAKSNUMMER")),
            referanse = BrevbestillingReferanse(row.getUUID("REFERANSE")),
            brev = row.getStringOrNull("BREV")?.let { DefaultJsonMapper.fromJson<Brev>(it) },
            brevmal = row.getStringOrNull("BREVMAL")?.let { DefaultJsonMapper.fromJson<BrevmalJson>(it) },
            brevdata = row.getStringOrNull("BREVDATA")?.let { DefaultJsonMapper.fromJson<Brevdata>(it) },
            brukerIdent = row.getStringOrNull("BRUKER_IDENT"),
            signaturer = hentSignaturer(id),
            opprettet = row.getLocalDateTime("OPPRETTET_TID"),
            oppdatert = row.getLocalDateTime("OPPDATERT_TID"),
            behandlingReferanse = BehandlingReferanse(row.getUUID("BEHANDLING_REFERANSE")),
            unikReferanse = UnikReferanse(row.getString("UNIK_REFERANSE")),
            brevtype = row.getEnum("BREVTYPE"),
            språk = row.getEnum("SPRAK"),
            status = row.getEnumOrNull("STATUS"),
            prosesseringStatus = row.getEnumOrNull("PROSESSERING_STATUS"),
            vedlegg = hentVedlegg(id).toSet(),
        )
    }

    private fun hentSignaturer(brevbestillingId: BrevbestillingId): List<SorterbarSignatur> {
        val signaturQuery = """
            SELECT * FROM SIGNATUR WHERE BREVBESTILLING_ID = ?
        """.trimIndent()

        return connection.queryList(signaturQuery) {
            setParams {
                setLong(1, brevbestillingId.id)
            }
            setRowMapper {
                SorterbarSignatur(
                    navIdent = it.getString("NAV_IDENT"),
                    sorteringsnøkkel = it.getInt("SORTERINGS_NOKKEL"),
                    rolle = it.getEnumOrNull("ROLLE"),
                    enhet = it.getStringOrNull("ENHET"),
                )
            }
        }
    }

    private fun hentVedlegg(brevbestillingId: BrevbestillingId): List<Vedlegg> {
        val vedleggQuery = """
            SELECT * FROM VEDLEGG WHERE BREVBESTILLING_ID = ?
        """.trimIndent()

        return connection.queryList(vedleggQuery) {
            setParams {
                setLong(1, brevbestillingId.id)
            }
            setRowMapper {
                Vedlegg(
                    journalpostId = JournalpostId(it.getString("JOURNALPOST_ID")),
                    dokumentInfoId = DokumentInfoId(it.getString("DOKUMENT_INFO_ID")),
                )
            }
        }
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

    override fun oppdaterBrevmal(
        id: BrevbestillingId,
        brevmal: BrevmalJson
    ) {
        connection.execute(
            "UPDATE BREVBESTILLING SET BREVMAL = ?::jsonb, OPPDATERT_TID = ? WHERE ID = ?"
        ) {
            setParams {
                setString(1, DefaultJsonMapper.toJson(brevmal))
                setLocalDateTime(2, LocalDateTime.now())
                setLong(3, id.id)
            }
            setResultValidator {
                if (it != 1) {
                    throw ValideringsfeilException("Forsøkte å oppdatere brevbestilling som ikke finnes.")
                }
            }
        }
    }

    override fun oppdaterBrevdata(id: BrevbestillingId, brevdata: Brevdata) {
        connection.execute(
            "UPDATE BREVBESTILLING SET BREVDATA = ?::jsonb, OPPDATERT_TID = ? WHERE ID = ?"
        ) {
            setParams {
                setString(1, DefaultJsonMapper.toJson(brevdata))
                setLocalDateTime(2, LocalDateTime.now())
                setLong(3, id.id)
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

    override fun nullstillProsesseringStatus(
        referanse: BrevbestillingReferanse
    ) {
        connection.execute(
            "UPDATE BREVBESTILLING SET PROSESSERING_STATUS = NULL, OPPDATERT_TID = ? WHERE REFERANSE = ?"
        ) {
            setParams {
                setLocalDateTime(1, LocalDateTime.now())
                setUUID(2, referanse.referanse)
            }
            setResultValidator {
                require(1 == it)
            }
        }
    }

    override fun lagreSignaturer(brevbestillingId: BrevbestillingId, signaturer: List<SignaturGrunnlag>) {
        val query = """
            INSERT INTO SIGNATUR (BREVBESTILLING_ID, NAV_IDENT, ROLLE, ENHET, SORTERINGS_NOKKEL) VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        connection.executeBatch(query, signaturer.tilSorterbareSignaturer()) {
            setParams {
                setLong(1, brevbestillingId.id)
                setString(2, it.navIdent)
                setEnumName(3, it.rolle)
                setString(4, it.enhet)
                setInt(5, it.sorteringsnøkkel)
            }
        }
    }

    override fun oppdaterStatus(id: BrevbestillingId, status: Status) {
        connection.execute(
            "UPDATE BREVBESTILLING SET STATUS = ?, OPPDATERT_TID = ? WHERE ID = ?"
        ) {
            setParams {
                setEnumName(1, status)
                setLocalDateTime(2, LocalDateTime.now())
                setLong(3, id.id)
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

    override fun lagreJournalpostFerdigstilt(id: BrevbestillingId, journalpostFerdigstilt: Boolean) {
        connection.execute(
            "UPDATE BREVBESTILLING SET JOURNALPOST_FERDIGSTILT = ?, OPPDATERT_TID = ? WHERE ID = ?"
        ) {
            setParams {
                setBoolean(1, journalpostFerdigstilt)
                setLocalDateTime(2, LocalDateTime.now())
                setLong(3, id.id)
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
