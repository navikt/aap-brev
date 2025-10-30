package no.nav.aap.brev.bestilling

import no.nav.aap.brev.arkivoppslag.Journalpost
import no.nav.aap.brev.feil.ValideringsfeilException
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.test.fakes.gittJournalpostIArkivet
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.brev.test.randomBrukerIdent
import no.nav.aap.brev.test.randomDokumentInfoId
import no.nav.aap.brev.test.randomJournalpostId
import no.nav.aap.brev.test.randomSaksnummer
import no.nav.aap.brev.test.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.booleanArrayOf

class BestillingValideringTest {

    companion object {
        private val dataSource = InitTestDatabase.freshDatabase()

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            Fakes.start()
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `bestilling går igjennom dersom ingen valideringsfeil`(brukV3: Boolean) {
        val saksnummer = randomSaksnummer()
        val brukerIdent = randomBrukerIdent()
        val dokumentInfoId = randomDokumentInfoId()
        val journalpost = gittJournalpostIArkivet(
            journalpostId = randomJournalpostId(),
            saksnummer = saksnummer,
            dokumentInfoId = dokumentInfoId
        )
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)

            val referanse =
                if (brukV3) {
                    brevbestillingService.opprettBestillingV3(
                        saksnummer = saksnummer,
                        brukerIdent = brukerIdent,
                        behandlingReferanse = randomBehandlingReferanse(),
                        unikReferanse = randomUnikReferanse(),
                        brevtype = Brevtype.INNVILGELSE,
                        språk = Språk.NB,
                        faktagrunnlag = emptySet(),
                        vedlegg = setOf(Vedlegg(journalpost.journalpostId, dokumentInfoId)),
                        ferdigstillAutomatisk = false,
                    ).brevbestilling.referanse
                } else {
                    brevbestillingService.opprettBestillingV2(
                        saksnummer = saksnummer,
                        brukerIdent = brukerIdent,
                        behandlingReferanse = randomBehandlingReferanse(),
                        unikReferanse = randomUnikReferanse(),
                        brevtype = Brevtype.INNVILGELSE,
                        språk = Språk.NB,
                        faktagrunnlag = emptySet(),
                        vedlegg = setOf(Vedlegg(journalpost.journalpostId, dokumentInfoId)),
                        ferdigstillAutomatisk = false,
                    ).brevbestilling.referanse
                }
            assertNotNull(brevbestillingRepository.hent(referanse))
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `validering feiler dersom sak på vedlegg ikke har fagsakId lik bestillingens saksnummer`(brukV3: Boolean) {
        val dokumentInfoId = randomDokumentInfoId()
        val journalpost = gittJournalpostIArkivet(
            journalpostId = randomJournalpostId(),
            saksnummer = randomSaksnummer(),
            dokumentInfoId = dokumentInfoId
        )

        validerFeilVedBestilling(
            brukV3 = brukV3,
            saksnummer = randomSaksnummer(),
            vedlegg = journalpost.somVedlegg(),
            feilmelding = "Ulik sak."
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `validering feiler dersom sak på vedlegg ikke har fagsystem KELVIN`(brukV3: Boolean) {
        val saksnummer = randomSaksnummer()
        val dokumentInfoId = randomDokumentInfoId()
        val journalpost = gittJournalpostIArkivet(
            journalpostId = randomJournalpostId(),
            saksnummer = saksnummer,
            dokumentInfoId = dokumentInfoId,
            fagsaksystem = "ARENA"
        )

        validerFeilVedBestilling(
            brukV3 = brukV3,
            saksnummer = saksnummer,
            vedlegg = journalpost.somVedlegg(),
            feilmelding = "Ulik sak."
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `validering feiler dersom sak på vedlegg ikke er sakstype FAGSAK`(brukV3: Boolean) {
        val saksnummer = randomSaksnummer()
        val dokumentInfoId = randomDokumentInfoId()
        val journalpost = gittJournalpostIArkivet(
            journalpostId = randomJournalpostId(),
            saksnummer = saksnummer,
            dokumentInfoId = dokumentInfoId,
            sakstype = "GENERELL_SAK"
        )

        validerFeilVedBestilling(
            brukV3 = brukV3,
            saksnummer = saksnummer,
            vedlegg = journalpost.somVedlegg(),
            feilmelding = "Ulik sak."
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `validering feiler dersom sak på vedlegg ikke har tema AAP`(brukV3: Boolean) {
        val saksnummer = randomSaksnummer()
        val dokumentInfoId = randomDokumentInfoId()
        val journalpost = gittJournalpostIArkivet(
            journalpostId = randomJournalpostId(),
            saksnummer = saksnummer,
            dokumentInfoId = dokumentInfoId,
            tema = "OPP"
        )

        validerFeilVedBestilling(
            brukV3 = brukV3,
            saksnummer = saksnummer,
            vedlegg = journalpost.somVedlegg(),
            feilmelding = "Ulik sak."
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `validering feiler dersom vedlegg ikke journalstatus FERDIGSTILT, EKSPEDERT eller FEILREGISTRERT`(brukV3: Boolean) {
        val saksnummer = randomSaksnummer()
        val dokumentInfoId = randomDokumentInfoId()
        val journalpost = gittJournalpostIArkivet(
            journalpostId = randomJournalpostId(),
            saksnummer = saksnummer,
            dokumentInfoId = dokumentInfoId,
            journalstatus = "JOURNALFOERT"
        )

        validerFeilVedBestilling(
            brukV3 = brukV3,
            saksnummer = saksnummer,
            vedlegg = journalpost.somVedlegg(),
            feilmelding = "Feil status JOURNALFOERT."
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `validering feiler dersom vedlegg ikke har dokumentet i arkivet`(brukV3: Boolean) {
        val saksnummer = randomSaksnummer()
        val journalpost = gittJournalpostIArkivet(
            journalpostId = randomJournalpostId(),
            saksnummer = saksnummer,
            dokumentInfoId = randomDokumentInfoId(),
        )

        validerFeilVedBestilling(
            brukV3 = brukV3,
            saksnummer = saksnummer,
            vedlegg = setOf(Vedlegg(journalpost.journalpostId, randomDokumentInfoId())),
            feilmelding = "Fant ikke dokument i journalpost."
        )
    }

    private fun Journalpost.somVedlegg(): Set<Vedlegg> {
        return dokumenter.map { Vedlegg(journalpostId, it.dokumentInfoId) }.toSet()
    }

    private fun validerFeilVedBestilling(
        brukV3: Boolean,
        saksnummer: Saksnummer,
        vedlegg: Set<Vedlegg>,
        feilmelding: String,
    ) {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val exception = assertThrows<ValideringsfeilException> {
                if (brukV3) {
                    brevbestillingService.opprettBestillingV3(
                        saksnummer = saksnummer,
                        brukerIdent = randomBrukerIdent(),
                        behandlingReferanse = randomBehandlingReferanse(),
                        unikReferanse = randomUnikReferanse(),
                        brevtype = Brevtype.INNVILGELSE,
                        språk = Språk.NB,
                        faktagrunnlag = emptySet(),
                        vedlegg = vedlegg,
                        ferdigstillAutomatisk = false,
                    )
                } else {
                    brevbestillingService.opprettBestillingV2(
                        saksnummer = saksnummer,
                        brukerIdent = randomBrukerIdent(),
                        behandlingReferanse = randomBehandlingReferanse(),
                        unikReferanse = randomUnikReferanse(),
                        brevtype = Brevtype.INNVILGELSE,
                        språk = Språk.NB,
                        faktagrunnlag = emptySet(),
                        vedlegg = vedlegg,
                        ferdigstillAutomatisk = false,
                    )
                }
            }
            assertThat(exception.message).endsWith(feilmelding)
        }
    }
}
