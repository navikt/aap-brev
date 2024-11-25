package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.bestilling.Pdf
import no.nav.aap.brev.bestilling.Personinfo

interface ArkivGateway {
    fun journalførBrev(bestilling: Brevbestilling,
                       personinfo: Personinfo,
                       pdf: Pdf)
}