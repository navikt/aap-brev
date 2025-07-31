package no.nav.aap.brev.bestilling

import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.brev.test.randomBrevtype
import no.nav.aap.brev.test.randomBrukerIdent
import no.nav.aap.brev.test.randomDistribusjonBestillingId
import no.nav.aap.brev.test.randomJournalpostId
import no.nav.aap.brev.test.randomSaksnummer
import no.nav.aap.brev.test.randomSpråk
import no.nav.aap.brev.test.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JournalpostRepositoryImplTest {

    companion object {
        private val dataSource = InitTestDatabase.freshDatabase()
    }

    @Test
    fun `lagrer, henter og oppdaterer`() {
        dataSource.transaction { connection ->
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)
            val mottakerRepository = MottakerRepositoryImpl(connection)
            val journalpostRepository = JournalpostRepositoryImpl(connection)

            val bestilling = brevbestillingRepository.opprettBestilling(
                saksnummer = randomSaksnummer(),
                brukerIdent = randomBrukerIdent(),
                behandlingReferanse = randomBehandlingReferanse(),
                unikReferanse = randomUnikReferanse(),
                brevtype = randomBrevtype(),
                språk = randomSpråk(),
                vedlegg = emptySet()
            )

            val mottaker1 = Mottaker(
                ident = bestilling.brukerIdent,
                identType = IdentType.FNR,
                bestillingMottakerReferanse = "${bestilling.referanse.referanse}-1"
            )
            val mottaker2 = Mottaker(
                navnOgAdresse = NavnOgAdresse(
                    navn = "verge", adresse = Adresse(
                        landkode = "NO",
                        adresselinje1 = "adresselinje1",
                        adresselinje2 = "adresselinje2",
                        adresselinje3 = "adresselinje3",
                        postnummer = "postnummer",
                        poststed = "poststed",
                    )
                ),
                bestillingMottakerReferanse = "${bestilling.referanse.referanse}-2"
            )
            mottakerRepository.lagreMottakere(
                bestilling.id,
                listOf(mottaker1, mottaker2)
            )

            val mottakere = mottakerRepository.hentMottakere(bestilling.id)
            val lagretMottaker1 = mottakere.single { it.ident == mottaker1.ident }
            val lagretMottaker2 = mottakere.single { it.navnOgAdresse == mottaker2.navnOgAdresse }

            assertThat(journalpostRepository.hentAlleFor(bestilling.referanse)).isEmpty()

            val journalpostId1 = randomJournalpostId()
            val journalpostId2 = randomJournalpostId()

            journalpostRepository.lagreJournalpost(
                journalpostId = journalpostId1,
                journalpostFerdigstilt = true,
                mottakerId = lagretMottaker1.id!!
            )
            journalpostRepository.lagreJournalpost(
                journalpostId = journalpostId2,
                journalpostFerdigstilt = false,
                mottakerId = lagretMottaker2.id!!
            )
            val journalposter = journalpostRepository.hentAlleFor(bestilling.referanse)
            assertThat(journalposter).hasSize(2)
            assertThat(journalposter).anySatisfy { journalpost ->
                OpprettetJournalpost(
                    journalpostId = journalpostId1,
                    mottaker = lagretMottaker1,
                    brevbestillingId = bestilling.id,
                    ferdigstilt = true,
                    distribusjonBestillingId = null,
                )
            }
            assertThat(journalposter).anySatisfy { journalpost ->
                OpprettetJournalpost(
                    journalpostId = journalpostId2,
                    mottaker = lagretMottaker2,
                    brevbestillingId = bestilling.id,
                    ferdigstilt = false,
                    distribusjonBestillingId = null,
                )
            }

            journalpostRepository.lagreJournalpostFerdigstilt(journalpostId2, true)
            assertThat(journalpostRepository.hentHvisEksisterer(lagretMottaker2.id)?.ferdigstilt).isTrue

            val distribusjonBestillingId1 = randomDistribusjonBestillingId()

            journalpostRepository.lagreDistribusjonBestilling(journalpostId1, distribusjonBestillingId1)
            assertThat(journalpostRepository.hentHvisEksisterer(lagretMottaker1.id)?.distribusjonBestillingId).isEqualTo(
                distribusjonBestillingId1
            )
        }
    }
}