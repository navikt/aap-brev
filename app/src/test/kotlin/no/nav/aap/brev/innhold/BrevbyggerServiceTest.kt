package no.nav.aap.brev.innhold

import no.nav.aap.brev.bestilling.Brevdata
import no.nav.aap.brev.IntegrationTest
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
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
        val brevbestilling = opprettBrevbestilling(ferdigstillAutomatisk = false).brevbestilling
        dataSource.transaction { connection ->
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)
            val brevbyggerService = BrevbyggerService.konstruer(connection)
            val brevmal = FileUtils.lesFilTilJson<BrevmalJson>("brevmal.json")
            val aapFomDato = Faktagrunnlag.AapFomDato(LocalDate.now())
            val faktagrunnlag = setOf(aapFomDato)

            brevbestillingRepository.oppdaterBrevmal(brevbestilling.id, brevmal)

            brevbyggerService.lagreInitiellBrevdata(
                brevbestilling.referanse,
                faktagrunnlag
            )

            val oppdatertBestilling = brevbestillingRepository.hent(brevbestilling.referanse)

            assertThat(oppdatertBestilling.brevdata?.delmaler)
                .containsExactlyInAnyOrder(Brevdata.Delmal("49d9c7a7-29db-43c6-aece-45e97314a50a"))

            assertThat(oppdatertBestilling.brevdata?.faktagrunnlag).containsExactlyInAnyOrder(
                Brevdata.FaktagrunnlagMedVerdi(
                    FAKTAGRUNNLAG_TYPE_AAP_FOM_DATO,
                    aapFomDato.dato.formaterFullLengde(brevbestilling.språk)
                )
            )
        }
    }
}