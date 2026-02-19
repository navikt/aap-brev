package no.nav.aap.brev.bestilling

import no.nav.aap.brev.IntegrationTest
import no.nav.aap.brev.feil.ValideringsfeilException
import no.nav.aap.brev.innhold.KjentFaktagrunnlag
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.FaktagrunnlagType
import no.nav.aap.brev.kontrakt.Rolle
import no.nav.aap.brev.kontrakt.SignaturGrunnlag
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.test.BrevmalBuilder
import no.nav.aap.brev.test.fakes.brev
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode
import kotlin.Boolean

class FerdigstillValideringTest : IntegrationTest() {

    @Test
    fun `ferdigstilling går igjennom dersom ingen valideringsfeil`() {
        val brevbestilling =
            gittBrevMed(
                brev = brev(medFaktagrunnlag = emptyList()),
                status = Status.UNDER_ARBEID,
            )
        assertAntallJobber(brevbestilling.referanse, 0)
        ferdigstill(brevbestilling.referanse)
        assertStatus(brevbestilling.referanse, Status.FERDIGSTILT)
        assertAntallJobber(brevbestilling.referanse, 1)

        dataSource.transaction { connection ->
            val mottakerRepository = MottakerRepositoryImpl(connection)
            val mottakere = mottakerRepository.hentMottakere(brevbestilling.id)
            assertThat(mottakere).hasSize(1)
            assertThat(mottakere).allSatisfy {
                assertThat(it.ident).isEqualTo(brevbestilling.brukerIdent)
                assertThat(it.identType).isEqualTo(IdentType.FNR)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(
        IdentType::class, mode = Mode.INCLUDE, names = ["FNR", "ORGNR"]
    )
    fun `ferdigstilling går igjennom uten at navn og adresse oppgis da dette slås opp av tjenester for journalføring og distribusjon`(
        identType: IdentType
    ) {
        val brevbestilling =
            gittBrevMed(
                brev = brev(),
                status = Status.UNDER_ARBEID,
            )

        assertAntallJobber(brevbestilling.referanse, 0)
        ferdigstill(
            referanse = brevbestilling.referanse,
            mottakere = listOf(
                Mottaker(
                    ident = "123",
                    identType = identType,
                    navnOgAdresse = null,
                    bestillingMottakerReferanse = "bestillingMottakerReferanse"
                )
            ),
        )
        assertStatus(brevbestilling.referanse, Status.FERDIGSTILT)
        assertAntallJobber(brevbestilling.referanse, 1)
    }

    @ParameterizedTest
    @EnumSource(
        IdentType::class, mode = Mode.EXCLUDE, names = ["FNR", "ORGNR"]
    )
    fun `ferdigstilling feiler dersom navn og adresse på mottaker ikke oppgis`(identType: IdentType) {
        val brevbestilling =
            gittBrevMed(
                brev = brev(),
                status = Status.UNDER_ARBEID,
            )

        val exception = assertThrows<IllegalArgumentException> {
            ferdigstill(
                referanse = brevbestilling.referanse,
                mottakere = listOf(
                    Mottaker(
                        ident = "123",
                        identType = identType,
                        navnOgAdresse = null,
                        bestillingMottakerReferanse = "bestillingMottakerReferanse"
                    )
                ),
            )
        }
        assertThat(exception.message).isEqualTo("navnOgAdresse må være satt dersom identType ikke er FNR eller ORGNR.")
    }

    @Test
    fun `ferdigstill feiler ikke dersom allerede ferdigstilt, men gjør ingen endring`() {
        val bestilling = gittBrevMed(
            brev = brev(),
            status = Status.UNDER_ARBEID,
        )

        val referanse = bestilling.referanse

        assertStatus(
            referanse,
            Status.UNDER_ARBEID
        )
        assertAntallJobber(referanse, 0)

        ferdigstill(
            referanse = referanse,
            signaturer = listOf(
                SignaturGrunnlag("ident", Rolle.SAKSBEHANDLER_OPPFOLGING, null)
            ),
            mottakere = listOf(
                Mottaker(
                    ident = bestilling.brukerIdent,
                    identType = IdentType.FNR,
                    bestillingMottakerReferanse = "ref"
                )
            )
        )

        val ferdigstiltBestilling = hentBestilling(referanse)
        val mottakere = hentMottakere(ferdigstiltBestilling.id)
        assertThat(ferdigstiltBestilling.status).isEqualTo(Status.FERDIGSTILT)
        assertThat(ferdigstiltBestilling.signaturer).hasSize(1)
        assertThat(mottakere).hasSize(1)
        assertAntallJobber(referanse, 1)

        ferdigstill(
            referanse = referanse,
            signaturer = listOf(
                SignaturGrunnlag("ident", Rolle.SAKSBEHANDLER_OPPFOLGING, null)
            ),
            mottakere = listOf(
                Mottaker(
                    ident = bestilling.brukerIdent,
                    identType = IdentType.FNR,
                    bestillingMottakerReferanse = "ref"
                )
            )
        )

        val bestillingEtterDuplikatFerdigstillKall = hentBestilling(referanse)
        assertThat(bestillingEtterDuplikatFerdigstillKall.status).isEqualTo(Status.FERDIGSTILT)
        assertThat(bestillingEtterDuplikatFerdigstillKall.signaturer).isEqualTo(ferdigstiltBestilling.signaturer)
        assertThat(hentMottakere(bestillingEtterDuplikatFerdigstillKall.id)).isEqualTo(mottakere)
        assertAntallJobber(referanse, 1)
    }

    @Test
    fun `ferdigstilling feiler dersom validering av brevet feiler v2`() {
        val referanse =
            gittBrevMed(
                brev = brev(medFaktagrunnlag = listOf(FaktagrunnlagType.FRIST_DATO_11_7.verdi)),
                status = Status.UNDER_ARBEID,
                brukV3 = false
            ).referanse
        assertAntallJobber(referanse, 0)
        val exception = assertThrows<ValideringsfeilException> {
            ferdigstill(referanse)
        }
        assertThat(exception.message).endsWith(
            "Brevet mangler utfylling av faktagrunnlag med teknisk navn: ${KjentFaktagrunnlag.FRIST_DATO_11_7.name}."
        )
        assertStatus(referanse, Status.UNDER_ARBEID)
        assertAntallJobber(referanse, 0)
    }

    @Test
    fun `ferdigstilling feiler dersom validering av brevet feiler v3`() {
        val referanse =
            gittBrevMed(
                brevmal = BrevmalBuilder.builder {
                    delmal {
                        obligatorisk = true
                        faktagrunnlag(FaktagrunnlagType.FRIST_DATO_11_7.verdi)
                    }
                },
                status = Status.UNDER_ARBEID,
                brukV3 = true
            ).referanse
        assertAntallJobber(referanse, 0)
        val exception = assertThrows<ValideringsfeilException> {
            ferdigstill(referanse)
        }
        assertThat(exception.message).contains(
            "Kan ikke ferdigstille brevbestilling med referanse=${referanse.referanse}. Validering av brevinnhold feilet"
        )
        assertStatus(referanse, Status.UNDER_ARBEID)
        assertAntallJobber(referanse, 0)
    }

    @ParameterizedTest
    @EnumSource(
        Status::class, mode = Mode.EXCLUDE, names = ["UNDER_ARBEID", "FERDIGSTILT"]
    )
    fun `ferdigstill med annen status enn UNDER_ARBEID og FERDIGSTILT feiler`(status: Status) {
        val referanse = gittBrevMed(
            brev = brev(),
            status = status,
        ).referanse
        assertAntallJobber(referanse, 0)
        val exception = assertThrows<ValideringsfeilException> {
            ferdigstill(referanse)
        }
        assertThat(exception.message).endsWith(
            "Bestillingen er i feil status for ferdigstilling, status=$status"
        )

        assertStatus(referanse, status)
        assertAntallJobber(referanse, 0)
    }

    private fun hentBestilling(referanse: BrevbestillingReferanse): Brevbestilling {
        return dataSource.transaction { connection ->
            BrevbestillingRepositoryImpl(connection).hent(referanse)
        }
    }

    private fun hentMottakere(id: BrevbestillingId): List<Mottaker> {
        return dataSource.transaction { connection ->
            MottakerRepositoryImpl(connection).hentMottakere(id)
        }
    }

    private fun gittBrevMed(
        status: Status,
        brev: Brev? = null, // fjernes og tilpass resterende tester når v2 fjernes
        brevmal: Brevmal? = null,
        brukV3: Boolean = false,
    ): Brevbestilling {
        return dataSource.transaction { connection ->
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)
            val bestilling =
                opprettBrevbestilling(
                    brukV3 = brukV3,
                    brevtype = Brevtype.INNVILGELSE,
                    språk = Språk.NB,
                    faktagrunnlag = emptySet(),
                    vedlegg = emptySet(),
                    ferdigstillAutomatisk = false,
                ).brevbestilling

            if (brev != null) {
                brevbestillingRepository.oppdaterBrev(bestilling.referanse, brev)
            }
            if (brevmal != null) {
                brevbestillingRepository.oppdaterBrevmal(
                    bestilling.id,
                    DefaultJsonMapper.fromJson(DefaultJsonMapper.toJson(brevmal))
                )
            }
            brevbestillingRepository.oppdaterStatus(bestilling.id, status)

            bestilling
        }
    }

    private fun assertStatus(referanse: BrevbestillingReferanse, status: Status) {
        dataSource.transaction { connection ->
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)
            val bestilling = brevbestillingRepository.hent(referanse)
            assertThat(bestilling.status).isEqualTo(status)
        }
    }

    private fun ferdigstill(
        referanse: BrevbestillingReferanse,
        signaturer: List<SignaturGrunnlag> = emptyList(),
        mottakere: List<Mottaker> = emptyList()
    ) {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)

            brevbestillingService.ferdigstill(referanse, signaturer, mottakere)
        }
    }
}
