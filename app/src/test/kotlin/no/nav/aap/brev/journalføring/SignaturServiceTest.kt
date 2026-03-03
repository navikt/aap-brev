package no.nav.aap.brev.journalføring

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.brev.bestilling.Personinfo
import no.nav.aap.brev.bestilling.SorterbarSignatur
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Rolle
import no.nav.aap.brev.kontrakt.Signatur
import no.nav.aap.brev.organisasjon.AnsattInfo
import no.nav.aap.brev.organisasjon.Enhet
import no.nav.aap.brev.organisasjon.EnhetsType
import no.nav.aap.brev.organisasjon.NomInfoGateway
import no.nav.aap.brev.organisasjon.NorgGateway
import no.nav.aap.brev.test.randomBrukerIdent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class SignaturServiceTest {

    private val ansattInfoGateway = mockk<NomInfoGateway>()
    private val enhetGateway = mockk<NorgGateway>()
    private val signaturService = SignaturService(ansattInfoGateway, enhetGateway)

    @Test
    fun `signaturer vises ikke ved strengt fortrolig adresse`() {
        val signaturer = signaturService.signaturer(
            listOf(SorterbarSignatur("navident", 0, Rolle.BESLUTTER, "1234")),
            Brevtype.INNVILGELSE,
            Personinfo(personIdent = randomBrukerIdent(), navn = "navn", harStrengtFortroligAdresse = true)
        )
        assertThat(signaturer).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `signaturer sorteres basert på angitt sorteringsnøkkel`(harEnhetISignatur: Boolean) {

        every { ansattInfoGateway.hentAnsattInfo("navident1") } returns AnsattInfo(
            navIdent = "navident1",
            navn = "Ansattnavn1",
            enhetsnummer = "1234",
        )
        every { ansattInfoGateway.hentAnsattInfo("navident2") } returns AnsattInfo(
            navIdent = "navident2",
            navn = "Ansattnavn2",
            enhetsnummer = "1234",
        )
        every { ansattInfoGateway.hentAnsattInfo("navident3") } returns AnsattInfo(
            navIdent = "navident3",
            navn = "Ansattnavn3",
            enhetsnummer = "1234",
        )
        every { enhetGateway.hentOverordnetFylkesenhet(any()) } returns Enhet(
            "4321",
            "Overordnet enhet",
            EnhetsType.FYLKE
        )
        every { enhetGateway.hentEnheter(any()) } returns listOf(Enhet("1234", "Enhet", EnhetsType.LOKAL))

        val enhet = if (harEnhetISignatur) "1234" else null
        val signaturer = signaturService.signaturer(
            listOf(
                SorterbarSignatur("navident2", 1, null, enhet),
                SorterbarSignatur("navident1", 0, null, enhet),
                SorterbarSignatur("navident3", 2, null, enhet)
            ),
            Brevtype.INNVILGELSE,
            personinfo = Personinfo(
                personIdent = randomBrukerIdent(),
                navn = "navn",
                harStrengtFortroligAdresse = false
            )
        )
        assertThat(signaturer).isEqualTo(
            listOf(
                Signatur(navn = "Ansattnavn1", enhet = "Enhet"),
                Signatur(navn = "Ansattnavn2", enhet = "Enhet"),
                Signatur(navn = "Ansattnavn3", enhet = "Enhet")
            )
        )
    }

    @Test
    fun `kvalitetssikrer signerer med navn på overordnet fylkesenhet`() {

        every { ansattInfoGateway.hentAnsattInfo(any()) } returns AnsattInfo(
            navIdent = "navident",
            navn = "Ansattnavn",
            enhetsnummer = "1234",
        )
        every { enhetGateway.hentOverordnetFylkesenhet(any()) } returns Enhet(
            "4321",
            "Overordnet enhet",
            EnhetsType.FYLKE
        )
        every { enhetGateway.hentEnheter(any()) } returns listOf(Enhet("1234", "Enhet", EnhetsType.LOKAL))

        val signaturer = signaturService.signaturer(
            sorterbareSignaturer = listOf(SorterbarSignatur("navident", 1, Rolle.KVALITETSSIKRER, null)),
            brevtype = Brevtype.INNVILGELSE,
            personinfo = Personinfo(
                personIdent = randomBrukerIdent(),
                navn = "navn",
                harStrengtFortroligAdresse = false
            )
        )
        assertThat(signaturer).isEqualTo(listOf(Signatur(navn = "Ansattnavn", enhet = "Overordnet enhet")))
    }

    @Test
    fun `bruker enhet fra signatur dersom definert med unntak for NAY AAP enhet, ellers ansatt-enhet`() {
        every { ansattInfoGateway.hentAnsattInfo("navident1") } returns AnsattInfo(
            navIdent = "navident1",
            navn = "Ansattnavn1",
            enhetsnummer = "4567",
        )

        every { ansattInfoGateway.hentAnsattInfo("navident2") } returns AnsattInfo(
            navIdent = "navident2",
            navn = "Ansattnavn2",
            enhetsnummer = "5678",
        )

        every { ansattInfoGateway.hentAnsattInfo("navident3") } returns AnsattInfo(
            navIdent = "navident3",
            navn = "Ansattnavn3",
            enhetsnummer = "6789",
        )

        every { ansattInfoGateway.hentAnsattInfo("navident4") } returns AnsattInfo(
            navIdent = "navident4",
            navn = "Ansattnavn4",
            enhetsnummer = "7890",
        )

        every { enhetGateway.hentEnheter(any()) } returns listOf(
            Enhet("4491", "Enhetsnavn 4491", EnhetsType.ANNET),
            Enhet("1234", "Enhetsnavn 1234", EnhetsType.LOKAL),
            Enhet("2345", "Enhetsnavn 2345", EnhetsType.LOKAL),
            Enhet("4567", "Enhetsnavn 4567", EnhetsType.LOKAL),
            Enhet("5678", "Enhetsnavn 5678", EnhetsType.LOKAL),
            Enhet("6789", "Enhetsnavn 6789", EnhetsType.LOKAL),
            Enhet("7890", "Enhetsnavn 7890", EnhetsType.LOKAL),
        )

        val signaturer = signaturService.signaturer(
            sorterbareSignaturer = listOf(
                SorterbarSignatur("navident1", 1, null, "4491"),
                SorterbarSignatur("navident2", 2, null, "1234"),
                SorterbarSignatur("navident3", 3, null, "2345"),
                SorterbarSignatur("navident4", 4, null, null),
            ),
            brevtype = Brevtype.INNVILGELSE,
            personinfo = Personinfo(
                personIdent = randomBrukerIdent(),
                navn = "navn",
                harStrengtFortroligAdresse = false
            )
        )
        assertThat(signaturer).isEqualTo(listOf(
            Signatur(navn = "Ansattnavn1", enhet = "Enhetsnavn 4567"),
            Signatur(navn = "Ansattnavn2", enhet = "Enhetsnavn 1234"),
            Signatur(navn = "Ansattnavn3", enhet = "Enhetsnavn 2345"),
            Signatur(navn = "Ansattnavn4", enhet = "Enhetsnavn 7890"),
        ))
    }
}
