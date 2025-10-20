package no.nav.aap.brev.innhold

import no.nav.aap.brev.IntegrationTest
import no.nav.aap.brev.bestilling.BrevbestillingId
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.Brevdata
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
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year

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
    fun `validerFerdigstilling er ok dersom brevet kan sendes`() {
        val bestilling = opprettBrevbestilling(brukV3 = true, ferdigstillAutomatisk = false).brevbestilling

        lagreInitiellBrevdata(
            bestilling.referanse, setOf(
                Faktagrunnlag.AapFomDato(LocalDate.now()), Faktagrunnlag.GrunnlagBeregning(
                    beregningstidspunkt = LocalDate.now(),
                    beregningsgrunnlag = BigDecimal("123123"),
                    inntekterPerÅr = listOf(
                        Faktagrunnlag.GrunnlagBeregning.InntektPerÅr(
                            Year.of(2022),
                            BigDecimal("321321")
                        )
                    )
                )
            )
        )
        oppdaterBrevmal(bestilling.id, BrevmalBuilder.builder {
            kanSendesAutomatisk = false
            delmal {
                obligatorisk = false
            }
            delmal {

                obligatorisk = true
                faktagrunnlag(KjentFaktagrunnlag.AAP_FOM_DATO.name)
                val valg1 = valg {
                    obligatorisk = true
                    alternativ("kategori_1", listOf(KjentFaktagrunnlag.BEREGNINGSTIDSPUNKT.name))
                    alternativ("kategori_2", emptyList())
                }
                val periodetekst1 = periodetekst(
                    listOf(
                        KjentFaktagrunnlag.FRIST_DATO_11_7.name,
                        KjentFaktagrunnlag.BEREGNINGSGRUNNLAG.name
                    )
                )

                val betingetTekst1 = betingetTekst(
                    listOf("kategori_1"), listOf(KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_1_AARSTALL.name)
                )
                val fritekst = fritekst()

                oppdaterBrevdata(bestilling.referanse) {
                    this.copy(
                        delmaler = listOf(Delmal(_id)),
                        faktagrunnlag = listOf(
                            FaktagrunnlagMedVerdi(KjentFaktagrunnlag.AAP_FOM_DATO.name, "AAP_FOM_DATO"),
                            FaktagrunnlagMedVerdi(KjentFaktagrunnlag.BEREGNINGSTIDSPUNKT.name, "BEREGNINGSTIDSPUNKT"),
                            FaktagrunnlagMedVerdi(KjentFaktagrunnlag.BEREGNINGSGRUNNLAG.name, "BEREGNINGSGRUNNLAG"),
                            FaktagrunnlagMedVerdi(
                                KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_1_AARSTALL.name,
                                "GRUNNLAG_BEREGNING_AAR_1_AARSTALL"
                            )
                        ),
                        periodetekster = listOf(
                            Brevdata.Periodetekst(
                                periodetekst1.periodetekst._id, listOf(
                                    FaktagrunnlagMedVerdi(KjentFaktagrunnlag.FRIST_DATO_11_7.name, "FRIST_DATO_11_7"),
                                    FaktagrunnlagMedVerdi(
                                        KjentFaktagrunnlag.BEREGNINGSGRUNNLAG.name,
                                        "BEREGNINGSGRUNNLAG"
                                    )
                                )
                            ),
                        ),
                        valg = listOf(Brevdata.Valg(valg1.valg._id, valg1.valg.alternativer.first()._key, null)),
                        betingetTekst = listOf(
                            Brevdata.BetingetTekst(betingetTekst1.tekst._id)
                        ),
                        fritekster = listOf(
                            Brevdata.FritekstMedKey(
                                fritekst._key,
                                Brevdata.Fritekst(DefaultJsonMapper.fromJson("""{"fritekst": "abc"}"""))
                            )
                        )
                    )
                }
            }

        })
        assertDoesNotThrow {
            validerFerdigstilling(bestilling.referanse)
        }
    }

    @Test
    fun `validerFerdigstilling feiler dersom obligatorisk delmal ikke er valgt`() {
        val bestilling = opprettBrevbestilling(brukV3 = true, ferdigstillAutomatisk = false).brevbestilling

        var manglendeDelmal: Brevmal.DelmalValg? = null
        oppdaterBrevmal(bestilling.id, BrevmalBuilder.builder {
            kanSendesAutomatisk = false
            val delmal1 = delmal {
                obligatorisk = false
            }
            manglendeDelmal = delmal {
                obligatorisk = true
            }
            oppdaterBrevdata(bestilling.referanse) {
                this.copy(delmaler = listOf(Delmal(delmal1.delmal._id)))
            }
        })
        val exception = assertThrows<ValideringsfeilException> {
            validerFerdigstilling(bestilling.referanse)
        }
        assertThat(exception.message).isEqualTo(
            "Kan ikke ferdigstille brevbestilling med referanse=${bestilling.referanse.referanse}. Validering av brevinnhold feilet: Mangler obligatoriske delmaler med id ${manglendeDelmal?.delmal?._id}."
        )
    }

    @Test
    fun `validerFerdigstilling feiler dersom fritekst i valgt delmal mangler`() {
        val bestilling = opprettBrevbestilling(brukV3 = true, ferdigstillAutomatisk = false).brevbestilling

        var delmalMedMangler: Brevmal.DelmalValg? = null
        var manglendeFritekst: Brevmal.TeksteditorElement.Fritekst? = null
        oppdaterBrevmal(bestilling.id, BrevmalBuilder.builder {
            kanSendesAutomatisk = false
            delmalMedMangler = delmal {
                manglendeFritekst = fritekst()
            }
            oppdaterBrevdata(bestilling.referanse) {
                this.copy(delmaler = listOf(Delmal(delmalMedMangler.delmal._id)))
            }
        })
        val exception = assertThrows<ValideringsfeilException> {
            validerFerdigstilling(bestilling.referanse)
        }
        assertThat(exception.message).isEqualTo(
            "Kan ikke ferdigstille brevbestilling med referanse=${bestilling.referanse.referanse}. Validering av brevinnhold feilet: Mangler fritekst(er) med key ${manglendeFritekst?._key} for delmal med id ${delmalMedMangler?.delmal?._id}."
        )
    }

    @Test
    fun `validerFerdigstilling feiler dersom faktagrunnlag mangler verdi`() {
        val bestilling = opprettBrevbestilling(brukV3 = true, ferdigstillAutomatisk = false).brevbestilling

        oppdaterBrevmal(bestilling.id, BrevmalBuilder.builder {
            kanSendesAutomatisk = false
             delmal {
                faktagrunnlag(KjentFaktagrunnlag.AAP_FOM_DATO.name)
                val valg1 = valg {
                    obligatorisk = true
                    alternativ("kategori_1", listOf(KjentFaktagrunnlag.BEREGNINGSGRUNNLAG.name))
                    alternativ("kategori_2", listOf(KjentFaktagrunnlag.DAGSATS.name))
                }
                val betingetTekst1 =
                    betingetTekst(listOf("kategori_1", "kategori_2"), listOf(KjentFaktagrunnlag.BARNETILLEGG_SATS.name))
                oppdaterBrevdata(bestilling.referanse) {
                    this.copy(
                        delmaler = listOf(Delmal(_id)),
                        valg = listOf(
                            Brevdata.Valg(
                                valg1.valg._id,
                                valg1.valg.alternativer.first()._key,
                                null
                            )
                        ),
                        betingetTekst = listOf(Brevdata.BetingetTekst(betingetTekst1.tekst._id))
                    )
                }
            }
        })
        val exception = assertThrows<ValideringsfeilException> {
            validerFerdigstilling(bestilling.referanse)
        }
        assertThat(exception.message).isEqualTo(
            "Kan ikke ferdigstille brevbestilling med referanse=${bestilling.referanse.referanse}. Validering av brevinnhold feilet: Mangler faktagrunnlag for ${KjentFaktagrunnlag.AAP_FOM_DATO.name},${KjentFaktagrunnlag.BEREGNINGSGRUNNLAG.name},${KjentFaktagrunnlag.BARNETILLEGG_SATS.name}."
        )
    }

    @Test
    fun `validerFerdigstilling feiler dersom obligatorisk valg ikke er valgt`() {
        val bestilling = opprettBrevbestilling(brukV3 = true, ferdigstillAutomatisk = false).brevbestilling

        var delmalMedMangler: Brevmal.DelmalValg? = null
        var manglendeValg: Brevmal.TeksteditorElement.Valg? = null
        oppdaterBrevmal(bestilling.id, BrevmalBuilder.builder {
            kanSendesAutomatisk = false
            delmalMedMangler = delmal {
                manglendeValg = valg {
                    obligatorisk = true
                    alternativ("kategori_1", emptyList())
                    alternativ("kategori_2", emptyList())
                }
            }
            oppdaterBrevdata(bestilling.referanse) {
                this.copy(delmaler = listOf(Delmal(delmalMedMangler.delmal._id)))
            }
        })
        val exception = assertThrows<ValideringsfeilException> {
            validerFerdigstilling(bestilling.referanse)
        }
        assertThat(exception.message).isEqualTo(
            "Kan ikke ferdigstille brevbestilling med referanse=${bestilling.referanse.referanse}. Validering av brevinnhold feilet: Obligatorisk(e) valg med id ${manglendeValg?.valg?._id} er ikke valgt."
        )
    }

    @Test
    fun `validerFerdigstilling feiler dersom valgt valg mangler faktagrunnlag`() {
        val bestilling = opprettBrevbestilling(brukV3 = true, ferdigstillAutomatisk = false).brevbestilling

        oppdaterBrevmal(bestilling.id, BrevmalBuilder.builder {
            kanSendesAutomatisk = false
            delmal {
                val valg1 = valg {
                    obligatorisk = true
                    alternativ("kategori_1", emptyList())
                    alternativ("kategori_1", listOf(KjentFaktagrunnlag.BEREGNINGSTIDSPUNKT.name))
                }

                oppdaterBrevdata(bestilling.referanse) {
                    this.copy(
                        delmaler = listOf(Delmal(_id)),
                        valg = listOf(
                            Brevdata.Valg(
                                valg1.valg._id,
                                valg1.valg.alternativer[1]._key,
                                null
                            )
                        )
                    )
                }
            }
        })
        val exception = assertThrows<ValideringsfeilException> {
            validerFerdigstilling(bestilling.referanse)
        }
        assertThat(exception.message).isEqualTo(
            "Kan ikke ferdigstille brevbestilling med referanse=${bestilling.referanse.referanse}. Validering av brevinnhold feilet: Mangler faktagrunnlag for ${KjentFaktagrunnlag.BEREGNINGSTIDSPUNKT.name}."
        )
    }

    @Test
    fun `validerFerdigstilling feiler dersom valgt periodetekst mangler faktagrunnlag`() {
        val bestilling = opprettBrevbestilling(brukV3 = true, ferdigstillAutomatisk = false).brevbestilling

        oppdaterBrevmal(bestilling.id, BrevmalBuilder.builder {
            kanSendesAutomatisk = false
            delmal {
                val periodetekst1 = periodetekst(
                    listOf(
                        KjentFaktagrunnlag.FRIST_DATO_11_7.name,
                        KjentFaktagrunnlag.BEREGNINGSGRUNNLAG.name
                    )
                )
                oppdaterBrevdata(bestilling.referanse) {
                    this.copy(
                        delmaler = listOf(Delmal(_id)),
                        periodetekster = listOf(
                            Brevdata.Periodetekst(
                                periodetekst1.periodetekst._id, listOf(
                                    FaktagrunnlagMedVerdi(KjentFaktagrunnlag.FRIST_DATO_11_7.name, "FRIST_DATO_11_7")
                                )
                            ),
                        ),
                    )
                }
            }
        })
        val exception = assertThrows<ValideringsfeilException> {
            validerFerdigstilling(bestilling.referanse)
        }
        assertThat(exception.message).isEqualTo(
            "Kan ikke ferdigstille brevbestilling med referanse=${bestilling.referanse.referanse}. Validering av brevinnhold feilet: Mangler faktagrunnlag ${KjentFaktagrunnlag.BEREGNINGSGRUNNLAG.name} for periodetekst."
        )
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

    private fun validerFerdigstilling(referanse: BrevbestillingReferanse) {
        dataSource.transaction { connection ->
            val brevbyggerService = BrevbyggerService.konstruer(connection)
            brevbyggerService.validerFerdigstilling(referanse)
        }
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
            val brevbyggerService = BrevbyggerService.konstruer(connection)
            brevbyggerService.lagreInitiellBrevdata(referanse, faktagrunnlag)
        }
    }

    private fun oppdaterBrevdata(referanse: BrevbestillingReferanse, oppdater: Brevdata.() -> Brevdata) {
        dataSource.transaction { connection ->
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)
            val bestilling = brevbestillingRepository.hent(referanse)
            val eksisterendeBrevdata = bestilling.brevdata ?: Brevdata(
                delmaler = emptyList(),
                faktagrunnlag = emptyList(),
                periodetekster = emptyList(),
                valg = emptyList(),
                betingetTekst = emptyList(),
                fritekster = emptyList()
            )
            brevbestillingRepository.oppdaterBrevdata(bestilling.id, oppdater(eksisterendeBrevdata))
        }
    }
}