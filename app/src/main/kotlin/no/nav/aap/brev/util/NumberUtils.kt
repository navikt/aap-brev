package no.nav.aap.brev.util

import no.nav.aap.brev.kontrakt.Språk
import java.math.BigDecimal
import java.text.DecimalFormat

object NumberUtils {
    fun BigDecimal.formater(språk: Språk): String {
        val decimalFormat = DecimalFormat.getInstance(språk.toLocale())
        decimalFormat.maximumFractionDigits = 2
        return decimalFormat.format(this)
    }
}
