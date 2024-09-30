package no.nav.aap.brev.innhold

import no.nav.aap.brev.domene.Brev
import no.nav.aap.brev.domene.Brevtype
import no.nav.aap.brev.domene.Språk

interface BrevinnholdGateway {
    fun hentBrevmal(brevtype: Brevtype, språk: Språk): Brev
}