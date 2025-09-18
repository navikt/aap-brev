package no.nav.aap.brev.kontrakt

import com.fasterxml.jackson.core.JsonParser
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.junit.jupiter.api.Test

class BrevmalTest {

    @Test
    fun `test`() {
        val json = lesFil("brevmal.json")
        val objectMapper = DefaultJsonMapper.objectMapper()
        objectMapper.enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION)
        val brevmal = objectMapper.readValue(json, Brevmal::class.java)
        println(DefaultJsonMapper.toJson(brevmal))
    }

    private fun lesFil(filnavn: String): String {
        val inputStream = checkNotNull(this::class.java.getResourceAsStream("/$filnavn")) {
            "Fant ikke fil med navn $filnavn"
        }
        return inputStream.bufferedReader().readText()
    }
}