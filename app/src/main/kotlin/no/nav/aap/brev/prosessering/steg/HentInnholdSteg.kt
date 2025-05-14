package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.innhold.BrevinnholdService
import no.nav.aap.komponenter.dbconnect.DBConnection

class HentInnholdSteg(
    private val brevinnholdService: BrevinnholdService,
) : Steg.Utfører {
    override fun utfør(kontekst: Steg.Kontekst): Steg.Resultat {
        brevinnholdService.hentOgLagre(kontekst.referanse)

        return Steg.Resultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): HentInnholdSteg {
            return HentInnholdSteg(
                brevinnholdService = BrevinnholdService.konstruer(connection),
            )
        }
    }
}
