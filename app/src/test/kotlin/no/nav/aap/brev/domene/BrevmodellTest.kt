package no.nav.aap.brev.domene

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test


class BrevmodellTest {

    @Test
    fun `deserialiserer og serialiserer Brev riktig`() {
        val mapper = jacksonObjectMapper()
        val deserialisert = mapper.readValue<Brev>(brevJson)
        val serialisert: JsonNode = mapper.valueToTree(deserialisert)
        assertThat(serialisert).isEqualTo(mapper.readTree(brevJson))
    }

    @Language("JSON")
    val brevJson = """
        {
          "overskrift": "H1 overskrift",
          "tekstbolker": [
            {
              "overskrift": "H2 overskrift",
              "innhold": [
                {
                  "sprak": "nb",
                  "overskrift": "H3 overskrift",
                  "kanRedigeres": true,
                  "erFullstendig": false,
                  "avsnitt": [
                    {
                      "listeInnrykk": null,
                      "tekst": [
                        {
                          "tekst": "Startdatoen er: ",
                          "type": "tekst",
                          "formattering": [
                            "kursiv"
                          ]
                        },
                        {
                          "visningsnavn": "Startdato",
                          "tekniskNavn": "STARTDATO",
                          "type": "faktagrunnlag"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
          ]
        }
    """.trimIndent()
}