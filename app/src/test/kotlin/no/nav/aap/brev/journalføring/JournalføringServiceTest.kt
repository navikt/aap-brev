package no.nav.aap.brev.journalføring

import no.nav.aap.brev.IntegrationTest
import no.nav.aap.brev.bestilling.Adresse
import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.bestilling.Brevdata
import no.nav.aap.brev.bestilling.Brevdata.FaktagrunnlagMedVerdi
import no.nav.aap.brev.bestilling.Brevdata.FritekstMedKey
import no.nav.aap.brev.bestilling.IdentType
import no.nav.aap.brev.bestilling.JournalpostRepositoryImpl
import no.nav.aap.brev.bestilling.Mottaker
import no.nav.aap.brev.bestilling.MottakerRepositoryImpl
import no.nav.aap.brev.bestilling.NavnOgAdresse
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.test.fakes.journalpostForBestilling
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.brev.test.randomJournalpostId
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.booleanArrayOf

class JournalføringServiceTest : IntegrationTest() {

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `journalfører brevet og lagrer journalpostId`(brukV3: Boolean) {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val journalføringService = JournalføringService.konstruer(connection)

            val behandlingReferanse = randomBehandlingReferanse()
            val journalpostRepository = JournalpostRepositoryImpl(connection)

            val bestilling = opprettBrevbestilling(
                brukV3 = brukV3,
                behandlingReferanse = behandlingReferanse,
                brevtype = Brevtype.INNVILGELSE,
                ferdigstillAutomatisk = false,
            ).brevbestilling

            val forventetJournalpostId1 = randomJournalpostId()
            val forventetJournalpostId2 = randomJournalpostId()
            val bestillingMottakerReferanse1 = "${bestilling.referanse.referanse}-1"
            val bestillingMottakerReferanse2 = "${bestilling.referanse.referanse}-2"
            journalpostForBestilling(bestillingMottakerReferanse1, forventetJournalpostId1)
            journalpostForBestilling(bestillingMottakerReferanse2, forventetJournalpostId2)
            val mottaker1 = Mottaker(
                ident = bestilling.brukerIdent,
                identType = IdentType.FNR,
                bestillingMottakerReferanse = bestillingMottakerReferanse1
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
                bestillingMottakerReferanse = bestillingMottakerReferanse2
            )
            if (bestilling.brevdata != null) {
                brevbestillingService.oppdaterBrevdata(
                    bestilling.referanse,
                    bestilling.brevdata.copy(
                        faktagrunnlag = bestilling.brevdata.faktagrunnlag
                            .plus(FaktagrunnlagMedVerdi("VAR_1", ""))
                            .plus(FaktagrunnlagMedVerdi("VAR_2", "")),
                        fritekster = bestilling.brevdata.fritekster.plus(
                            FritekstMedKey(
                                "8a3109fb503c",
                                Brevdata.Fritekst(DefaultJsonMapper.fromJson("""{"fritekst": "abc"}"""))
                            )
                        )
                    )
                )
            }
            brevbestillingService.ferdigstill(bestilling.referanse, emptyList(), listOf(mottaker1, mottaker2))

            journalføringService.journalførBrevbestilling(bestilling.referanse)
            val journalposter = journalpostRepository.hentAlleFor(bestilling.referanse)

            assertThat(journalposter).hasSize(2)
            assertThat(journalposter).anySatisfy { opprettetJournalpost ->
                assertThat(opprettetJournalpost.journalpostId).isEqualTo(forventetJournalpostId1)
            }
            assertThat(journalposter).anySatisfy { opprettetJournalpost ->
                assertThat(opprettetJournalpost.journalpostId).isEqualTo(forventetJournalpostId2)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `validering feiler dersom bestillingen mangler brev`(brukV3: Boolean) {
        dataSource.transaction { connection ->
            val journalføringService = JournalføringService.konstruer(connection)
            val mottakerRepository = MottakerRepositoryImpl(connection)

            val bestilling = opprettBrevbestilling(
                brukV3 = brukV3,
                brevtype = Brevtype.INNVILGELSE,
                ferdigstillAutomatisk = false,
            ).brevbestilling
            val referanse = bestilling.referanse
            val bestillingMottakerReferanse = "${bestilling.referanse.referanse}-1"
            mottakerRepository.lagreMottakere(
                bestilling.id,
                mottakereLikBrukerIdent(bestilling, bestillingMottakerReferanse)
            )

            connection.execute(
                "UPDATE BREVBESTILLING SET BREV = ?::jsonb, BREVMAL = ?::jsonb WHERE REFERANSE = ?"
            ) {
                setParams {
                    setString(1, null)
                    setString(2, null)
                    setUUID(3, referanse.referanse)
                }
            }

            val exception = assertThrows<IllegalStateException> {
                journalføringService.journalførBrevbestilling(referanse)
            }
            assertEquals(exception.message, "Kan ikke generere pdf av brevbestilling uten brev.")
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `håndterer respons med http status 409 pga allerede journalført`(brukV3: Boolean) {
        dataSource.transaction { connection ->
            val journalføringService = JournalføringService.konstruer(connection)
            val behandlingReferanse = randomBehandlingReferanse()
            val mottakerRepository = MottakerRepositoryImpl(connection)
            val journalpostRepository = JournalpostRepositoryImpl(connection)

            val bestilling = opprettBrevbestilling(
                brukV3 = brukV3,
                behandlingReferanse = behandlingReferanse,
                brevtype = Brevtype.INNVILGELSE,
                ferdigstillAutomatisk = false,
            ).brevbestilling
            val referanse = bestilling.referanse
            val bestillingMottakerReferanse = "${bestilling.referanse.referanse}-1"
            mottakerRepository.lagreMottakere(
                bestilling.id,
                mottakereLikBrukerIdent(bestilling, bestillingMottakerReferanse)
            )

            val forventetJournalpostId = randomJournalpostId()
            journalpostForBestilling(
                bestillingMottakerReferanse,
                forventetJournalpostId,
                finnesAllerede = true
            )

            journalføringService.journalførBrevbestilling(referanse)

            val journalpost = journalpostRepository.hentAlleFor(referanse).single()
            assertEquals(forventetJournalpostId, journalpost.journalpostId)
        }
    }

    private fun mottakereLikBrukerIdent(
        brevbestilling: Brevbestilling,
        bestillingMottakerReferanse: String
    ): List<Mottaker> {
        requireNotNull(brevbestilling.brukerIdent) { "Denne hjelpemetoden støtter ikke null" }
        return listOf(
            Mottaker(
                ident = brevbestilling.brukerIdent,
                identType = IdentType.FNR,
                bestillingMottakerReferanse = bestillingMottakerReferanse,
            )
        )
    }
}
