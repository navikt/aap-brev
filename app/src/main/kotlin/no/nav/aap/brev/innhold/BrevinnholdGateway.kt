package no.nav.aap.brev.innhold

import no.nav.aap.brev.domene.Brevinnhold
import no.nav.aap.brev.domene.Brevtype
import no.nav.aap.brev.domene.Språk

interface BrevinnholdGateway {
    fun hentBrev(brevtype: Brevtype, språk: Språk): Brevinnhold
}