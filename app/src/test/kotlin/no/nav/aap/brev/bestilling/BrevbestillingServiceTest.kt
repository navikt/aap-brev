package no.nav.aap.brev.bestilling

import no.nav.aap.brev.IntegrationTest
import no.nav.aap.brev.feil.ValideringsfeilException
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.test.fakes.FakeUnleashGateway
import no.nav.aap.brev.unleash.BrevFeature
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BrevbestillingServiceTest : IntegrationTest() {
    @Nested
    inner class MottakerTest {
        @Test
        fun `ferdigstill bruker mottakere lagret via oppdaterMottakere når feature er aktivert selv om ferdigstill-forespørselen har tom mottakerliste`() {
            val fakeUnleash = FakeUnleashGateway(BrevFeature.RedigerMottakerBrevbygger)

            val bestilling = opprettBrevbestilling(
                brukV3 = true,
                brevtype = Brevtype.INNVILGELSE,
                ferdigstillAutomatisk = false,
                unleashGateway = fakeUnleash,
            ).brevbestilling
            val referanse = bestilling.referanse

            dataSource.transaction { connection ->
                val service = BrevbestillingService.konstruer(connection, fakeUnleash)
                val mottakerRepository = MottakerRepositoryImpl(connection)

                // Bruker i saken skal bli hovedmottaker
                val mottakere = mottakerRepository.hentMottakere(bestilling.id)
                assertThat(mottakere).singleElement()
                assertThat(mottakere.single().ident).isEqualTo(bestilling.brukerIdent)

                val mottaker = Mottaker(
                    ident = bestilling.brukerIdent,
                    identType = IdentType.FNR,
                    bestillingMottakerReferanse = "${referanse.referanse}-1",
                    type = Mottaker.Type.HOVED,
                )

                val nyeMottakere = listOf(mottaker)
                // Tilsvarer kallet som gjøres fra /oppdater-mottakere-endepunktet.
                service.oppdaterMottakere(referanse, nyeMottakere)

                service.ferdigstill(referanse, null, emptyList())

                val oppdatertBestilling = BrevbestillingRepositoryImpl(connection).hent(referanse)
                assertThat(oppdatertBestilling.status).isEqualTo(Status.FERDIGSTILT)
                assertThat(mottakerRepository.hentMottakere(bestilling.id).map { it.copy(id = null) })
                    .containsExactly(mottaker)
            }
        }

        @Test
        fun `ferdigstill feiler når mottakerliste er tom både i forespørsel og lagret, og feature er aktivert`() {
            val bestilling = opprettBrevbestilling(
                brevtype = Brevtype.INNVILGELSE,
                ferdigstillAutomatisk = false,
            ).brevbestilling
            val referanse = bestilling.referanse

            dataSource.transaction { connection ->
                val service = BrevbestillingService.konstruer(
                    connection,
                    FakeUnleashGateway(BrevFeature.RedigerMottakerBrevbygger),
                )

                assertThrows<ValideringsfeilException> {
                    service.ferdigstill(referanse, null, emptyList())
                }
            }
        }

        @Test
        fun `ferdigstill krever ikke eksplisitt mottakerliste når feature er deaktivert`() {
            val bestilling = opprettBrevbestilling(
                brevtype = Brevtype.INNVILGELSE,
                ferdigstillAutomatisk = false,
            ).brevbestilling
            val referanse = bestilling.referanse

            dataSource.transaction { connection ->
                val service = BrevbestillingService.konstruer(connection)

                service.ferdigstill(referanse, null, emptyList())

                val oppdatertBestilling = BrevbestillingRepositoryImpl(connection).hent(referanse)
                assertThat(oppdatertBestilling.status).isEqualTo(Status.FERDIGSTILT)
            }
        }

        @Test
        fun `samme service-instans kan håndtere ulik featureoppførsel for ulike bestillinger`() {
            val fakeUnleash = FakeUnleashGateway()
            val bestillingUtenToggle = opprettBrevbestilling(
                brevtype = Brevtype.INNVILGELSE,
                ferdigstillAutomatisk = false,
            ).brevbestilling
            val bestillingMedToggle = opprettBrevbestilling(
                brevtype = Brevtype.INNVILGELSE,
                ferdigstillAutomatisk = false,
            ).brevbestilling

            dataSource.transaction { connection ->
                val service = BrevbestillingService.konstruer(connection, fakeUnleash)

                // Feature av: ferdigstill krever ikke mottakerliste.
                service.ferdigstill(bestillingUtenToggle.referanse, null, emptyList())
                assertThat(
                    BrevbestillingRepositoryImpl(connection).hent(bestillingUtenToggle.referanse).status
                ).isEqualTo(Status.FERDIGSTILT)

                // Samme instans, feature på: ferdigstill krever mottakerliste.
                fakeUnleash.enable(BrevFeature.RedigerMottakerBrevbygger)
                assertThrows<ValideringsfeilException> {
                    service.ferdigstill(bestillingMedToggle.referanse, null, emptyList())
                }
            }
        }

        @Test
        fun `oppdaterMottakere feiler når feature er deaktivert`() {
            val bestilling = opprettBrevbestilling(ferdigstillAutomatisk = false).brevbestilling
            val referanse = bestilling.referanse

            dataSource.transaction { connection ->
                val service = BrevbestillingService.konstruer(connection)

                assertThrows<UgyldigForespørselException> {
                    service.oppdaterMottakere(
                        referanse, listOf(
                            Mottaker(
                                ident = bestilling.brukerIdent,
                                identType = IdentType.FNR,
                                bestillingMottakerReferanse = "${referanse.referanse}-1",
                                type = Mottaker.Type.HOVED,
                            )
                        )
                    )
                }
            }
        }

        @Test
        fun `oppdaterMottakere lagrer hovedmottaker og kopimottaker`() {
            val unleash = FakeUnleashGateway(BrevFeature.RedigerMottakerBrevbygger)
            val bestilling = opprettBrevbestilling(ferdigstillAutomatisk = false, unleashGateway = unleash).brevbestilling
            val referanse = bestilling.referanse

            dataSource.transaction { connection ->
                val service = BrevbestillingService.konstruer(connection, unleash)
                val mottakerRepository = MottakerRepositoryImpl(connection)

                val hovedmottaker = Mottaker(
                    ident = bestilling.brukerIdent,
                    identType = IdentType.FNR,
                    bestillingMottakerReferanse = "${referanse.referanse}-1",
                    type = Mottaker.Type.HOVED,
                )
                val kopimottaker = Mottaker(
                    navnOgAdresse = NavnOgAdresse(
                        navn = "verge",
                        adresse = Adresse(
                            landkode = "NO",
                            adresselinje1 = "adresselinje1",
                            postnummer = "1234",
                            poststed = "Oslo",
                        )
                    ),
                    bestillingMottakerReferanse = "${referanse.referanse}-2",
                    type = Mottaker.Type.KOPI,
                )

                service.oppdaterMottakere(referanse, listOf(hovedmottaker, kopimottaker))

                assertThat(mottakerRepository.hentMottakere(bestilling.id).map { it.copy(id = null) })
                    .containsExactlyInAnyOrder(hovedmottaker, kopimottaker)
            }
        }

        @Test
        fun `oppdaterMottakere feiler med tom mottakerliste`() {
            val unleash = FakeUnleashGateway(BrevFeature.RedigerMottakerBrevbygger)
            val bestilling = opprettBrevbestilling(ferdigstillAutomatisk = false, unleashGateway = unleash).brevbestilling
            val referanse = bestilling.referanse

            dataSource.transaction { connection ->
                val service = BrevbestillingService.konstruer(connection, unleash)

                assertThrows<ValideringsfeilException> {
                    service.oppdaterMottakere(referanse, emptyList())
                }
            }
        }

        @Test
        fun `oppdaterMottakere feiler når bestilling ikke har status under arbeid`() {
            val unleash = FakeUnleashGateway(BrevFeature.RedigerMottakerBrevbygger)
            val bestilling = opprettBrevbestilling(ferdigstillAutomatisk = false, unleashGateway = unleash).brevbestilling
            val referanse = bestilling.referanse

            dataSource.transaction { connection ->
                val service = BrevbestillingService.konstruer(connection, unleash)
                BrevbestillingRepositoryImpl(connection).oppdaterStatus(bestilling.id, Status.FERDIGSTILT)

                assertThrows<ValideringsfeilException> {
                    service.oppdaterMottakere(
                        referanse, listOf(
                            Mottaker(
                                ident = bestilling.brukerIdent,
                                identType = IdentType.FNR,
                                bestillingMottakerReferanse = "${referanse.referanse}-1",
                                type = Mottaker.Type.HOVED,
                            )
                        )
                    )
                }
            }
        }
    }
}
