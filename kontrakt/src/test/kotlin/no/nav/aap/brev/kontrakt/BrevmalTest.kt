package no.nav.aap.brev.kontrakt

import no.nav.aap.brev.test.FileUtils
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.junit.jupiter.api.Test

class BrevmalTest {

    @Test
    fun `test`() {
        val brevmal = FileUtils.lesFilTilJson<Brevmal>("brevmal.json")
        println(DefaultJsonMapper.toJson(brevmal))

    }
}