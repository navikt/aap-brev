package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.komponenter.dbconnect.DBConnection

sealed interface Steg {

    fun konstruer(connection: DBConnection): Utfører

    sealed interface Utfører {
        fun utfør(kontekst: Kontekst)
    }

    data class Kontekst(val referanse: BrevbestillingReferanse)
}
