package no.nav.aap.brev.kontrakt

import com.fasterxml.jackson.databind.JsonNode
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test


class BrevmodellTest {

    @Test
    fun `deserialiserer og serialiserer Brev riktig med alle felter`() {
        val deserialisert = DefaultJsonMapper.fromJson<Brev>(maksimalModell)
        val serialisert: JsonNode = DefaultJsonMapper.objectMapper().valueToTree(deserialisert)
        assertThat(serialisert)
            .isEqualTo(DefaultJsonMapper.objectMapper().readTree(maksimalModell))
    }

    @Test
    fun `deserialiserer og serialiserer Brev riktig med minimalt med felter`() {
        val mapper = DefaultJsonMapper.objectMapper()
        val deserialisert = DefaultJsonMapper.fromJson<Brev>(minimalModell)
        val serialisert: JsonNode = mapper.readTree(mapper.writeValueAsString(deserialisert))
        assertThat(serialisert)
            .isEqualTo(DefaultJsonMapper.objectMapper().readTree(minimalModell))
    }

    @Language("JSON")
    val maksimalModell = """
        {
          "overskrift": "H1 overskrift",
          "journalpostTittel": "Journalpost tittel",
          "tekstbolker": [
            {
              "id": "f5934e55-c30a-422d-a597-4e39311d10c9",
              "overskrift": "H2 overskrift",
              "innhold": [
                {
                  "id": "9a2e4aea-a37d-44de-8e6f-6b7c739adee4",
                  "overskrift": "H3 overskrift",
                  "kanRedigeres": true,
                  "erFullstendig": false,
                  "blokker": [
                    {
                      "id": "f158a425-4807-40c0-8f44-656259b2dfe8",
                      "type": "AVSNITT",
                      "innhold": [
                        {
                          "id": "fcae2886-3063-4816-9ef0-d08ff2cd5979",
                          "tekst": "Startdatoen er: ",
                          "type": "TEKST",
                          "formattering": [
                            "KURSIV"
                          ]
                        },
                        {
                          "id": "0bfe7b93-63b7-4374-946a-99774024363a",
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

    @Language("JSON")
    val minimalModell = """
   {
          "overskrift": null,
          "journalpostTittel": null,
          "tekstbolker": [
            {
              "id": "f5934e55-c30a-422d-a597-4e39311d10c9",
              "overskrift": null,
              "innhold": [
                {
                  "id": "9a2e4aea-a37d-44de-8e6f-6b7c739adee4",
                  "overskrift": null,
                  "kanRedigeres": true,
                  "erFullstendig": false,
                  "blokker": [
                    {
                      "id": "f158a425-4807-40c0-8f44-656259b2dfe8",
                      "type": "AVSNITT",
                      "innhold": [
                        {
                          "id": "fcae2886-3063-4816-9ef0-d08ff2cd5979",
                          "tekst": "Startdatoen er: ",
                          "type": "TEKST",
                          "formattering": [
                            "KURSIV"
                          ]
                        },
                        {
                          "id": "0bfe7b93-63b7-4374-946a-99774024363a",
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