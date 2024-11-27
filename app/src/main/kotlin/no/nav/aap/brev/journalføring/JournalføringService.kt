package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.BehandlingsflytGateway
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.PdfGateway
import no.nav.aap.brev.bestilling.Personinfo
import no.nav.aap.brev.bestilling.PersoninfoGateway
import no.nav.aap.brev.bestilling.SaksbehandlingPdfGenGateway
import no.nav.aap.brev.bestilling.Saksnummer
import no.nav.aap.brev.kontrakt.BlokkInnhold
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.PdfBrev
import no.nav.aap.brev.kontrakt.PdfBrev.Blokk
import no.nav.aap.brev.kontrakt.PdfBrev.FormattertTekst
import no.nav.aap.brev.kontrakt.PdfBrev.Innhold
import no.nav.aap.brev.kontrakt.PdfBrev.Mottaker
import no.nav.aap.brev.kontrakt.PdfBrev.Tekstbolk
import no.nav.aap.komponenter.dbconnect.DBConnection
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
        val personinfo = personinfoGateway.hentPersoninfo(bestilling.saksnummer)
        val pdfBrev = mapPdfBrev(personinfo, bestilling.saksnummer, bestilling.brev!!, LocalDate.now())
        val journalpostInfo = JournalpostInfo(
            fnr = personinfo.fnr,
            navn = personinfo.navn,
            saksnummer = bestilling.saksnummer,
            eksternReferanseId = bestilling.referanse.referanse,
            tittel = checkNotNull(value = bestilling.brev.overskrift),
            brevkode = bestilling.brevtype.name
        )

        val pdf = pdfGateway.genererPdf(pdfBrev)
        val journalpostId =  arkivGateway.journalførBrev(journalpostInfo, pdf)
        brevbestillingRepository.lagreJournalpost(bestilling.id, journalpostId)
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

                                            else -> null
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
