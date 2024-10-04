package no.nav.aap.brev.prosessering.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class FerdigstillBrevSteg() : Steg.Utfører {
    private val log = LoggerFactory.getLogger(FerdigstillBrevSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst): Steg.Resultat {
        log.info("FerdigstillBrevSteg")
        return Steg.Resultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): Steg.Utfører {
            return FerdigstillBrevSteg()
        }
    }
}
