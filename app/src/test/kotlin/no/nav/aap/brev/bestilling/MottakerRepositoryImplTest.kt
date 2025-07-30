package no.nav.aap.brev.bestilling

import no.nav.aap.brev.IntegrationTest
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.brev.test.randomBrevtype
import no.nav.aap.brev.test.randomBrukerIdent
import no.nav.aap.brev.test.randomSaksnummer
import no.nav.aap.brev.test.randomSpråk
import no.nav.aap.brev.test.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MottakerRepositoryImplTest  {

    companion object {
        private val dataSource = InitTestDatabase.freshDatabase()
    }

    @Test
    fun `lagrer og henter`() {
        dataSource.transaction { connection ->
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)
            val mottakerRepository = MottakerRepositoryImpl(connection)

            val bestilling = brevbestillingRepository.opprettBestilling(
                saksnummer = randomSaksnummer(),
                brukerIdent = randomBrukerIdent(),
                behandlingReferanse = randomBehandlingReferanse(),
                unikReferanse = randomUnikReferanse(),
                brevtype = randomBrevtype(),
                språk = randomSpråk(),
                vedlegg = emptySet()
            )

            val brukerIdent = randomBrukerIdent()

            val mottaker1 = Mottaker(
                ident = brukerIdent,
                identType = IdentType.FNR,
                bestillingMottakerReferanse = "${bestilling.referanse.referanse}-1"
            )
            val mottaker2 = Mottaker(
                navnOgAdresse = NavnOgAdresse(
                    navn = "verge", adresse = Adresse(
                        landkode = "NOR",
                        adresselinje1 = "adresselinje1",
                        adresselinje2 = "adresselinje2",
                        adresselinje3 = "adresselinje3",
                        postnummer = "postnummer",
                        poststed = "poststed",
                    )
                ),
                bestillingMottakerReferanse = "${bestilling.referanse.referanse}-2"
            )
            mottakerRepository.lagreMottakere(
                bestilling.id,
                listOf(mottaker1, mottaker2)
            )

            assertThat(
                mottakerRepository.hentMottakere(bestilling.id).map { it.copy(id = null) }).containsExactlyInAnyOrder(mottaker1,
                mottaker2
            )

            assertThat(
                mottakerRepository.hentMottakere(bestilling.referanse)
                    .map { it.copy(id = null) }).containsExactlyInAnyOrder(mottaker1, mottaker2)
        }
    }
}
