package no.nav.aap.brev.bestilling

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.aap.brev.kontrakt.Brevmal
import no.nav.aap.komponenter.json.DefaultJsonMapper

@JvmInline
value class BrevmalJson(val json: ObjectNode) {
    fun tilBrevmal(): Brevmal {
        return DefaultJsonMapper.objectMapper().treeToValue(json, Brevmal::class.java)
    }
}
