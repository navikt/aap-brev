package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.journalføring.JournalføringService
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class TilknyttVedleggSteg(val journalføringService: JournalføringService) : Steg.Utfører {
    private val log = LoggerFactory.getLogger(TilknyttVedleggSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst) {
        log.info("TilknyttVedleggSteg")
        journalføringService.tilknyttVedlegg(kontekst.referanse)
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): TilknyttVedleggSteg {
            return TilknyttVedleggSteg(JournalføringService.konstruer(connection))
        }
    }
}