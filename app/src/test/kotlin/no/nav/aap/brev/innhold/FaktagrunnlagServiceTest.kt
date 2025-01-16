package no.nav.aap.brev.innhold

import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.Faktagrunnlag
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.FaktagrunnlagType
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.test.fakes.brev
import no.nav.aap.brev.test.fakes.faktagrunnlagForBehandling
import no.nav.aap.brev.test.fakes.randomBehandlingReferanse
import no.nav.aap.brev.test.fakes.randomSaksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat

class FaktagrunnlagServiceTest {
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
    fun `finner og fyller inn faktagrunnlag som ligger til grunn for en behandling`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val faktagrunnlagService = FaktagrunnlagService.konstruer(connection)
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)

            val behandlingReferanse = randomBehandlingReferanse()
            val referanse =
                brevbestillingService.opprettBestilling(
                    saksnummer = randomSaksnummer(),
                    behandlingReferanse = behandlingReferanse,
                    unikReferanse = UUID.randomUUID().toString(),
                    brevtype = Brevtype.INNVILGELSE,
                    språk = Språk.NB,
                    vedlegg = emptySet(),
                )

            faktagrunnlagForBehandling(behandlingReferanse, setOf(Faktagrunnlag.Testverdi("Testverdi")))

            val ubehandletBrev =
                brev(medFaktagrunnlag = listOf(FaktagrunnlagType.TESTVERDI.verdi, "ukjentFaktagrunnlag"))

            brevbestillingRepository.oppdaterBrev(referanse, ubehandletBrev)

            val hentetBrev = checkNotNull(brevbestillingRepository.hent(referanse).brev)

            assertThat(
                FaktagrunnlagService.finnFaktagrunnlag(hentetBrev).map { it.tekniskNavn }
            ).containsExactlyInAnyOrder(FaktagrunnlagType.TESTVERDI.verdi, "ukjentFaktagrunnlag")

            faktagrunnlagService.hentOgFyllInnFaktagrunnlag(referanse)

            val oppdatertBrev = checkNotNull(brevbestillingRepository.hent(referanse).brev)

            assertThat(
                FaktagrunnlagService.finnFaktagrunnlag(oppdatertBrev).map { it.tekniskNavn }
            ).containsExactlyInAnyOrder("ukjentFaktagrunnlag")
        }
    }

    @Test
    fun `har faktagrunnlag`() {
        assertTrue(FaktagrunnlagService.harFaktagrunnlag(brev(listOf("faktagrunnlag"))))
    }

    @Test
    fun `har ikke faktagrunnlag`() {
        assertFalse(FaktagrunnlagService.harFaktagrunnlag(brev(medFaktagrunnlag = emptyList())))
    }
}
