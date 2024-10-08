package no.nav.aap.brev.innhold

import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk

interface BrevinnholdGateway {
    fun hentBrevmal(brevtype: Brevtype, språk: Språk): Brev
}