package no.nav.aap.brev.bestilling

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.aap.brev.journalføring.SignaturService
import no.nav.aap.brev.kontrakt.Blokk
import no.nav.aap.brev.kontrakt.BlokkInnhold
import no.nav.aap.brev.kontrakt.BlokkType
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Innhold
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.kontrakt.Tekstbolk
import no.nav.aap.brev.test.fakes.FakeUnleashGateway
import no.nav.aap.brev.test.randomBrukerIdent
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class PdfServiceTest {

    private val pdfGateway = mockk<PdfGateway>()
    private val pdfBrevSlot = slot<PdfBrev>()

    private val pdfService = PdfService(
        signaturService = SignaturService(mockk(), mockk()),
        brevbestillingRepository = mockk(),
        personinfoGateway = mockk(),
        pdfGateway = pdfGateway,
        pdfgeneratorSaksbehandlingGateway = mockk(),
        pdfGatewayV2 = mockk(),
        unleashGateway = FakeUnleashGateway(),
    )

    private val personinfo = Personinfo(
        personIdent = randomBrukerIdent(),
        navn = "Test Testesen",
        harStrengtFortroligAdresse = false,
    )

    init {
        every { pdfGateway.genererPdf(capture(pdfBrevSlot)) } returns Pdf(byteArrayOf())
    }

    @Test
    fun `avsnitt med tom tekst fjernes`() {
        val blokker = genererOgHentBlokker(avsnitt(tekst("")))

        assertThat(blokker).isEmpty()
    }

    @Test
    fun `avsnitt med bare mellomrom og linjeskift fjernes`() {
        val blokker = genererOgHentBlokker(
            avsnitt(tekst("   ")),
            avsnitt(tekst("\n\t")),
        )

        assertThat(blokker).isEmpty()
    }

    @Test
    fun `avsnitt uten innhold fjernes`() {
        val blokker = genererOgHentBlokker(avsnitt())

        assertThat(blokker).isEmpty()
    }

    @Test
    fun `avsnitt med flere tomme tekster fjernes`() {
        val blokker = genererOgHentBlokker(avsnitt(tekst(""), tekst(" "), tekst("\n")))

        assertThat(blokker).isEmpty()
    }

    @Test
    fun `avsnitt med både tom og ikke-tom tekst beholdes med all tekst`() {
        val blokker = genererOgHentBlokker(avsnitt(tekst(" "), tekst("Hei")))

        assertThat(blokker).hasSize(1)
        assertThat(blokker.single().type).isEqualTo(BlokkType.AVSNITT)
        assertThat(blokker.single().innhold.map { it.tekst }).containsExactly(" ", "Hei")
    }

    @Test
    fun `liste med tom tekst eller uten innhold beholdes`() {
        val blokker = genererOgHentBlokker(
            liste(tekst("")),
            liste(),
        )

        assertThat(blokker).hasSize(2)
        assertThat(blokker.map { it.type }).containsOnly(BlokkType.LISTE)
    }

    @Test
    fun `rekkefølgen på gjenværende blokker beholdes`() {
        val blokker = genererOgHentBlokker(
            avsnitt(tekst("A")),
            avsnitt(tekst("")),
            avsnitt(tekst("B")),
        )

        assertThat(blokker.map { it.innhold.single().tekst }).containsExactly("A", "B")
    }

    @Test
    fun `avsnitt med bare faktagrunnlag fjernes ikke og gir feil`() {
        val bestilling = bestillingMed(
            avsnitt(BlokkInnhold.Faktagrunnlag(UUID.randomUUID(), "Visningsnavn", "TEKNISK_NAVN"))
        )

        assertThatThrownBy { pdfService.genererPdfForJournalføring(bestilling, personinfo) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("TEKNISK_NAVN")
    }

    @Test
    fun `overskrifter beholdes selv om alle blokker fjernes`() {
        val bestilling = bestillingMed(
            avsnitt(tekst("")),
            tekstbolkOverskrift = "Tekstbolk",
            innholdOverskrift = "Innhold",
        )

        pdfService.genererPdfForJournalføring(bestilling, personinfo)

        val tekstbolk = pdfBrevSlot.captured.tekstbolker.single()
        assertThat(tekstbolk.overskrift).isEqualTo("Tekstbolk")
        val innhold = tekstbolk.innhold.single()
        assertThat(innhold.overskrift).isEqualTo("Innhold")
        assertThat(innhold.blokker).isEmpty()
    }

    private fun genererOgHentBlokker(vararg blokker: Blokk): List<PdfBrev.Blokk> {
        pdfService.genererPdfForJournalføring(bestillingMed(*blokker), personinfo)
        return pdfBrevSlot.captured.tekstbolker.single().innhold.single().blokker
    }

    private fun tekst(tekst: String): BlokkInnhold.FormattertTekst =
        BlokkInnhold.FormattertTekst(id = UUID.randomUUID(), tekst = tekst, formattering = emptyList())

    private fun avsnitt(vararg innhold: BlokkInnhold): Blokk =
        Blokk(id = UUID.randomUUID(), innhold = innhold.toList(), type = BlokkType.AVSNITT)

    private fun liste(vararg innhold: BlokkInnhold): Blokk =
        Blokk(id = UUID.randomUUID(), innhold = innhold.toList(), type = BlokkType.LISTE)

    private fun bestillingMed(
        vararg blokker: Blokk,
        tekstbolkOverskrift: String? = null,
        innholdOverskrift: String? = null,
    ): Brevbestilling {
        val brev = Brev(
            kanSendesAutomatisk = false,
            overskrift = "Overskrift",
            kanOverstyreBrevtittel = false,
            journalpostTittel = null,
            tekstbolker = listOf(
                Tekstbolk(
                    id = UUID.randomUUID(),
                    overskrift = tekstbolkOverskrift,
                    innhold = listOf(
                        Innhold(
                            id = UUID.randomUUID(),
                            overskrift = innholdOverskrift,
                            blokker = blokker.toList(),
                            kanRedigeres = true,
                            erFullstendig = true,
                        )
                    ),
                )
            ),
        )
        return Brevbestilling(
            id = BrevbestillingId(1),
            saksnummer = Saksnummer("123"),
            referanse = BrevbestillingReferanse(UUID.randomUUID()),
            brev = brev,
            brevmal = null,
            brevdata = null,
            brukerIdent = personinfo.personIdent,
            signaturer = emptyList(),
            opprettet = LocalDateTime.now(),
            oppdatert = LocalDateTime.now(),
            behandlingReferanse = BehandlingReferanse(UUID.randomUUID()),
            unikReferanse = UnikReferanse(UUID.randomUUID().toString()),
            brevtype = Brevtype.INNVILGELSE,
            språk = Språk.NB,
            status = null,
            prosesseringStatus = null,
            vedlegg = emptySet(),
        )
    }
}
