package no.nav.aap.brev.bestilling

import no.nav.aap.brev.arkivoppslag.Journalpost
import no.nav.aap.brev.exception.ValideringsfeilException
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.test.fakes.gittJournalpostIArkivet
import no.nav.aap.brev.test.fakes.randomBehandlingReferanse
import no.nav.aap.brev.test.fakes.randomDokumentInfoId
import no.nav.aap.brev.test.fakes.randomJournalpostId
import no.nav.aap.brev.test.fakes.randomSaksnummer
import no.nav.aap.brev.test.fakes.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BestillingValideringTest {

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
    fun `bestilling går igjennom dersom ingen valideringsfeil`() {
        val saksnummer = randomSaksnummer()
        val dokumentInfoId = randomDokumentInfoId()
        val journalpost = gittJournalpostIArkivet(
            journalpostId = randomJournalpostId(),
            saksnummer = saksnummer,
            dokumentInfoId = dokumentInfoId
        )
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)

            val referanse = brevbestillingService.opprettBestilling(
                saksnummer = saksnummer,
                behandlingReferanse = randomBehandlingReferanse(),
                unikReferanse = randomUnikReferanse(),
                brevtype = Brevtype.INNVILGELSE,
                språk = Språk.NB,
                vedlegg = setOf(Vedlegg(journalpost.journalpostId, dokumentInfoId)),
            ).referanse
            assertNotNull(brevbestillingRepository.hent(referanse))
        }
    }

    @Test
    fun `validering feiler dersom sak på vedlegg ikke har fagsakId lik bestillingens saksnummer`() {
        val dokumentInfoId = randomDokumentInfoId()
        val journalpost = gittJournalpostIArkivet(
            journalpostId = randomJournalpostId(),
            saksnummer = randomSaksnummer(),
            dokumentInfoId = dokumentInfoId
        )

        validerFeilVedBestilling(
            saksnummer = randomSaksnummer(),
            vedlegg = journalpost.somVedlegg(),
            feilmelding = "Ulik sak."
        )
    }

    @Test
    fun `validering feiler dersom sak på vedlegg ikke har fagsystem KELVIN`() {
        val saksnummer = randomSaksnummer()
        val dokumentInfoId = randomDokumentInfoId()
        val journalpost = gittJournalpostIArkivet(
            journalpostId = randomJournalpostId(),
            saksnummer = saksnummer,
            dokumentInfoId = dokumentInfoId,
            fagsaksystem = "ARENA"
        )

        validerFeilVedBestilling(
            saksnummer = saksnummer,
            vedlegg = journalpost.somVedlegg(),
            feilmelding = "Ulik sak."
        )
    }

    private fun Journalpost.somVedlegg(): Set<Vedlegg> {
        return dokumenter.map { Vedlegg(journalpostId, it.dokumentInfoId) }.toSet()
    }

    @Test
    fun `validering feiler dersom sak på vedlegg ikke er sakstype FAGSAK`() {
        val saksnummer = randomSaksnummer()
        val dokumentInfoId = randomDokumentInfoId()
        val journalpost = gittJournalpostIArkivet(
            journalpostId = randomJournalpostId(),
            saksnummer = saksnummer,
            dokumentInfoId = dokumentInfoId,
            sakstype = "GENERELL_SAK"
        )

        validerFeilVedBestilling(
            saksnummer = saksnummer,
            vedlegg = journalpost.somVedlegg(),
            feilmelding = "Ulik sak."
        )
    }

    @Test
    fun `validering feiler dersom sak på vedlegg ikke har tema AAP`() {
        val saksnummer = randomSaksnummer()
        val dokumentInfoId = randomDokumentInfoId()
        val journalpost = gittJournalpostIArkivet(
            journalpostId = randomJournalpostId(),
            saksnummer = saksnummer,
            dokumentInfoId = dokumentInfoId,
            tema = "OPP"
        )

        validerFeilVedBestilling(
            saksnummer = saksnummer,
            vedlegg = journalpost.somVedlegg(),
            feilmelding = "Ulik sak."
        )
    }

    @Test
    fun `validering feiler dersom bruker ikke har tilgang til vedlegg (journalposten)`() {
        val saksnummer = randomSaksnummer()
        val dokumentInfoId = randomDokumentInfoId()
        val journalpost = gittJournalpostIArkivet(
            journalpostId = randomJournalpostId(),
            saksnummer = saksnummer,
            dokumentInfoId = dokumentInfoId,
            brukerHarTilgangTilJournalpost = false
        )

        validerFeilVedBestilling(
            saksnummer = saksnummer,
            vedlegg = journalpost.somVedlegg(),
            feilmelding = "Bruker har ikke tilgang til journalpost."
        )
    }

    @Test
    fun `validering feiler dersom vedlegg ikke journalstatus FERDIGSTILT eller EKSPEDERT`() {
        val saksnummer = randomSaksnummer()
        val dokumentInfoId = randomDokumentInfoId()
        val journalpost = gittJournalpostIArkivet(
            journalpostId = randomJournalpostId(),
            saksnummer = saksnummer,
            dokumentInfoId = dokumentInfoId,
            journalstatus = "JOURNALFOERT"
        )

        validerFeilVedBestilling(
            saksnummer = saksnummer,
            vedlegg = journalpost.somVedlegg(),
            feilmelding = "Feil status JOURNALFOERT."
        )
    }

    @Test
    fun `validering feiler dersom vedlegg ikke har dokumentet i arkivet`() {
        val saksnummer = randomSaksnummer()
        val journalpost = gittJournalpostIArkivet(
            journalpostId = randomJournalpostId(),
            saksnummer = saksnummer,
            dokumentInfoId = randomDokumentInfoId(),
        )

        validerFeilVedBestilling(
            saksnummer = saksnummer,
            vedlegg = setOf(Vedlegg(journalpost.journalpostId, randomDokumentInfoId())),
            feilmelding = "Fant ikke dokument i journalpost."
        )
    }

    @Test
    fun `validering feiler dersom bruker ikke har tilgang til vedlegg (dokument)`() {
        val saksnummer = randomSaksnummer()
        val dokumentInfoId = randomDokumentInfoId()
        val journalpost = gittJournalpostIArkivet(
            journalpostId = randomJournalpostId(),
            saksnummer = saksnummer,
            dokumentInfoId = dokumentInfoId,
            brukerHarTilgangTilDokument = false,
        )

        validerFeilVedBestilling(
            saksnummer = saksnummer,
            vedlegg = journalpost.somVedlegg(),
            feilmelding = "Bruker har ikke tilgang til dokumentet."
        )
    }

    private fun validerFeilVedBestilling(
        saksnummer: Saksnummer,
        vedlegg: Set<Vedlegg>,
        feilmelding: String,
    ) {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val exception = assertThrows<ValideringsfeilException> {
                brevbestillingService.opprettBestilling(
                    saksnummer = saksnummer,
                    behandlingReferanse = randomBehandlingReferanse(),
                    unikReferanse = randomUnikReferanse(),
                    brevtype = Brevtype.INNVILGELSE,
                    språk = Språk.NB,
                    vedlegg = vedlegg,
                )
            }
            assertThat(exception.message).endsWith(feilmelding)
        }
    }
}
