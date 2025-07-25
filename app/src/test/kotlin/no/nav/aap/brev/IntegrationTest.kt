package no.nav.aap.brev

import no.nav.aap.brev.bestilling.BehandlingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.bestilling.OpprettBrevbestillingResultat
import no.nav.aap.brev.bestilling.Saksnummer
import no.nav.aap.brev.bestilling.UnikReferanse
import no.nav.aap.brev.bestilling.Vedlegg
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.brev.test.randomBrevtype
import no.nav.aap.brev.test.randomBrukerIdent
import no.nav.aap.brev.test.randomSaksnummer
import no.nav.aap.brev.test.randomSpråk
import no.nav.aap.brev.test.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll

abstract class IntegrationTest {

    companion object {

        @JvmStatic
        protected val dataSource = InitTestDatabase.freshDatabase()

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            Fakes.start()
        }
    }


    fun opprettBrevbestilling(
        saksnummer: Saksnummer = randomSaksnummer(),
        brukerIdent: String = randomBrukerIdent(),
        behandlingReferanse: BehandlingReferanse = randomBehandlingReferanse(),
        unikReferanse: UnikReferanse = randomUnikReferanse(),
        brevtype: Brevtype = randomBrevtype(),
        språk: Språk = randomSpråk(),
        faktagrunnlag: Set<Faktagrunnlag> = emptySet(),
        vedlegg: Set<Vedlegg> = emptySet(),
        ferdigstillAutomatisk: Boolean = false,
    ): OpprettBrevbestillingResultat {
        return dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            brevbestillingService.opprettBestillingV2(
                saksnummer = saksnummer,
                brukerIdent = brukerIdent,
                behandlingReferanse = behandlingReferanse,
                unikReferanse = unikReferanse,
                brevtype = brevtype,
                språk = språk,
                faktagrunnlag = faktagrunnlag,
                vedlegg = vedlegg,
                ferdigstillAutomatisk = ferdigstillAutomatisk,
            )
        }
    }

    protected fun assertAntallJobber(referanse: BrevbestillingReferanse, forventetAntall: Int) {
        return dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val bestilling = brevbestillingService.hent(referanse)

            val query = """
                SELECT count(*) as antall
                FROM JOBB
                WHERE sak_id = ?
            """.trimIndent()

            val antall = connection.queryFirst(query) {
                setParams {
                    setLong(1, bestilling.id.id)
                }
                setRowMapper { row -> row.getInt("antall") }
            }

            assertThat(antall).isEqualTo(forventetAntall)
        }
    }

}