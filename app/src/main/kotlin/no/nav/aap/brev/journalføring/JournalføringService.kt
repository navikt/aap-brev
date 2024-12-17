package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.BehandlingsflytGateway
import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.PdfGateway
import no.nav.aap.brev.bestilling.Personinfo
import no.nav.aap.brev.bestilling.PersoninfoGateway
import no.nav.aap.brev.bestilling.SaksbehandlingPdfGenGateway
import no.nav.aap.brev.bestilling.Saksnummer
import no.nav.aap.brev.journalføring.JournalpostInfo.MottakerType
import no.nav.aap.brev.kontrakt.BlokkInnhold
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.PdfBrev
import no.nav.aap.brev.kontrakt.PdfBrev.Blokk
import no.nav.aap.brev.kontrakt.PdfBrev.FormattertTekst
import no.nav.aap.brev.kontrakt.PdfBrev.Innhold
import no.nav.aap.brev.kontrakt.PdfBrev.Mottaker
import no.nav.aap.brev.kontrakt.PdfBrev.Tekstbolk
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import java.time.LocalDate

class JournalføringService(
    private val brevbestillingRepository: BrevbestillingRepository,
    private val personinfoGateway: PersoninfoGateway,
    private val pdfGateway: PdfGateway,
    private val arkivGateway: ArkivGateway,
) {

    companion object {
        fun konstruer(connection: DBConnection): JournalføringService {
            return JournalføringService(
                BrevbestillingRepositoryImpl(connection),
                BehandlingsflytGateway(),
                SaksbehandlingPdfGenGateway(),
                DokarkivGateway(),
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

        val personinfo = personinfoGateway.hentPersoninfo(bestilling.saksnummer)

        val pdfBrev = mapPdfBrev(personinfo, bestilling.saksnummer, bestilling.brev, LocalDate.now())
        val pdf = pdfGateway.genererPdf(pdfBrev)

        val journalpostInfo = JournalpostInfo(
            brukerFnr = personinfo.fnr,
            mottakerIdent = personinfo.fnr,
            mottakerType = MottakerType.FNR,
            mottakerNavn = null,
            saksnummer = bestilling.saksnummer,
            eksternReferanseId = bestilling.referanse.referanse,
            tittel = checkNotNull(value = bestilling.brev.overskrift),
            brevkode = bestilling.brevtype.name,
            overstyrInnsynsregel = false,
        )

        val forsøkFerdigstill = ferdigstillVedOpprettelseAvJournalpost(bestilling)
        val response = arkivGateway.journalførBrev(
            journalpostInfo = journalpostInfo,
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
            arkivGateway.tilknyttVedlegg(journalpostId, vedlegg)
        }
    }

    fun ferdigstillJournalpost(referanse: BrevbestillingReferanse) {
        val bestilling = brevbestillingRepository.hent(referanse)
        val journalpostId = checkNotNull(bestilling.journalpostId)
        if (bestilling.journalpostFerdigstilt != true) {
            arkivGateway.ferdigstillJournalpost(journalpostId)
        }
    }

    private fun ferdigstillVedOpprettelseAvJournalpost(bestilling: Brevbestilling): Boolean {
        return bestilling.vedlegg.isEmpty()
    }

    private fun mapPdfBrev(
        personinfo: Personinfo,
        saksnummer: Saksnummer,
        brev: Brev,
        dato: LocalDate,
    ): PdfBrev {
        return PdfBrev(
            mottaker = Mottaker(navn = personinfo.navn, ident = personinfo.fnr),
            saksnummer = saksnummer.nummer,
            dato = dato,
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
                                                if (Miljø.er() != MiljøKode.DEV) { // TODO ta bort denne når faktagrunnlag hentes fra behandlignsflyt
                                                    throw IllegalStateException("Kan ikke lage PDF av brev med manglende faktagrunnlag ${it.tekniskNavn}.")
                                                }
                                                null
                                            }
                                        }
                                    },
                                    type = it.type
                                )
                            })
                    })
            },
        )
    }

}
