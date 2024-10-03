package no.nav.aap.brev.prosessering

import no.nav.aap.brev.BrevbestillingRepository
import no.nav.aap.brev.domene.BrevbestillingReferanse
import no.nav.aap.brev.innhold.BrevinnholdGateway
import org.slf4j.LoggerFactory

class ProsesserStegService(
    private val brevbestillingRepository: BrevbestillingRepository,
    private val brevinnholdGateway: BrevinnholdGateway,
) {
    private val log = LoggerFactory.getLogger(ProsesserStegService::class.java)

    data class Kontekst(val referanse: BrevbestillingReferanse)

    fun prosesserBestilling(kontekst: Kontekst) {
        val referanse = kontekst.referanse
        val bestilling = brevbestillingRepository.hent(referanse)

        log.info("Henter brevinnhold for bestillingsreferanse=$referanse")
        val brev = brevinnholdGateway.hentBrevmal(bestilling.brevtype, bestilling.språk)

        brevbestillingRepository.oppdaterBrev(referanse, brev)
    }
}