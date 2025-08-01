package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.journalføring.JournalføringService
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class JournalførBrevSteg(val journalføringService: JournalføringService) : Steg.Utfører {
    private val log = LoggerFactory.getLogger(JournalførBrevSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst) {
        log.info("JournalførBrevSteg")
        journalføringService.journalførBrevbestilling(kontekst.referanse)
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): JournalførBrevSteg {
            return JournalførBrevSteg(JournalføringService.konstruer(connection))
        }
    }
}
