package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.journalføring.JournalføringService
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class FerdigstillJournalpostSteg(val journalføringService: JournalføringService) : Steg.Utfører {
    private val log = LoggerFactory.getLogger(FerdigstillJournalpostSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst) {
        log.info("FerdigstillJournalpostSteg")
        journalføringService.ferdigstillJournalpost(kontekst.referanse)
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): FerdigstillJournalpostSteg {
            return FerdigstillJournalpostSteg(JournalføringService.konstruer(connection))
        }
    }
}
