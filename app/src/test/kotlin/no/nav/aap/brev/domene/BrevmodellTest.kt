package no.nav.aap.brev.domene

import com.fasterxml.jackson.databind.JsonNode
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test


class BrevmodellTest {

    @Test
    fun `deserialiserer og serialiserer Brev riktig`() {
        val deserialisert = DefaultJsonMapper.fromJson<Brev>(brevJson)
        val serialisert: JsonNode = DefaultJsonMapper.objectMapper().valueToTree(deserialisert)
        assertThat(serialisert).isEqualTo(DefaultJsonMapper.objectMapper().readTree(brevJson))
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
                  "overskrift": "H3 overskrift",
                  "kanRedigeres": true,
                  "erFullstendig": false,
                  "blokker": [
                    {
                      "type": "AVSNITT",
                      "innhold": [
                        {
                          "tekst": "Startdatoen er: ",
                          "type": "TEKST",
                          "formattering": [
                            "KURSIV"
                          ]
                        },
                        {
                          "visningsnavn": "Startdato",
                          "tekniskNavn": "STARTDATO",
                          "type": "FAKTAGRUNNLAG"
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