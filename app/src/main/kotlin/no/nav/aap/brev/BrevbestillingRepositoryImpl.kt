package no.nav.aap.brev

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.aap.brev.domene.BehandlingReferanse
import no.nav.aap.brev.domene.Brev
import no.nav.aap.brev.domene.BrevbestillingReferanse
import no.nav.aap.brev.domene.Brevtype
import no.nav.aap.brev.domene.Språk
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.util.UUID

class BrevbestillingRepositoryImpl(private val connection: DBConnection) : BrevbestillingRepository {

    override fun opprettBestilling(
        behandlingReferanse: BehandlingReferanse,
        brevtype: Brevtype,
        sprak: Språk,
        brev: Brev
    ): BrevbestillingReferanse {
        val referanse: UUID = UUID.randomUUID()
        val query = """
            INSERT INTO BREVBESTILLING (REFERANSE, DATA, BEHANDLING_REFERANSE, SPRAK, BREVTYPE)
                VALUES (?, ?::jsonb, ?, ?, ?)
        """.trimIndent()
        connection.execute(query) {
            setParams {
                setUUID(1, referanse)
                setString(2, jacksonObjectMapper().writeValueAsString(brev))
                setUUID(3, behandlingReferanse.referanse)
                setEnumName(4, sprak)
                setEnumName(5, brevtype)
            }
        }
        return BrevbestillingReferanse(referanse)
    }
}
