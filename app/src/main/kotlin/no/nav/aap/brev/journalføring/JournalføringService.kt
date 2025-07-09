package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.JournalpostRepository
import no.nav.aap.brev.bestilling.JournalpostRepositoryImpl
import no.nav.aap.brev.bestilling.Mottaker
import no.nav.aap.brev.bestilling.OpprettetJournalpost
import no.nav.aap.brev.bestilling.Pdf
import no.nav.aap.brev.bestilling.PdfService
import no.nav.aap.brev.bestilling.Personinfo
import no.nav.aap.brev.bestilling.PersoninfoGateway
import no.nav.aap.brev.journalføring.JournalføringData.MottakerType
import no.nav.aap.brev.person.PdlGateway
import no.nav.aap.komponenter.dbconnect.DBConnection

class JournalføringService(
    private val pdfService: PdfService,
    private val brevbestillingRepository: BrevbestillingRepository,
    private val personinfoGateway: PersoninfoGateway,
    private val journalføringGateway: JournalføringGateway,
    private val journalpostRepository: JournalpostRepository,
) {

    companion object {
        fun konstruer(connection: DBConnection): JournalføringService {
            return JournalføringService(
                pdfService = PdfService.konstruer(connection),
                brevbestillingRepository = BrevbestillingRepositoryImpl(connection),
                personinfoGateway = PdlGateway(),
                journalføringGateway = DokarkivGateway(),
                journalpostRepository = JournalpostRepositoryImpl(connection),
            )
        }
    }

    fun journalførBrevbestilling(referanse: BrevbestillingReferanse, mottakere: List<Mottaker>) {
        val bestilling = brevbestillingRepository.hent(referanse)

        check(bestilling.brukerIdent != null) {
            "Kan ikke journalføre brev for bestilling der brukerIdent er null"
        }

        val personinfo = personinfoGateway.hentPersoninfo(bestilling.brukerIdent)

        val pdf = pdfService.genererPdfForJournalføring(bestilling, personinfo)

        val journalposterSomAlleredeErJournalført =
            journalpostRepository.hentAlleFor(referanse).filter { it.ferdigstilt }

        mottakere.filterNot { it.id in journalposterSomAlleredeErJournalført.map { j -> j.mottaker.id } }
            .forEach { mottaker ->
                journalførBrevbestilling(bestilling, mottaker, personinfo, pdf)
            }
    }

    fun journalførBrevbestilling(
        bestilling: Brevbestilling,
        mottaker: Mottaker,
        personinfo: Personinfo,
        pdf: Pdf
    ) {
        checkNotNull(bestilling.brev) {
            "Kan ikke journalføre bestilling uten brev."
        }
        requireNotNull(mottaker.id) {
            "Mottaker må være lagret i databasen før journalføring"
        }
        val journalpost = journalpostRepository.hentHvisEksisterer(mottaker.id)
        if (journalpost != null) {
            // Journalpost finnes allerede, ingenting å gjøre
            return
        }
        val journalføringData = JournalføringData(
            brukerFnr = personinfo.personIdent,
            mottakerIdent = mottaker.ident,
            mottakerType = mottaker.identType?.let { MottakerType.valueOf(it.name) },
            mottakerNavn = mottaker.navnOgAdresse?.navn,
            saksnummer = bestilling.saksnummer,
            eksternReferanseId = bestilling.referanse.referanse, // TODO: Hva gjør egentlig denne?
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

        journalpostRepository.lagreJournalpost(
            response.journalpostId,
            response.journalpostferdigstilt,
            mottakerId = mottaker.id
        )
    }

    fun tilknyttVedlegg(referanse: BrevbestillingReferanse, journalpostId: JournalpostId) {
        val bestilling = brevbestillingRepository.hent(referanse)
        val vedlegg = bestilling.vedlegg
        if (vedlegg.isNotEmpty()) {
            journalføringGateway.tilknyttVedlegg(journalpostId, vedlegg)
        }
    }

    fun ferdigstillJournalpost(journalpost: OpprettetJournalpost) {
        if (!journalpost.ferdigstilt) {
            journalføringGateway.ferdigstillJournalpost(journalpost.journalpostId)
            journalpostRepository.lagreJournalpostFerdigstilt(journalpost.journalpostId, true)
        }
    }

    private fun ferdigstillVedOpprettelseAvJournalpost(bestilling: Brevbestilling): Boolean {
        return bestilling.vedlegg.isEmpty()
    }
}
