package no.nav.aap.brev.bestilling

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.aap.brev.App
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger(App::class.java)

@JvmInline
value class BrevmalJson(val json: ObjectNode) {
    fun tilBrevmal(): Brevmal {
        LOGGER.info("BrevmalJson: $json")
        return DefaultJsonMapper.objectMapper().treeToValue(json, Brevmal::class.java)
    }
}
