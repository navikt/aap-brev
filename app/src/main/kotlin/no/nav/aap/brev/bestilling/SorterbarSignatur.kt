package no.nav.aap.brev.bestilling

import no.nav.aap.brev.kontrakt.Rolle
import no.nav.aap.brev.kontrakt.SignaturGrunnlag
import kotlin.collections.mapIndexed

data class SorterbarSignatur(val navIdent: String, val sorteringsnøkkel: Int, val rolle: Rolle?, val enhet: String?)

fun List<SignaturGrunnlag>.tilSorterbareSignaturer(): List<SorterbarSignatur> {
    return this.mapIndexed { index, signatur ->
        SorterbarSignatur(
            navIdent = signatur.navIdent,
            sorteringsnøkkel = index + 1,
            rolle = signatur.rolle,
            enhet = signatur.enhet
        )
    }
}