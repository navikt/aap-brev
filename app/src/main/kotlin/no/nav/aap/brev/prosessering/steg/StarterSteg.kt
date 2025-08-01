package no.nav.aap.brev.prosessering.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class StarterSteg() : Steg.Utfører {
    private val log = LoggerFactory.getLogger(StarterSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst) {
        log.info("Prosessering har startet.")
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): StarterSteg {
            return StarterSteg()
        }
    }
}
