package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.PdfService
import no.nav.aap.brev.bestilling.PersoninfoGateway
import no.nav.aap.brev.journalføring.JournalføringData.MottakerType
import no.nav.aap.brev.person.PdlGateway
import no.nav.aap.komponenter.dbconnect.DBConnection

class JournalføringService(
    private val pdfService: PdfService,
    private val brevbestillingRepository: BrevbestillingRepository,
    private val personinfoGateway: PersoninfoGateway,
    private val journalføringGateway: JournalføringGateway,
) {

    companion object {
        fun konstruer(connection: DBConnection): JournalføringService {
            return JournalføringService(
                pdfService = PdfService.konstruer(connection),
                brevbestillingRepository = BrevbestillingRepositoryImpl(connection),
                personinfoGateway = PdlGateway(),
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

        val pdf = pdfService.genererPdfForJournalføring(bestilling, personinfo)

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
            brevbestillingRepository.lagreJournalpostFerdigstilt(bestilling.id, true)
        }
    }

    private fun ferdigstillVedOpprettelseAvJournalpost(bestilling: Brevbestilling): Boolean {
        return bestilling.vedlegg.isEmpty()
    }
}
