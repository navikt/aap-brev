package no.nav.aap.brev.innhold

import no.nav.aap.brev.domene.BehandlingReferanse
import no.nav.aap.brev.domene.Brevtype
import no.nav.aap.brev.domene.Språk
import org.slf4j.LoggerFactory

class BrevinnholdService(private val brevinnholdGateway: BrevinnholdGateway) {

    private val log = LoggerFactory.getLogger(BrevinnholdService::class.java)

    fun behandleBrevbestilling(
        behandlingReferanse: BehandlingReferanse,
        brevtype: Brevtype,
        språk: Språk,
    ) {
        log.info("Henter brevinnhold for behandlingReferanse=$behandlingReferanse  brevtype=$brevtype språk=$språk")
        val brevinnhold = brevinnholdGateway.hentBrev(brevtype, språk)
        log.info("Hentet brevinnhold: ${brevinnhold.innhold}")
    }
}
