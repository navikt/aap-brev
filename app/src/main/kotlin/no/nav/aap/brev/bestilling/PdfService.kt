package no.nav.aap.brev.bestilling

import no.nav.aap.brev.bestilling.PdfBrev.Blokk
import no.nav.aap.brev.bestilling.PdfBrev.FormattertTekst
import no.nav.aap.brev.bestilling.PdfBrev.Innhold
import no.nav.aap.brev.bestilling.PdfBrev.Mottaker
import no.nav.aap.brev.bestilling.PdfBrev.Mottaker.IdentType
import no.nav.aap.brev.bestilling.PdfBrev.Tekstbolk
import no.nav.aap.brev.innhold.BrevSanityProxyGateway
import no.nav.aap.brev.journalføring.SignaturService
import no.nav.aap.brev.kontrakt.BlokkInnhold
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Signatur
import no.nav.aap.brev.kontrakt.SignaturGrunnlag
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.person.PdlGateway
import no.nav.aap.brev.util.TimeUtils.formaterFullLengde
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.LocalDate

class PdfService(
    private val signaturService: SignaturService,
    private val brevbestillingRepository: BrevbestillingRepository,
    private val personinfoGateway: PersoninfoGateway,
    private val pdfGateway: PdfGateway,
    private val pdfGatewayV2: PdfGatewayV2,
) {

    companion object {
        fun konstruer(connection: DBConnection): PdfService {
            return PdfService(
                signaturService = SignaturService.konstruer(),
                brevbestillingRepository = BrevbestillingRepositoryImpl(connection),
                personinfoGateway = PdlGateway(),
                pdfGateway = SaksbehandlingPdfGenGateway(),
                pdfGatewayV2 = BrevSanityProxyGateway()
            )
        }
    }

    fun genererPdfForForhåndsvisning(referanse: BrevbestillingReferanse, signaturer: List<SignaturGrunnlag>): Pdf {
        val bestilling = brevbestillingRepository.hent(referanse)
        check(bestilling.brukerIdent != null) {
            "Kan ikke generere pdf for bestilling der brukerIdent er null"
        }
        val personinfo = personinfoGateway.hentPersoninfo(bestilling.brukerIdent)

        return genererPdf(bestilling, personinfo, signaturer.tilSorterbareSignaturer())
    }

    fun genererPdfForJournalføring(bestilling: Brevbestilling, personinfo: Personinfo): Pdf {
        return genererPdf(bestilling, personinfo, bestilling.signaturer)
    }

    private fun genererPdf(
        bestilling: Brevbestilling,
        personinfo: Personinfo,
        sorterbareSignaturer: List<SorterbarSignatur>
    ): Pdf {
        val signaturer: List<Signatur> =
            signaturService.signaturer(sorterbareSignaturer, bestilling.brevtype, personinfo)

        if (bestilling.erBestillingMedBrevmal()) {
            checkNotNull(bestilling.brevmal) {
                "Kan ikke generere pdf av brevbestilling uten brevmal."
            }
            checkNotNull(bestilling.brevdata) {
                "Kan ikke generere pdf av brevbestilling uten brevdata."
            }

            val request = GenererPdfRequest(
                brukerIdent = personinfo.personIdent,
                navn = personinfo.navn,
                saksnummer = bestilling.saksnummer,
                brevmal = bestilling.brevmal,
                brevdata = bestilling.brevdata,
                dato = LocalDate.now(),
                språk = bestilling.språk,
                signaturer = signaturer,
            )

            return pdfGatewayV2.genererPdf(request)
        } else {
            checkNotNull(bestilling.brev) {
                "Kan ikke generere pdf av brevbestilling uten brev."
            }

            val pdfBrev = mapPdfBrev(
                brukerIdent = personinfo.personIdent,
                navn = personinfo.navn,
                saksnummer = bestilling.saksnummer,
                brev = bestilling.brev,
                dato = LocalDate.now(),
                språk = bestilling.språk,
                signaturer = signaturer,
            )

            return pdfGateway.genererPdf(pdfBrev)
        }
    }

    private fun mapPdfBrev(
        brukerIdent: String,
        navn: String,
        saksnummer: Saksnummer,
        brev: Brev,
        dato: LocalDate,
        språk: Språk,
        signaturer: List<Signatur>,
    ): PdfBrev {
        return PdfBrev(
            mottaker = Mottaker(
                navn = navn,
                ident = brukerIdent,
                identType = IdentType.FNR
            ),
            saksnummer = saksnummer.nummer,
            dato = dato.formaterFullLengde(språk),
            overskrift = brev.overskrift,
            tekstbolker = brev.tekstbolker.map {
                Tekstbolk(
                    overskrift = it.overskrift,
                    innhold = it.innhold.map {
                        Innhold(
                            overskrift = it.overskrift,
                            blokker = it.blokker.map {
                                Blokk(
                                    innhold = it.innhold.mapNotNull {
                                        when (it) {
                                            is BlokkInnhold.FormattertTekst -> FormattertTekst(
                                                tekst = it.tekst,
                                                formattering = it.formattering
                                            )

                                            is BlokkInnhold.Faktagrunnlag -> {
                                                throw IllegalStateException("Kan ikke lage PDF av brev med manglende faktagrunnlag ${it.tekniskNavn}.")
                                            }
                                        }
                                    },
                                    type = it.type
                                )
                            })
                    })
            },
            signaturer = signaturer
        )
    }
}
