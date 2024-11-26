package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.BehandlingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.bestilling.Saksnummer
import no.nav.aap.brev.innhold.BrevinnholdService
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.test.fakes.journalpostForBestilling
import no.nav.aap.brev.test.fakes.randomJournalpostId
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.random.Random
import kotlin.random.nextInt

class JournalføringServiceTest {

    companion object {
        private val fakes = Fakes()
        private val dataSource = InitTestDatabase.dataSource

        @JvmStatic
        @AfterAll
        fun afterAll() {
            fakes.close()
        }
    }

    @Test
    fun `journalfører brevet og lagrer journalpostId`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val brevinnholdService = BrevinnholdService.konstruer(connection)
            val journalføringService = JournalføringService.konstruer(connection)

            val referanse = brevbestillingService.opprettBestilling(
                Saksnummer(Random.nextInt(1000..9999).toString()),
                BehandlingReferanse(UUID.randomUUID()),
                Brevtype.INNVILGELSE,
                Språk.NB,
            )

            val forventetJournalpostId = randomJournalpostId()
            journalpostForBestilling(referanse, forventetJournalpostId)

            brevinnholdService.hentOgLagre(referanse)

            journalføringService.genererBrevOgJournalfør(referanse)

            val bestilling = brevbestillingService.hent(referanse)
            assertEquals(forventetJournalpostId, bestilling.journalpostId)
        }
    }
}
