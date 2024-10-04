package no.nav.aap.brev.prosessering.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class HentFaktagrunnlagSteg() : Steg.Utfører {
    private val log = LoggerFactory.getLogger(HentFaktagrunnlagSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst): Steg.Resultat {
        log.info("HentFaktagrunnlagSteg")
        return Steg.Resultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): Steg.Utfører {
            return HentFaktagrunnlagSteg()
        }
    }
}
