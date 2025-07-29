package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.JournalpostRepository
import no.nav.aap.brev.bestilling.JournalpostRepositoryImpl
import no.nav.aap.brev.bestilling.Mottaker
import no.nav.aap.brev.bestilling.MottakerRepository
import no.nav.aap.brev.bestilling.MottakerRepositoryImpl
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
    private val mottakerRepository: MottakerRepository,
) {

    companion object {
        fun konstruer(connection: DBConnection): JournalføringService {
            return JournalføringService(
                pdfService = PdfService.konstruer(connection),
                brevbestillingRepository = BrevbestillingRepositoryImpl(connection),
                personinfoGateway = PdlGateway(),
                journalføringGateway = DokarkivGateway(),
                journalpostRepository = JournalpostRepositoryImpl(connection),
                mottakerRepository = MottakerRepositoryImpl(connection),
            )
        }
    }

    fun journalførBrevbestilling(referanse: BrevbestillingReferanse) {
        val bestilling = brevbestillingRepository.hent(referanse)

        val mottakere = mottakerRepository.hentMottakere(referanse)
        require(mottakere.isNotEmpty()) {
            "Det må være minst én mottaker for å kunne journalføre brevbestilling."
        }

        check(bestilling.brukerIdent != null) {
            "Kan ikke journalføre brev for bestilling der brukerIdent er null"
        }

        val personinfo = personinfoGateway.hentPersoninfo(bestilling.brukerIdent)

        val pdf = pdfService.genererPdfForJournalføring(bestilling, personinfo)

        mottakere.forEach { mottaker ->
            journalførBrevbestilling(bestilling, mottaker, personinfo, pdf)
        }
    }

    private fun journalførBrevbestilling(
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
            eksternReferanseId = mottaker.bestillingMottakerReferanse,
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

        // midlertidig for bakoverkompabilitet
        brevbestillingRepository.lagreJournalpost(
            bestilling.id,
            response.journalpostId,
            response.journalpostferdigstilt
        )
    }

    fun tilknyttVedlegg(referanse: BrevbestillingReferanse) {
        val bestilling = brevbestillingRepository.hent(referanse)
        val vedlegg = bestilling.vedlegg
        if (vedlegg.isEmpty()) return

        val journalposter = journalpostRepository.hentAlleFor(referanse)
        journalposter.forEach {
            journalføringGateway.tilknyttVedlegg(it.journalpostId, vedlegg)
        }
    }

    fun ferdigstillJournalpost(referanse: BrevbestillingReferanse) {
        val journalposter = journalpostRepository.hentAlleFor(referanse)
        journalposter.forEach { journalpost ->
            if (!journalpost.ferdigstilt) {
                journalføringGateway.ferdigstillJournalpost(journalpost.journalpostId)
                journalpostRepository.lagreJournalpostFerdigstilt(journalpost.journalpostId, true)
            }
        }
    }

    private fun ferdigstillVedOpprettelseAvJournalpost(bestilling: Brevbestilling): Boolean {
        return bestilling.vedlegg.isEmpty()
    }
}
