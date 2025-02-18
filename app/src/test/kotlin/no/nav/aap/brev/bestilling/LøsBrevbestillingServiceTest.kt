package no.nav.aap.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.FaktagrunnlagType
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.test.fakes.brev
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.brev.test.randomBrevtype
import no.nav.aap.brev.test.randomSaksnummer
import no.nav.aap.brev.test.randomSpråk
import no.nav.aap.brev.test.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LøsBrevbestillingServiceTest {
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
    fun `ferdigstiller brev som ikke har faktagrunnlag, ikke kan redigeres, og er fullsteindig`() {
        val referanse = gittBestilling(
            brev = brev(
                medFaktagrunnlag = emptyList(),
                kanRedigeres = false,
                erFullstendig = true,
            )
        )
        assertLøsBestillingStatus(referanse, Status.FERDIGSTILT)
    }

    @Test
    fun `gir status under arbeid for brev som har faktagrunnlag`() {
        val referanse = gittBestilling(
            brev = brev(
                medFaktagrunnlag = listOf(FaktagrunnlagType.FRIST_DATO_11_7.verdi),
                kanRedigeres = false,
                erFullstendig = true,
            )
        )
        assertLøsBestillingStatus(referanse, Status.UNDER_ARBEID)
    }

    @Test
    fun `gir status under arbeid for brev som kan redigeres`() {
        val referanse = gittBestilling(
            brev = brev(
                medFaktagrunnlag = emptyList(),
                kanRedigeres = true,
                erFullstendig = true,
            )
        )
        assertLøsBestillingStatus(referanse, Status.UNDER_ARBEID)
    }

    @Test
    fun `gir status under arbeid for brev som ikke er fullstendige`() {
        val referanse = gittBestilling(
            brev = brev(
                medFaktagrunnlag = emptyList(),
                kanRedigeres = false,
                erFullstendig = false,
            )
        )
        assertLøsBestillingStatus(referanse, Status.UNDER_ARBEID)
    }

    fun gittBestilling(brev: Brev): BrevbestillingReferanse {
        return dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)


            val referanse = brevbestillingService.opprettBestilling(
                saksnummer = randomSaksnummer(),
                behandlingReferanse = randomBehandlingReferanse(),
                unikReferanse = randomUnikReferanse(),
                brevtype = randomBrevtype(),
                språk = randomSpråk(),
                vedlegg = emptySet(),
            ).referanse

            brevbestillingRepository.oppdaterBrev(
                referanse, brev
            )
            referanse
        }
    }

    fun assertLøsBestillingStatus(referanse: BrevbestillingReferanse, status: Status) {
        dataSource.transaction { connection ->
            val løsBrevbestillingService = LøsBrevbestillingService.konstruer(connection)
            assertEquals(status, løsBrevbestillingService.løsBestilling(referanse))
        }
    }
}
