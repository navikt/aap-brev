package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.prosessering.steg.StegUtfører.Kontekst
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class HentFaktagrunnlagSteg() : StegUtfører {
    private val log = LoggerFactory.getLogger(HentFaktagrunnlagSteg::class.java)
    override fun utfør(kontekst: Kontekst): StegResultat {
        log.info("HentFaktagrunnlagSteg")
        return StegResultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): StegUtfører {
            return HentFaktagrunnlagSteg()
        }
    }
}
