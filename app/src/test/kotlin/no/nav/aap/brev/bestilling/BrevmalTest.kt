package no.nav.aap.brev.bestilling

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.aap.brev.test.FileUtils
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BrevmalTest {

    @Test
    fun `deserialiserer brevmal`() {
        val brevmal = FileUtils.lesFilTilJson<Brevmal>("brevmal.json")
        val forventetBrevmal = FileUtils.lesFilTilJson<ObjectNode>("forventet_serialisert_brevmal.json")
        assertEquals(DefaultJsonMapper.toJson(forventetBrevmal), DefaultJsonMapper.toJson(brevmal))
    }
}
