package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.innhold.HentFaktagrunnlagService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import org.slf4j.LoggerFactory

class HentFaktagrunnlagSteg(
    private val hentFaktagrunnlagService: HentFaktagrunnlagService
) : Steg.Utfører {
    private val log = LoggerFactory.getLogger(HentFaktagrunnlagSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst): Steg.Resultat {
        log.info("HentFaktagrunnlagSteg")
        if (Miljø.er() != MiljøKode.DEV) {
            hentFaktagrunnlagService.hentFaktagrunnlag(kontekst.referanse)
        }
        return Steg.Resultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): Steg.Utfører {
            return HentFaktagrunnlagSteg(HentFaktagrunnlagService.konstruer(connection))
        }
    }
}
