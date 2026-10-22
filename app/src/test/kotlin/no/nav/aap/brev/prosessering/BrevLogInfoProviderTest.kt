package no.nav.aap.brev.prosessering

import no.nav.aap.brev.IntegrationTest
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.JobbInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BrevLogInfoProviderTest : IntegrationTest() {

    @Test
    fun `returnerer null når jobbinput mangler bestillingsreferanse`() {
        val logInfo = dataSource.transaction { connection ->
            BrevLogInfoProvider.hentInformasjon(connection, JobbInput(ProsesserBrevbestillingJobbUtfører))
        }

        assertThat(logInfo).isNull()
    }

    @Test
    fun `kaster exception hvis bestillingsreferanse ikke finnes i databasen`() {
        val jobbInput = JobbInput(ProsesserBrevbestillingJobbUtfører)
            .medBestillingsreferanse(BrevbestillingReferanse(java.util.UUID.randomUUID()))

        assertThrows<Exception> {
            dataSource.transaction { connection ->
                BrevLogInfoProvider.hentInformasjon(connection, jobbInput)
            }
        }
    }

    @Test
    fun `returnerer logginformasjon med referanse og saksnummer`() {
        val bestilling = opprettBrevbestilling(brevtype = Brevtype.VARSEL_OM_BESTILLING, ferdigstillAutomatisk = false)
        val jobbInput = JobbInput(ProsesserBrevbestillingJobbUtfører)
            .medBestillingsreferanse(bestilling.brevbestilling.referanse)

        val logInfo = dataSource.transaction { connection ->
            BrevLogInfoProvider.hentInformasjon(connection, jobbInput)
        }

        assertThat(logInfo?.felterMedVerdi).isEqualTo(
            mapOf(
                "referanse" to bestilling.brevbestilling.referanse.referanse.toString(),
                "saksnummer" to bestilling.brevbestilling.saksnummer.nummer,
            )
        )
    }

}
