package no.nav.aap.brev.innhold

import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.BrevbestillingService
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.no.nav.aap.brev.test.Fakes
import no.nav.aap.brev.test.fakes.faktagrunnlagForBehandling
import no.nav.aap.brev.test.fakes.randomBehandlingReferanse
import no.nav.aap.brev.test.fakes.randomSaksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class HentFaktagrunnlagTest {
    companion object {
        private val fakes = Fakes()
        private val dataSource = InitTestDatabase.dataSource

        @JvmStatic
        @AfterAll
        fun afterAll() {
            fakes.close()
        }
    }

    @Test
    fun `erstatt faktagrunnlag`() {
        dataSource.transaction { connection ->
            val brevbestillingService = BrevbestillingService.konstruer(connection)
            val hentFaktagrunnlagService = HentFaktagrunnlagService.konstruer(connection)
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)

            val behandlingReferanse = randomBehandlingReferanse()
            val referanse =
                brevbestillingService.opprettBestilling(
                    randomSaksnummer(),
                    behandlingReferanse,
                    Brevtype.INNVILGELSE,
                    Språk.NB,
                    emptySet(),
                )

            faktagrunnlagForBehandling(behandlingReferanse, setOf(Faktagrunnlag.Startdato(LocalDate.of(2001, 2, 3))))

            val ubehandletBrev = DefaultJsonMapper.fromJson<Brev>(brevMedFaktagrunnlag)

            brevbestillingRepository.oppdaterBrev(referanse, ubehandletBrev)

            hentFaktagrunnlagService.hentFaktagrunnlag(referanse)

            val oppdatertBrev = brevbestillingRepository.hent(referanse).brev
            val brevMedInnflettetFaktagrunnlag = DefaultJsonMapper.fromJson<Brev>(brevMedInnflettetFaktagrunnlag)

            assertEquals(oppdatertBrev, brevMedInnflettetFaktagrunnlag)
        }
    }

    @Language("JSON")
    val brevMedFaktagrunnlag =
        """
        {
          "overskrift": "H1 overskrift",
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
    val brevMedInnflettetFaktagrunnlag =
        """
        {
          "overskrift": "H1 overskrift",
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
                          "tekst": "03.02.2001",
                          "type": "TEKST",
                          "formattering": []
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