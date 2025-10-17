package no.nav.aap.brev.innhold

import no.nav.aap.brev.IntegrationTest
import no.nav.aap.brev.bestilling.BrevbestillingId
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.Brevdata.Delmal
import no.nav.aap.brev.bestilling.Brevdata.FaktagrunnlagMedVerdi
import no.nav.aap.brev.bestilling.BrevmalJson
import no.nav.aap.brev.feil.ValideringsfeilException
import no.nav.aap.brev.kontrakt.Brevmal
import no.nav.aap.brev.kontrakt.FAKTAGRUNNLAG_TYPE_AAP_FOM_DATO
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.test.BrevmalBuilder
import no.nav.aap.brev.test.FileUtils
import no.nav.aap.brev.util.TimeUtils.formaterFullLengde
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class BrevbyggerServiceTest : IntegrationTest() {

    @Test
    fun `lagrer initielle brevdata`() {
        val brevmal = FileUtils.lesFilTilJson<BrevmalJson>("brevmal.json")
        val aapFomDato = Faktagrunnlag.AapFomDato(LocalDate.now())
        val faktagrunnlag = setOf(aapFomDato)

        val bestilling = opprettBrevbestilling(brukV3 = true, ferdigstillAutomatisk = false).brevbestilling
        oppdaterBrevmalJson(bestilling.id, brevmal)
        lagreInitiellBrevdata(bestilling.referanse, faktagrunnlag)

        dataSource.transaction { connection ->
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)

            val oppdatertBestilling = brevbestillingRepository.hent(bestilling.referanse)

            assertThat(oppdatertBestilling.brevdata?.delmaler)
                .containsExactlyInAnyOrder(Delmal("49d9c7a7-29db-43c6-aece-45e97314a50a"))

            assertThat(oppdatertBestilling.brevdata?.faktagrunnlag).containsExactlyInAnyOrder(
                FaktagrunnlagMedVerdi(
                    FAKTAGRUNNLAG_TYPE_AAP_FOM_DATO,
                    aapFomDato.dato.formaterFullLengde(bestilling.språk)
                )
            )
        }
    }

    @Test
    fun `validerAutomatiskFerdigstilling er ok dersom brevet kan sendes automatisk`() {
        val bestilling = opprettBrevbestilling(brukV3 = true, ferdigstillAutomatisk = false).brevbestilling
        oppdaterBrevmal(bestilling.id, BrevmalBuilder.builder {
            kanSendesAutomatisk = true
            delmal {
                obligatorisk = true
                faktagrunnlag(KjentFaktagrunnlag.AAP_FOM_DATO.name)
            }
        })
        lagreInitiellBrevdata(bestilling.referanse, setOf(Faktagrunnlag.AapFomDato(LocalDate.now())))
        assertDoesNotThrow {
            validerAutomatiskFerdigstilling(bestilling.referanse)
        }
    }

    @Test
    fun `validerAutomatiskFerdigstilling feiler dersom brevet ikke kan sendes automatisk`() {
        val bestilling = opprettBrevbestilling(brukV3 = true, ferdigstillAutomatisk = false).brevbestilling

        oppdaterBrevmal(bestilling.id, BrevmalBuilder.builder {
            kanSendesAutomatisk = false
        })
        val exception = assertThrows<ValideringsfeilException> {
            validerAutomatiskFerdigstilling(bestilling.referanse)
        }
        assertThat(exception.message).isEqualTo(
            "Kan ikke automatisk ferdigstille brevbestilling: Brevmal er ikke konfigurert til at brevet kan sendes automatisk."
        )
    }

    @Test
    fun `validerAutomatiskFerdigstilling feiler dersom brevet har delmaler som ikke er obligatorisk`() {
        val bestilling = opprettBrevbestilling(brukV3 = true, ferdigstillAutomatisk = false).brevbestilling

        oppdaterBrevmal(bestilling.id, BrevmalBuilder.builder {
            kanSendesAutomatisk = true
            delmal { obligatorisk = false }
        })
        val exception = assertThrows<ValideringsfeilException> {
            validerAutomatiskFerdigstilling(bestilling.referanse)
        }
        assertThat(exception.message).isEqualTo(
            "Kan ikke automatisk ferdigstille brevbestilling: Det er delmaler som ikke er obligatorisk."
        )
    }

    @Test
    fun `validerAutomatiskFerdigstilling feiler dersom brevmal har faktagrunnlag uten verdi`() {
        val bestilling = opprettBrevbestilling(brukV3 = true, ferdigstillAutomatisk = false).brevbestilling

        oppdaterBrevmal(bestilling.id, BrevmalBuilder.builder {
            kanSendesAutomatisk = true
            delmal {
                obligatorisk = true
                faktagrunnlag(KjentFaktagrunnlag.AAP_FOM_DATO.name)
                faktagrunnlag(KjentFaktagrunnlag.DAGSATS.name)
            }
        })
        lagreInitiellBrevdata(bestilling.referanse, setOf(Faktagrunnlag.AapFomDato(LocalDate.now())))
        val exception = assertThrows<ValideringsfeilException> {
            validerAutomatiskFerdigstilling(bestilling.referanse)
        }
        assertThat(exception.message).isEqualTo(
            "Kan ikke automatisk ferdigstille brevbestilling: Mangler faktagrunnlag for ${KjentFaktagrunnlag.DAGSATS}."
        )
    }

    @Test
    fun `validerAutomatiskFerdigstilling feiler dersom brevmal inneholder valg`() {
        val bestilling = opprettBrevbestilling(brukV3 = true, ferdigstillAutomatisk = false).brevbestilling

        oppdaterBrevmal(bestilling.id, BrevmalBuilder.builder {
            kanSendesAutomatisk = true
            delmal {
                obligatorisk = true
                valg {
                    obligatorisk = false
                    alternativ("kategori_1")
                    alternativ("kategori_2")
                    fritekst()
                }
            }
        })
        val exception = assertThrows<ValideringsfeilException> {
            validerAutomatiskFerdigstilling(bestilling.referanse)
        }
        assertThat(exception.message).isEqualTo(
            "Kan ikke automatisk ferdigstille brevbestilling: Det er delmaler som inneholder valg."
        )
    }

    @Test
    fun `validerAutomatiskFerdigstilling feiler dersom brevmal inneholder fritekst`() {
        val bestilling = opprettBrevbestilling(brukV3 = true, ferdigstillAutomatisk = false).brevbestilling

        oppdaterBrevmal(bestilling.id, BrevmalBuilder.builder {
            kanSendesAutomatisk = true
            delmal {
                obligatorisk = true
                fritekst()
            }
        })
        val exception = assertThrows<ValideringsfeilException> {
            validerAutomatiskFerdigstilling(bestilling.referanse)
        }
        assertThat(exception.message).isEqualTo(
            "Kan ikke automatisk ferdigstille brevbestilling: Det er delmaler som inneholder fritekst."
        )
    }

    @Test
    fun `validerAutomatiskFerdigstilling feiler dersom brevmal inneholder betinget tekst`() {
        val bestilling = opprettBrevbestilling(brukV3 = true, ferdigstillAutomatisk = false).brevbestilling

        oppdaterBrevmal(bestilling.id, BrevmalBuilder.builder {
            kanSendesAutomatisk = true
            delmal {
                obligatorisk = true
                betingetTekst(listOf("kategori_1", "kategori_2"), listOf(KjentFaktagrunnlag.AAP_FOM_DATO.name))
            }
        })
        lagreInitiellBrevdata(bestilling.referanse, setOf(Faktagrunnlag.AapFomDato(LocalDate.now())))
        val exception = assertThrows<ValideringsfeilException> {
            validerAutomatiskFerdigstilling(bestilling.referanse)
        }
        assertThat(exception.message).isEqualTo(
            "Kan ikke automatisk ferdigstille brevbestilling: Det er delmaler som inneholder betinget tekst."
        )
    }

    @Test
    fun `validerAutomatiskFerdigstilling feiler dersom brevmal inneholder periodetekst`() {
        val bestilling = opprettBrevbestilling(brukV3 = true, ferdigstillAutomatisk = false).brevbestilling

        oppdaterBrevmal(bestilling.id, BrevmalBuilder.builder {
            kanSendesAutomatisk = true
            delmal {
                obligatorisk = true
                periodetekst(listOf("VAR_1", "VAR_2"))
            }
        })
        val exception = assertThrows<ValideringsfeilException> {
            validerAutomatiskFerdigstilling(bestilling.referanse)
        }
        assertThat(exception.message).isEqualTo(
            "Kan ikke automatisk ferdigstille brevbestilling: Det er delmaler som inneholder periodetekst."
        )
    }

    private fun validerAutomatiskFerdigstilling(referanse: BrevbestillingReferanse) {
        dataSource.transaction { connection ->
            val brevbyggerService = BrevbyggerService.konstruer(connection)
            brevbyggerService.validerAutomatiskFerdigstilling(referanse)
        }
    }

    private fun oppdaterBrevmal(id: BrevbestillingId, brevmal: Brevmal) {
        oppdaterBrevmalJson(id, DefaultJsonMapper.fromJson(DefaultJsonMapper.toJson(brevmal)))
    }

    private fun oppdaterBrevmalJson(id: BrevbestillingId, brevmalJson: BrevmalJson) {
        dataSource.transaction { connection ->
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)
            brevbestillingRepository.oppdaterBrevmal(id, brevmalJson)
        }
    }

    private fun lagreInitiellBrevdata(referanse: BrevbestillingReferanse, faktagrunnlag: Set<Faktagrunnlag>) {
        dataSource.transaction { connection ->
            val brevbestillingRepository = BrevbyggerService.konstruer(connection)
            brevbestillingRepository.lagreInitiellBrevdata(referanse, faktagrunnlag)
        }
    }
}