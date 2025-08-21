package no.nav.aap.brev.util

import no.nav.aap.brev.kontrakt.Språk
import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle

object TimeUtils {
    fun LocalDate.formaterFullLengde(språk: Språk): String {
        return this.format(
            DateTimeFormatterBuilder().appendLocalized(FormatStyle.LONG, null).toFormatter(språk.toLocale())
        )
    }
}
