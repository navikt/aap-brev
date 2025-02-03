package no.nav.aap.brev.util

import no.nav.aap.brev.kontrakt.Språk
import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.*

fun LocalDate.formaterFullLengde(språk: Språk): String {
    val locale = when (språk) {
        Språk.NB -> Locale.forLanguageTag("no-nb")
        Språk.NN -> Locale.forLanguageTag("no-nn")
        Språk.EN -> Locale.ENGLISH
    }
    return this.format(DateTimeFormatterBuilder().appendLocalized(FormatStyle.LONG, null).toFormatter(locale))
}
