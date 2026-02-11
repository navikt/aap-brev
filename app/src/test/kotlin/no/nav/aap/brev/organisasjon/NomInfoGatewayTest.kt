package no.nav.aap.brev.organisasjon

import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.test.fakes.nomDataForNavIdent
import no.nav.aap.brev.test.randomNavIdent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class NomInfoGatewayTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            Fakes.start()
        }
    }

    @Test
    fun `bruker enhet for gjeldende OrgTilknytning med daglig oppfølging`() {
        val navIdent = randomNavIdent()
        nomDataForNavIdent(
            navIdent, NomData(
                NomDataRessurs(
                    orgTilknytning = listOf(
                        OrgTilknytning(
                            orgEnhet = OrgEnhet("1234"),
                            erDagligOppfolging = true,
                            gyldigFom = LocalDate.now().minusDays(61),
                            gyldigTom = LocalDate.now().minusDays(31)
                        ),
                        OrgTilknytning(
                            orgEnhet = OrgEnhet("2345"),
                            erDagligOppfolging = true,
                            gyldigFom = LocalDate.now().minusDays(30),
                            gyldigTom = LocalDate.now().minusDays(1)
                        ),
                        OrgTilknytning(
                            orgEnhet = OrgEnhet("3456"),
                            erDagligOppfolging = true,
                            gyldigFom = LocalDate.now(),
                            gyldigTom = null
                        ),
                        OrgTilknytning(
                            orgEnhet = OrgEnhet("4567"),
                            erDagligOppfolging = false,
                            gyldigFom = LocalDate.now(),
                            gyldigTom = null
                        )
                    ), visningsnavn = "Saksbehandler Navn"
                )
            )
        )

        val ansattInfo = NomInfoGateway().hentAnsattInfo(navIdent)

        assertThat(ansattInfo.navIdent).isEqualTo(navIdent)
        assertThat(ansattInfo.navn).isEqualTo("Saksbehandler Navn")
        assertThat(ansattInfo.enhetsnummer).isEqualTo("3456")
    }

    @Test
    fun `bruker enhet fra siste OrgTilknytning med daglig oppfølging dersom det ikke finnes en som er gyldig nå`() {
        val navIdent = randomNavIdent()
        nomDataForNavIdent(
            navIdent, NomData(
                NomDataRessurs(
                    orgTilknytning = listOf(
                        OrgTilknytning(
                            orgEnhet = OrgEnhet("1234"),
                            erDagligOppfolging = true,
                            gyldigFom = LocalDate.now().minusDays(61),
                            gyldigTom = LocalDate.now().minusDays(31)
                        ),
                        OrgTilknytning(
                            orgEnhet = OrgEnhet("2345"),
                            erDagligOppfolging = true,
                            gyldigFom = LocalDate.now().minusDays(30),
                            gyldigTom = LocalDate.now().minusDays(1)
                        ),
                    ), visningsnavn = "Saksbehandler Navn"
                )
            )
        )

        val ansattInfo = NomInfoGateway().hentAnsattInfo(navIdent)

        assertThat(ansattInfo.navIdent).isEqualTo(navIdent)
        assertThat(ansattInfo.navn).isEqualTo("Saksbehandler Navn")
        assertThat(ansattInfo.enhetsnummer).isEqualTo("2345")
    }

    @Test
    fun `feiler dersom det ikke finnes en OrgTilknytning daglig oppfølging`() {
        val navIdent = randomNavIdent()
        nomDataForNavIdent(
            navIdent, NomData(
                NomDataRessurs(
                    orgTilknytning = listOf(
                        OrgTilknytning(
                            orgEnhet = OrgEnhet("4567"),
                            erDagligOppfolging = false,
                            gyldigFom = LocalDate.now(),
                            gyldigTom = null
                        )
                    ), visningsnavn = "Saksbehandler Navn"
                )
            )
        )

        val exception = assertThrows<IllegalStateException> {
            NomInfoGateway().hentAnsattInfo(navIdent)
        }
        assertThat(exception.message)
            .isEqualTo("Fant ikke OrgTilknytning for ansatt. Klarer ikke utlede enhet for signatur.")
    }

    @Test
    fun `feiler dersom valgt OrgTilknytning mangler enhet ID fra Remedy`() {
        val navIdent = randomNavIdent()
        nomDataForNavIdent(
            navIdent, NomData(
                NomDataRessurs(
                    orgTilknytning = listOf(
                        OrgTilknytning(
                            orgEnhet = OrgEnhet(remedyEnhetId = null),
                            erDagligOppfolging = true,
                            gyldigFom = LocalDate.now(),
                            gyldigTom = null
                        )
                    ), visningsnavn = "Saksbehandler Navn"
                )
            )
        )
        val exception = assertThrows<IllegalStateException> {
            NomInfoGateway().hentAnsattInfo(navIdent)
        }
        assertThat(exception.message)
            .isEqualTo("Klarer ikke utlede enhet for signatur. OrgEnhet til OrgTilknytning mangler RemedyEnhetId.")
    }
}
