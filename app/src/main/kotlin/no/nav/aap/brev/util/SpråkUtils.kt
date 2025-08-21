package no.nav.aap.brev.util

import no.nav.aap.brev.kontrakt.Språk
import java.util.Locale

fun Språk.toLocale(): Locale {
    return when (this) {
        Språk.NB -> Locale.forLanguageTag("no-nb")
        Språk.NN -> Locale.forLanguageTag("no-nn")
        Språk.EN -> Locale.ENGLISH
    }
}
