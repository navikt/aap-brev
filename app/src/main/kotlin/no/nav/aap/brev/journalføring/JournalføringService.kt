package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.PdfBrev
import no.nav.aap.brev.bestilling.PdfBrev.Blokk
import no.nav.aap.brev.bestilling.PdfBrev.FormattertTekst
import no.nav.aap.brev.bestilling.PdfBrev.Innhold
import no.nav.aap.brev.bestilling.PdfBrev.Mottaker
import no.nav.aap.brev.bestilling.PdfBrev.Mottaker.IdentType
import no.nav.aap.brev.bestilling.PdfBrev.Tekstbolk
import no.nav.aap.brev.bestilling.PdfGateway
import no.nav.aap.brev.bestilling.PersoninfoGateway
import no.nav.aap.brev.bestilling.SaksbehandlingPdfGenGateway
import no.nav.aap.brev.bestilling.Saksnummer
import no.nav.aap.brev.journalføring.JournalføringData.MottakerType
import no.nav.aap.brev.kontrakt.BlokkInnhold
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Signatur
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.person.PdlGateway
import no.nav.aap.brev.util.formaterFullLengde
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.LocalDate

class JournalføringService(
    private val signaturService: SignaturService,
    private val brevbestillingRepository: BrevbestillingRepository,
    private val personinfoGateway: PersoninfoGateway,
    private val pdfGateway: PdfGateway,
    private val journalføringGateway: JournalføringGateway,
) {

    companion object {
        fun konstruer(connection: DBConnection): JournalføringService {
            return JournalføringService(
                signaturService = SignaturService.konstruer(),
                brevbestillingRepository = BrevbestillingRepositoryImpl(connection),
                personinfoGateway = PdlGateway(),
                pdfGateway = SaksbehandlingPdfGenGateway(),
                journalføringGateway = DokarkivGateway(),
            )
        }
    }

    fun journalførBrevbestilling(referanse: BrevbestillingReferanse) {
        val bestilling = brevbestillingRepository.hent(referanse)

        checkNotNull(bestilling.brev) {
            "Kan ikke journalføre bestilling uten brev."
        }
        check(bestilling.journalpostId == null) {
            "Kan ikke journalføre brev for bestilling som allerede er journalført."
        }
        check(bestilling.brukerIdent != null) {
            "Kan ikke journalføre brev for bestilling der brukerIdent er null"
        }

        val personinfo = personinfoGateway.hentPersoninfo(bestilling.brukerIdent)

        val signaturer: List<Signatur> =
            signaturService.signaturer(bestilling.signaturer, bestilling.brevtype, personinfo)
        val pdfBrev = mapPdfBrev(
            brukerIdent = personinfo.personIdent,
            navn = personinfo.navn,
            saksnummer = bestilling.saksnummer,
            brev = bestilling.brev,
            dato = LocalDate.now(),
            språk = bestilling.språk,
            signaturer = signaturer,
        )
        val pdf = pdfGateway.genererPdf(pdfBrev)

        val journalføringData = JournalføringData(
            brukerFnr = personinfo.personIdent,
            mottakerIdent = personinfo.personIdent,
            mottakerType = MottakerType.FNR,
            mottakerNavn = null,
            saksnummer = bestilling.saksnummer,
            eksternReferanseId = bestilling.referanse.referanse,
            tittelJournalpost = checkNotNull(value = bestilling.brev.journalpostTittel ?: bestilling.brev.overskrift),
            tittelBrev = checkNotNull(value = bestilling.brev.overskrift),
            brevkode = bestilling.brevtype.name,
            overstyrInnsynsregel = false,
        )

        val forsøkFerdigstill = ferdigstillVedOpprettelseAvJournalpost(bestilling)
        val response = journalføringGateway.journalførBrev(
            journalføringData = journalføringData,
            pdf = pdf,
            forsøkFerdigstill = forsøkFerdigstill,
        )
        brevbestillingRepository.lagreJournalpost(
            bestilling.id,
            response.journalpostId,
            response.journalpostferdigstilt
        )
    }

    fun tilknyttVedlegg(referanse: BrevbestillingReferanse) {
        val bestilling = brevbestillingRepository.hent(referanse)
        val journalpostId = checkNotNull(bestilling.journalpostId)
        val vedlegg = bestilling.vedlegg
        if (vedlegg.isNotEmpty()) {
            journalføringGateway.tilknyttVedlegg(journalpostId, vedlegg)
        }
    }

    fun ferdigstillJournalpost(referanse: BrevbestillingReferanse) {
        val bestilling = brevbestillingRepository.hent(referanse)
        val journalpostId = checkNotNull(bestilling.journalpostId)
        if (bestilling.journalpostFerdigstilt != true) {
            journalføringGateway.ferdigstillJournalpost(journalpostId)
        }
    }

    private fun ferdigstillVedOpprettelseAvJournalpost(bestilling: Brevbestilling): Boolean {
        return bestilling.vedlegg.isEmpty()
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
