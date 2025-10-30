package no.nav.aap.brev.innhold

import no.nav.aap.brev.bestilling.BrevmalJson
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk

interface BrevinnholdGateway {
    fun hentBrev(brevtype: Brevtype, språk: Språk): Brev
    fun hentBrevmal(brevtype: Brevtype, språk: Språk): BrevmalJson
}