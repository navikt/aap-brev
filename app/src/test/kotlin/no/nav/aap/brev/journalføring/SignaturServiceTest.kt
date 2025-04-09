package no.nav.aap.brev.journalføring

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.nav.aap.brev.bestilling.Personinfo
import no.nav.aap.brev.bestilling.SorterbarSignatur
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Rolle
import no.nav.aap.brev.kontrakt.Signatur
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.organisasjon.AnsattInfo
import no.nav.aap.brev.organisasjon.AnsattInfoGateway
import no.nav.aap.brev.organisasjon.Enhet
import no.nav.aap.brev.organisasjon.EnhetsType
import no.nav.aap.brev.organisasjon.NomInfoGateway
import no.nav.aap.brev.organisasjon.NorgGateway
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SignaturServiceTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            Fakes.start()
        }
    }

    @Test
    fun `signaturer vises ikke ved strengt fortrolig adresse`() {
        val signaturService = SignaturService.konstruer()


        val signaturer = signaturService.signaturer(
            listOf(SorterbarSignatur("navident", 0, Rolle.BESLUTTER)),
            Brevtype.INNVILGELSE,
            personInfoMedStrengtFortroligAdresse
        )
        assertThat(signaturer).isEmpty()
    }

    @Test
    fun `signaturer sorteres basert på angitt sorteringsnøkkel`() {

        val ansattInfoGateway = spyk<AnsattInfoGateway> {
            every { hentAnsattInfo("navident0") } returns AnsattInfo(
                navIdent = "navident0",
                navn = "Ansattnavn0",
                enhetsnummer = "1234",
            )
            every { hentAnsattInfo("navident1") } returns AnsattInfo(
                navIdent = "navident1",
                navn = "Ansattnavn1",
                enhetsnummer = "1234",
            )
            every { hentAnsattInfo("navident2") } returns AnsattInfo(
                navIdent = "navident2",
                navn = "Ansattnavn2",
                enhetsnummer = "1234",
            )
        }
        val enhetGateway = mockk<NorgGateway> {
            every { hentOverordnetFylkesenhet(any()) } returns Enhet("4321", "Overordnet enhet", EnhetsType.FYLKE)
            every { hentEnheter(any()) } returns listOf(Enhet("1234", "Enhet", EnhetsType.LOKAL))
        }
        val signaturService = SignaturService(ansattInfoGateway, enhetGateway)

        val signaturer = signaturService.signaturer(
            listOf(
                SorterbarSignatur("navident1", 1, null),
                SorterbarSignatur("navident0", 0, null),
                SorterbarSignatur("navident2", 2, null)
            ),
            Brevtype.INNVILGELSE,
            personInfo
        )
        assertThat(signaturer).isEqualTo(
            listOf(
                Signatur(navn = "Ansattnavn0", enhet = "Enhet"),
                Signatur(navn = "Ansattnavn1", enhet = "Enhet"),
                Signatur(navn = "Ansattnavn2", enhet = "Enhet")
            )
        )
    }

    @Test
    fun `kvalitetssikrer signerer med navn på overordnet fylkesenhet`() {

        val ansattInfoGateway = mockk<NomInfoGateway> {
            every { hentAnsattInfo(any()) } returns AnsattInfo(
                navIdent = "navident",
                navn = "Ansattnavn",
                enhetsnummer = "1234",
            )
        }
        val enhetGateway = mockk<NorgGateway> {
            every { hentOverordnetFylkesenhet(any()) } returns Enhet("4321", "Overordnet enhet", EnhetsType.FYLKE)
            every { hentEnheter(any()) } returns listOf(Enhet("1234", "Enhet", EnhetsType.LOKAL))
        }

        val signaturService = SignaturService(ansattInfoGateway, enhetGateway)

        val signaturer = signaturService.signaturer(
            listOf(SorterbarSignatur("navident", 1, Rolle.KVALITETSSIKRER)),
            Brevtype.INNVILGELSE,
            personInfo
        )
        assertThat(signaturer).isEqualTo(listOf(Signatur(navn = "Ansattnavn", enhet = "Overordnet enhet")))
    }


    private val personInfoMedStrengtFortroligAdresse = Personinfo("ident", "navn", true)
    private val personInfo = Personinfo("ident", "navn", false)

}