package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.domene.BrevbestillingReferanse

sealed interface StegUtfører {

    data  class Kontekst(val referanse: BrevbestillingReferanse)

    fun utfør(kontekst: Kontekst): StegResultat
}
