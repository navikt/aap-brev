package no.nav.aap.brev.test

import com.fasterxml.jackson.core.JsonParser
import no.nav.aap.komponenter.json.DefaultJsonMapper

object FileUtils {
    fun lesFil(filnavn: String): String {
        val inputStream = checkNotNull(this::class.java.getResourceAsStream("/$filnavn")) {
            "Fant ikke fil med navn $filnavn"
        }
        return inputStream.bufferedReader().readText()
    }

    inline fun <reified T> lesFilTilJson(filnavn: String): T {
        val fil = lesFil(filnavn)
        return DefaultJsonMapper
            .objectMapper()
            .reader()
            .with(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION)
            .readValue(fil, T::class.java)
    }
}