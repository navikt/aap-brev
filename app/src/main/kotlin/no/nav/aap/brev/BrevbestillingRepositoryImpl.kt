package no.nav.aap.brev

import no.nav.aap.brev.domene.BehandlingReferanse
import no.nav.aap.brev.domene.Brev
import no.nav.aap.brev.domene.Brevbestilling
import no.nav.aap.brev.domene.BrevbestillingReferanse
import no.nav.aap.brev.domene.Brevtype
import no.nav.aap.brev.domene.Språk
import no.nav.aap.brev.exception.BestillingForBehandlingEksistererException
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import org.postgresql.util.PSQLException
import java.util.UUID

class BrevbestillingRepositoryImpl(private val connection: DBConnection) : BrevbestillingRepository {

    private val UNIQUE_VIOATION = "23505"

    override fun opprettBestilling(
        behandlingReferanse: BehandlingReferanse,
        brevtype: Brevtype,
        språk: Språk,
    ): BrevbestillingReferanse {
        val referanse: UUID = UUID.randomUUID()
        val query = """
            INSERT INTO BREVBESTILLING (REFERANSE, BEHANDLING_REFERANSE, SPRAK, BREVTYPE)
                VALUES (?, ?, ?, ?)
        """.trimIndent()

        try {
            connection.execute(query) {
                setParams {
                    setUUID(1, referanse)
                    setUUID(2, behandlingReferanse.referanse)
                    setEnumName(3, språk)
                    setEnumName(4, brevtype)
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
                    referanse = BrevbestillingReferanse(row.getUUID("REFERANSE")),
                    brev = row.getStringOrNull("BREV")?.let { DefaultJsonMapper.fromJson<Brev>(it) },
                    opprettet = row.getLocalDateTime("OPPRETTET_TID"),
                    oppdatert = row.getLocalDateTime("OPPDATERT_TID"),
                    behandlingReferanse = BehandlingReferanse(row.getUUID("BEHANDLING_REFERANSE")),
                    brevtype = row.getEnum("BREVTYPE"),
                    språk = row.getEnum("SPRAK"),
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
        }
    }

}
