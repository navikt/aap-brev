package no.nav.aap.brev.innhold

import no.nav.aap.brev.IntegrationTest
import no.nav.aap.brev.bestilling.BrevbestillingId
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.Brevdata.Delmal
import no.nav.aap.brev.bestilling.Brevdata.FaktagrunnlagMedVerdi
import no.nav.aap.brev.bestilling.BrevmalJson
import no.nav.aap.brev.kontrakt.FAKTAGRUNNLAG_TYPE_AAP_FOM_DATO
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.test.FileUtils
import no.nav.aap.brev.util.TimeUtils.formaterFullLengde
import no.nav.aap.komponenter.dbconnect.transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrevbyggerServiceTest : IntegrationTest() {
    @Test
    fun `lagrer initielle brevdata`() {
        val brevmal = FileUtils.lesFilTilJson<BrevmalJson>("brevmal.json")
        val aapFomDato = Faktagrunnlag.AapFomDato(LocalDate.now())
        val faktagrunnlag = setOf(aapFomDato)

        val bestilling = opprettBrevbestilling(ferdigstillAutomatisk = false).brevbestilling
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
}
