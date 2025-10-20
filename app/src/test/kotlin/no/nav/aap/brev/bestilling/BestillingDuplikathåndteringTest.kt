package no.nav.aap.brev.bestilling

import no.nav.aap.brev.IntegrationTest
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.test.fakes.gittJournalpostIArkivet
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.brev.test.randomBrukerIdent
import no.nav.aap.brev.test.randomDokumentInfoId
import no.nav.aap.brev.test.randomJournalpostId
import no.nav.aap.brev.test.randomSaksnummer
import no.nav.aap.brev.test.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class BestillingDuplikathåndteringTest : IntegrationTest() {

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `håndterer duplikat bestilling`(brukV3: Boolean) {
        val saksnummer = randomSaksnummer()
        val brukerIdent = randomBrukerIdent()
        val behandlingReferanse = randomBehandlingReferanse()
        val unikReferanse = randomUnikReferanse()
        val dokumentInfoId = randomDokumentInfoId()
        val journalpost = gittJournalpostIArkivet(
            journalpostId = randomJournalpostId(),
            saksnummer = saksnummer,
            dokumentInfoId = dokumentInfoId
        )
        val resultatFørste = opprettBestilling(
            brukV3 = brukV3,
            saksnummer = saksnummer,
            brukerIdent = brukerIdent,
            behandlingReferanse = behandlingReferanse,
            unikReferanse = unikReferanse,
            vedlegg = setOf(Vedlegg(journalpost.journalpostId, dokumentInfoId)),
        )

        assertFalse(resultatFørste.alleredeOpprettet)

        val resultatAndre = opprettBestilling(
            brukV3 = brukV3,
            saksnummer = saksnummer,
            brukerIdent = brukerIdent,
            behandlingReferanse = behandlingReferanse,
            unikReferanse = unikReferanse,
            vedlegg = setOf(Vedlegg(journalpost.journalpostId, dokumentInfoId)),
        )

        assertTrue(resultatAndre.alleredeOpprettet)
    }

    private fun opprettBestilling(
        brukV3: Boolean,
        saksnummer: Saksnummer,
        brukerIdent: String,
        behandlingReferanse: BehandlingReferanse,
        unikReferanse: UnikReferanse,
        vedlegg: Set<Vedlegg>,
        brevtype: Brevtype = Brevtype.INNVILGELSE,
        språk: Språk = Språk.NB,
        faktagrunnlag: Set<Faktagrunnlag> = emptySet(),
        ferdigstillAutomatisk: Boolean = false,
    ): OpprettBrevbestillingResultat {
        return dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            if (brukV3) {
                brevbestillingService.opprettBestillingV3(
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
            } else {
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
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `feiler dersom bestilling med lik unik referanse ikke er identisk med opprinnelig bestilling`(brukV3: Boolean) {
        val saksnummer = randomSaksnummer()
        val brukerIdent = randomBrukerIdent()
        val behandlingReferanse = randomBehandlingReferanse()
        val unikReferanse = randomUnikReferanse()
        val dokumentInfoId = randomDokumentInfoId()
        val journalpost = gittJournalpostIArkivet(
            journalpostId = randomJournalpostId(),
            saksnummer = saksnummer,
            dokumentInfoId = dokumentInfoId
        )
        val referanse = opprettBestilling(
            brukV3 = brukV3,
            saksnummer = saksnummer,
            brukerIdent = brukerIdent,
            behandlingReferanse = behandlingReferanse,
            unikReferanse = unikReferanse,
            vedlegg = setOf(Vedlegg(journalpost.journalpostId, dokumentInfoId)),
        ).brevbestilling.referanse


        assertThrowsVedLikUnikReferanseMenUlikBestilling(
            brukV3 = brukV3,
            referanse = referanse,
            saksnummer = randomSaksnummer(),
        )

        assertThrowsVedLikUnikReferanseMenUlikBestilling(
            brukV3 = brukV3,
            referanse = referanse,
            brukerIdent = randomBrukerIdent(),
        )

        assertThrowsVedLikUnikReferanseMenUlikBestilling(
            brukV3 = brukV3,
            referanse = referanse,
            behandlingReferanse = randomBehandlingReferanse()
        )

        assertThrowsVedLikUnikReferanseMenUlikBestilling(
            brukV3 = brukV3,
            referanse = referanse,
            brevtype = Brevtype.AVSLAG,
        )

        assertThrowsVedLikUnikReferanseMenUlikBestilling(
            brukV3 = brukV3,
            referanse = referanse,
            språk = Språk.NN
        )

        assertThrowsVedLikUnikReferanseMenUlikBestilling(
            brukV3 = brukV3,
            referanse = referanse,
            vedlegg = emptySet()
        )
    }

    fun assertThrowsVedLikUnikReferanseMenUlikBestilling(
        brukV3: Boolean = false,
        referanse: BrevbestillingReferanse,
        saksnummer: Saksnummer? = null,
        brukerIdent: String? = null,
        behandlingReferanse: BehandlingReferanse? = null,
        brevtype: Brevtype? = null,
        språk: Språk? = null,
        vedlegg: Set<Vedlegg>? = null,
    ) {

        val endretBestilling = dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val bestilling = brevbestillingService.hent(referanse)
            bestilling.copy(
                saksnummer = saksnummer ?: bestilling.saksnummer,
                brukerIdent = brukerIdent ?: bestilling.brukerIdent,
                behandlingReferanse = behandlingReferanse ?: bestilling.behandlingReferanse,
                brevtype = brevtype ?: bestilling.brevtype,
                språk = språk ?: bestilling.språk,
                vedlegg = vedlegg ?: bestilling.vedlegg,
            )
        }
        val exception = assertThrows<IllegalStateException> {
            opprettBestilling(
                brukV3 = brukV3,
                saksnummer = endretBestilling.saksnummer,
                brukerIdent = endretBestilling.brukerIdent!!,
                behandlingReferanse = endretBestilling.behandlingReferanse,
                unikReferanse = endretBestilling.unikReferanse,
                brevtype = endretBestilling.brevtype,
                språk = endretBestilling.språk,
                faktagrunnlag = emptySet(),
                vedlegg = endretBestilling.vedlegg,
                ferdigstillAutomatisk = false,
            )
        }

        assertEquals(
            exception.message,
            "Bestilling med unikReferanse=${endretBestilling.unikReferanse.referanse} finnnes allerede, men er ikke samme bestilling."
        )
    }
}
