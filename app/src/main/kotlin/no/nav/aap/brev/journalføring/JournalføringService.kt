package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.BehandlingsflytGateway
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.PdfGateway
import no.nav.aap.brev.bestilling.PersoninfoGateway
import no.nav.aap.brev.bestilling.SaksbehandlingPdfGenGateway
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.LocalDate

class JournalføringService(
    val brevbestillingRepository: BrevbestillingRepository,
    val personinfoGateway: PersoninfoGateway,
    val pdfGateway: PdfGateway,
    val arkivGateway: ArkivGateway,
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

    fun genererBrevOgJournalfør(referanse: BrevbestillingReferanse) {
        val bestilling = brevbestillingRepository.hent(referanse)
        val personinfo = personinfoGateway.hentPersoninfo(bestilling.saksnummer)
        val pdf = pdfGateway.genererPdf(personinfo, bestilling.saksnummer, bestilling.brev!!, LocalDate.now())

//        arkivGateway.journalførBrev(bestilling, personinfo, pdf)
    }
}
