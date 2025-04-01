package no.nav.aap.brev.bestilling

import no.nav.aap.brev.kontrakt.Rolle

data class SorterbarSignatur(val navIdent: String, val sorteringsnøkkel: Int, val rolle: Rolle?)