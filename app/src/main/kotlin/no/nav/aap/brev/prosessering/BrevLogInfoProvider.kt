package no.nav.aap.brev.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.mdc.JobbLogInfoProvider
import no.nav.aap.motor.mdc.LogInformasjon

object BrevLogInfoProvider : JobbLogInfoProvider {

    override fun hentInformasjon(connection: DBConnection, jobbInput: JobbInput): LogInformasjon? {

        val brevbestillingReferanse = jobbInput.bestillingsreferanseOrNull() ?: return null

        return connection.queryFirst("SELECT * FROM brevbestilling WHERE referanse = ?") {
            setParams {
                setUUID(1, brevbestillingReferanse.referanse)
            }
            setRowMapper { row ->
                LogInformasjon(
                    buildMap {
                        row.getStringOrNull("referanse")?.let { put("referanse", it) }
                        row.getStringOrNull("saksnummer")?.let { put("saksnummer", it) }
                        row.getStringOrNull("journalpost_id")?.let { put("journalpostId", it) }
                    }
                )
            }
        }
    }
}
