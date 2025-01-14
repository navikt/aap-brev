package no.nav.aap.brev.innhold

import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.Faktagrunnlag
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.test.fakes.brev
import no.nav.aap.brev.test.fakes.faktagrunnlagForBehandling
import no.nav.aap.brev.test.fakes.randomBehandlingReferanse
import no.nav.aap.brev.test.fakes.randomSaksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class HentFaktagrunnlagTest {
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
    fun `erstatt faktagrunnlag`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val hentFaktagrunnlagService = HentFaktagrunnlagService.konstruer(connection)
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

            val ubehandletBrev = brev()

            brevbestillingRepository.oppdaterBrev(referanse, ubehandletBrev)

            val hentetBrev = brevbestillingRepository.hent(referanse)
            assertTrue(HentFaktagrunnlagService.harFaktagrunnlag(hentetBrev.brev!!))

            hentFaktagrunnlagService.hentFaktagrunnlag(referanse)

            val oppdatertBrev = brevbestillingRepository.hent(referanse).brev
            assertFalse(HentFaktagrunnlagService.harFaktagrunnlag(oppdatertBrev!!))
        }
    }

    @Test
    fun `har faktagrunnlag`() {
        assertTrue(HentFaktagrunnlagService.harFaktagrunnlag(brev(medFaktaGrunnlag = true)))
    }

    @Test
    fun `har ikke faktagrunnlag`() {
        assertFalse(HentFaktagrunnlagService.harFaktagrunnlag(brev(medFaktaGrunnlag = false)))
    }
}