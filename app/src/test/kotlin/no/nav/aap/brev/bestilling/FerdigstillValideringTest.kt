package no.nav.aap.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.FaktagrunnlagType
import no.nav.aap.brev.exception.ValideringsfeilException
import no.nav.aap.brev.innhold.BrevinnholdService
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.kontrakt.Status
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

        private val dataSource = InitTestDatabase.freshDatabase()

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            Fakes.start()
        }
    }

    @Test
    fun `ferdigstilling går igjennom dersom ingen valideringsfeil`() {
        val referanse =
            gittBrevMed(
                brev = brev(medFaktagrunnlag = emptyList()),
                status = Status.UNDER_ARBEID,
                prosesseringStatus = ProsesseringStatus.BREVBESTILLING_LØST
            )
        assertAntallJobber(referanse, 1)
        ferdigstill(referanse)
        assertStatus(referanse, Status.FERDIGSTILT)
        assertAntallJobber(referanse, 2)
    }

    @Test
    fun `ferdigstilling feiler dersom brevet har faktagrunnlag`() {
        val referanse =
            gittBrevMed(
                brev = brev(medFaktagrunnlag = listOf(FaktagrunnlagType.FRIST_DATO_11_7.verdi)),
                status = Status.UNDER_ARBEID,
                prosesseringStatus = ProsesseringStatus.BREVBESTILLING_LØST
            )
        assertAntallJobber(referanse, 1)
        val exception = assertThrows<ValideringsfeilException> {
            ferdigstill(referanse)
        }
        assertThat(exception.message).endsWith(
            "Brevet mangler utfylling av faktagrunnlag med teknisk navn: ${FaktagrunnlagType.FRIST_DATO_11_7.verdi}."
        )
        assertStatus(referanse, Status.UNDER_ARBEID)
        assertAntallJobber(referanse, 1)
    }

    @ParameterizedTest
    @EnumSource(
        ProsesseringStatus::class, names = ["STARTET", "INNHOLD_HENTET", "FAKTAGRUNNLAG_HENTET", "AVBRUTT"]
    )
    fun `ferdigstill med status før BREVBESTILLING_LØST eller med status AVBRUTT feiler`(status: ProsesseringStatus) {
        val referanse = gittBrevMed(
            brev = brev(),
            status = Status.UNDER_ARBEID,
            prosesseringStatus = status
        )
        assertAntallJobber(referanse, 1)
        val exception = assertThrows<ValideringsfeilException> {
            ferdigstill(referanse)
        }
        assertThat(exception.message).endsWith(
            "Bestillingen er i feil status for ferdigstilling, prosesseringStatus=$status"
        )
        assertStatus(referanse, Status.UNDER_ARBEID)
        assertAntallJobber(referanse, 1)
    }

    @ParameterizedTest
    @EnumSource(
        ProsesseringStatus::class, mode = Mode.EXCLUDE, names = [
            "STARTET", "INNHOLD_HENTET", "FAKTAGRUNNLAG_HENTET", "BREVBESTILLING_LØST", "AVBRUTT"
        ]
    )
    fun `ferdigstill feiler ikke dersom status er etter BREVBESTILLING_LØST, men gjør ingen endring`(status: ProsesseringStatus) {
        val referanse = gittBrevMed(
            brev = brev(),
            status = Status.FERDIGSTILT,
            prosesseringStatus = status
        )
        assertAntallJobber(referanse, 1)
        ferdigstill(referanse)
        assertStatus(referanse, Status.FERDIGSTILT)
        assertAntallJobber(referanse, 1)
    }

    private fun gittBrevMed(
        brev: Brev,
        status: Status,
        prosesseringStatus: ProsesseringStatus
    ): BrevbestillingReferanse {
        return dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)
            val brevinnholdService = BrevinnholdService.konstruer(connection)

            val bestilling = brevbestillingService.opprettBestillingV1(
                saksnummer = randomSaksnummer(),
                brukerIdent = randomBrukerIdent(),
                behandlingReferanse = randomBehandlingReferanse(),
                unikReferanse = randomUnikReferanse(),
                brevtype = Brevtype.INNVILGELSE,
                språk = Språk.NB,
                vedlegg = emptySet(),
            ).brevbestilling

            brevinnholdService.hentOgLagre(bestilling.referanse)
            brevbestillingRepository.oppdaterBrev(bestilling.referanse, brev)
            brevbestillingRepository.oppdaterProsesseringStatus(bestilling.referanse, prosesseringStatus)
            brevbestillingRepository.oppdaterStatus(bestilling.id, status)

            bestilling.referanse
        }
    }

    private fun assertStatus(referanse: BrevbestillingReferanse, status: Status) {
        dataSource.transaction { connection ->
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)
            val bestilling = brevbestillingRepository.hent(referanse)
            assertThat(bestilling.status).isEqualTo(status)
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
