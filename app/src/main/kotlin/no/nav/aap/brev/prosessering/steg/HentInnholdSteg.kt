package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.innhold.BrevinnholdService
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class HentInnholdSteg(
    private val brevinnholdService: BrevinnholdService,
) : Steg.Utfører {
    private val log = LoggerFactory.getLogger(HentInnholdSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst): Steg.Resultat {
        log.info("Henter brevinnhold for bestillingsreferanse=${kontekst.referanse}")

        brevinnholdService.hentOgLagre(kontekst.referanse)

        return Steg.Resultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): Steg.Utfører {
            return HentInnholdSteg(
                brevinnholdService = BrevinnholdService.konstruer(connection),
            )
        }
    }
}
