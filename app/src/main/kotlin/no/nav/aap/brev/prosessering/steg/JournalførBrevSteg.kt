package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.prosessering.steg.StegUtfører.Kontekst
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class JournalførBrevSteg() : StegUtfører {
    private val log = LoggerFactory.getLogger(JournalførBrevSteg::class.java)
    override fun utfør(kontekst: Kontekst): StegResultat {
        log.info("JournalførBrevSteg")
        return StegResultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): StegUtfører {
            return JournalførBrevSteg()
        }
    }
}
