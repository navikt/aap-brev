package no.nav.aap.brev.innhold

import no.nav.aap.brev.bestilling.BrevmalJson
import no.nav.aap.brev.kontrakt.AvslagsÅrsak
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk

interface BrevinnholdGateway {
    fun hentBrev(brevtype: Brevtype, språk: Språk, avslagsårsaker: Set<AvslagsÅrsak>): Brev
    fun hentBrevmal(brevtype: Brevtype, språk: Språk): BrevmalJson
}