package no.nav.aap.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.FaktagrunnlagType
import no.nav.aap.brev.exception.ValideringsfeilException
import no.nav.aap.brev.innhold.BrevinnholdService
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.prosessering.ProsesseringStatus
import no.nav.aap.brev.test.fakes.brev
import no.nav.aap.brev.test.randomBehandlingReferanse
import no.nav.aap.brev.test.randomBrukerIdent
import no.nav.aap.brev.test.randomSaksnummer
import no.nav.aap.brev.test.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode

class FerdigstillValideringTest {

    companion object {

        private val dataSource = InitTestDatabase.dataSource

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            Fakes.start()
        }
    }

    @Test
    fun `ferdigstilling går igjennom dersom ingen valideringsfeil`() {
        val referanse =
            gittBrevMed(brev = brev(medFaktagrunnlag = emptyList()), status = ProsesseringStatus.BREVBESTILLING_LØST)
        assertAntallJobber(referanse, 1)
        ferdigstill(referanse)
        assertAntallJobber(referanse, 2)
    }

    @Test
    fun `ferdigstilling feiler dersom brevet har faktagrunnlag`() {
        val referanse =
            gittBrevMed(
                brev = brev(medFaktagrunnlag = listOf(FaktagrunnlagType.FRIST_DATO_11_7.verdi)),
                status = ProsesseringStatus.BREVBESTILLING_LØST
            )
        assertAntallJobber(referanse, 1)
        val exception = assertThrows<ValideringsfeilException> {
            ferdigstill(referanse)
        }
        assertThat(exception.message).endsWith(
            "Brevet mangler utfylling av faktagrunnlag med teknisk navn: ${FaktagrunnlagType.FRIST_DATO_11_7.verdi}."
        )
        assertAntallJobber(referanse, 1)
    }

    @ParameterizedTest
    @EnumSource(
        ProsesseringStatus::class, names = ["STARTET", "INNHOLD_HENTET", "FAKTAGRUNNLAG_HENTET", "AVBRUTT"]
    )
    fun `ferdigstill med status før BREVBESTILLING_LØST eller AVBRUTT feiler`(status: ProsesseringStatus) {
        val referanse = gittBrevMed(brev = brev(), status = status)
        assertAntallJobber(referanse, 1)
        val exception = assertThrows<ValideringsfeilException> {
            ferdigstill(referanse)
        }
        assertThat(exception.message).endsWith(
            "Bestillingen er i feil status for ferdigstilling, prosesseringStatus=$status"
        )
        assertAntallJobber(referanse, 1)
    }

    @ParameterizedTest
    @EnumSource(
        ProsesseringStatus::class, mode = Mode.EXCLUDE, names = [
            "STARTET", "INNHOLD_HENTET", "FAKTAGRUNNLAG_HENTET", "BREVBESTILLING_LØST", "AVBRUTT"
        ]
    )
    fun `ferdigstill feiler ikke dersom status er etter BREVBESTILLING_LØST, men gjør ingen endring`(status: ProsesseringStatus) {
        val referanse = gittBrevMed(brev = brev(), status = status)
        assertAntallJobber(referanse, 1)
        ferdigstill(referanse)
        assertAntallJobber(referanse, 1)
    }

    private fun gittBrevMed(brev: Brev, status: ProsesseringStatus): BrevbestillingReferanse {
        return dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)
            val brevinnholdService = BrevinnholdService.konstruer(connection)

            val referanse = brevbestillingService.opprettBestilling(
                saksnummer = randomSaksnummer(),
                brukerIdent = randomBrukerIdent(),
                behandlingReferanse = randomBehandlingReferanse(),
                unikReferanse = randomUnikReferanse(),
                brevtype = Brevtype.INNVILGELSE,
                språk = Språk.NB,
                vedlegg = emptySet(),
            ).referanse

            brevinnholdService.hentOgLagre(referanse)
            brevbestillingRepository.oppdaterBrev(referanse, brev)
            brevbestillingRepository.oppdaterProsesseringStatus(referanse, status)

            referanse
        }
    }

    private fun ferdigstill(referanse: BrevbestillingReferanse) {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)

            brevbestillingService.ferdigstill(referanse, null)
        }
    }

    private fun assertAntallJobber(referanse: BrevbestillingReferanse, forventetAntall: Int) {
        return dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val bestilling = brevbestillingService.hent(referanse)

            val query = """
                SELECT count(*) as antall
                FROM JOBB
                WHERE sak_id = ?
            """.trimIndent()

            val antall = connection.queryFirst(query) {
                setParams {
                    setLong(1, bestilling.id.id)
                }
                setRowMapper { row -> row.getInt("antall") }
            }

            assertThat(antall).isEqualTo(forventetAntall)
        }
    }
}
