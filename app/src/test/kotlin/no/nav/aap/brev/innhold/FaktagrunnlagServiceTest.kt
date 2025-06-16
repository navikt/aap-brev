package no.nav.aap.brev.innhold

import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.Faktagrunnlag
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.test.fakes.brev
import no.nav.aap.brev.test.fakes.faktagrunnlagForBehandling
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.brev.test.randomBrukerIdent
import no.nav.aap.brev.test.randomSaksnummer
import no.nav.aap.brev.test.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year

class FaktagrunnlagServiceTest {
    companion object {
        private val dataSource = InitTestDatabase.freshDatabase()

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            Fakes.start()
        }
    }

    @Test
    fun `finner og fyller inn faktagrunnlag som ligger til grunn for en behandling`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val faktagrunnlagService = FaktagrunnlagService.konstruer(connection)
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)

            val behandlingReferanse = randomBehandlingReferanse()
            val referanse =
                brevbestillingService.opprettBestillingV2(
                    saksnummer = randomSaksnummer(),
                    brukerIdent = randomBrukerIdent(),
                    behandlingReferanse = behandlingReferanse,
                    unikReferanse = randomUnikReferanse(),
                    brevtype = Brevtype.INNVILGELSE,
                    språk = Språk.NB,
                    faktagrunnlag = emptySet(),
                    vedlegg = emptySet(),
                    ferdigstillAutomatisk = false,
                ).brevbestilling.referanse

            faktagrunnlagForBehandling(
                behandlingReferanse, setOf(
                    Faktagrunnlag.FristDato11_7(LocalDate.now()),
                    Faktagrunnlag.GrunnlagBeregning(
                        listOf(
                            Faktagrunnlag.GrunnlagBeregning.InntektPerÅr(Year.of(2020), BigDecimal(123)),
                            Faktagrunnlag.GrunnlagBeregning.InntektPerÅr(Year.of(2021), BigDecimal(123)),
                        )
                    )
                )
            )

            val ubehandletBrev =
                brev(
                    medFaktagrunnlag = listOf(
                        KjentFaktagrunnlag.FRIST_DATO_11_7.name,
                        KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_1_AARSTALL.name,
                        KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_2_AARSTALL.name,
                        KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_3_AARSTALL.name,
                        KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_1_INNTEKT.name,
                        KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_2_INNTEKT.name,
                        KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_3_INNTEKT.name,
                        "ukjentFaktagrunnlag"
                    )
                )

            brevbestillingRepository.oppdaterBrev(referanse, ubehandletBrev)

            val hentetBrev = checkNotNull(brevbestillingRepository.hent(referanse).brev)

            assertThat(
                hentetBrev.alleFaktagrunnlag().map { it.tekniskNavn }
            ).containsExactlyInAnyOrder(
                KjentFaktagrunnlag.FRIST_DATO_11_7.name,
                KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_1_AARSTALL.name,
                KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_2_AARSTALL.name,
                KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_3_AARSTALL.name,
                KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_1_INNTEKT.name,
                KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_2_INNTEKT.name,
                KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_3_INNTEKT.name,
                "ukjentFaktagrunnlag"
            )

            faktagrunnlagService.hentOgFyllInnFaktagrunnlag(referanse)

            val oppdatertBrev = checkNotNull(brevbestillingRepository.hent(referanse).brev)

            assertThat(
                oppdatertBrev.alleFaktagrunnlag().map { it.tekniskNavn }
            ).containsExactlyInAnyOrder(
                KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_3_AARSTALL.name,
                KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_3_INNTEKT.name,
                "ukjentFaktagrunnlag")
        }
    }
}
