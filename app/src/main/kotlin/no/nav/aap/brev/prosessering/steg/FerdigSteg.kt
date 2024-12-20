package no.nav.aap.brev.prosessering.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class FerdigSteg() : Steg.Utfører {
    private val log = LoggerFactory.getLogger(FerdigSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst): Steg.Resultat {
        log.info("Prosessering er ferdig.")
        return Steg.Resultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): FerdigSteg {
            return FerdigSteg()
        }
    }
}
