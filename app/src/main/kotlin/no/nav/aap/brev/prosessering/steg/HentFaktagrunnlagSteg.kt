package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.innhold.FaktagrunnlagService
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class HentFaktagrunnlagSteg(
    private val faktagrunnlagService: FaktagrunnlagService
) : Steg.Utfører {
    private val log = LoggerFactory.getLogger(HentFaktagrunnlagSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst): Steg.Resultat {
        log.info("HentFaktagrunnlagSteg")
        faktagrunnlagService.hentOgFyllInnFaktagrunnlag(kontekst.referanse)
        return Steg.Resultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): HentFaktagrunnlagSteg {
            return HentFaktagrunnlagSteg(FaktagrunnlagService.konstruer(connection))
        }
    }
}
