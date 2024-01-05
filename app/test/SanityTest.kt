import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.assertNotNull

class SanityTest {

    private val jackson = jacksonObjectMapper()

    @Test
    fun loggers() {
        val pretty: Logger = LoggerFactory.getLogger("pretty")
        val secure: Logger = LoggerFactory.getLogger("secureLog")
        val root: Logger = LoggerFactory.getLogger("whatever")

        pretty.info("pretty")
        secure.info("secure")
        root.info("root")
    }

    @Test
    fun test_brevmaloversikt_serializer() {
        val brevOversikt = jackson.readValue<List<SanityModel.Brevmal>>(BREVMALER_RESPONSE)
        assertNotNull(brevOversikt)
    }

    @Test
    fun test_brevmal_serializer() {
        val brevmal = jackson.readValue<SanityModel.Brevmal>(BREVMAL_RESPONSE)
        assertNotNull(brevmal)
    }
}

@Language("json")
private const val BREVMALER_RESPONSE = """
[
    {
        "brevtittel": "NAV har avslått søknaden din om arbeidsavklaringspenger",
        "_id": "084b7075-10e2-4f10-9211-96f7bd560bc2",
        "brevtype": "Vedtak - Avslag AAP"
    },
    {
        "brevtittel": "NAV avslutter utbetalingen av arbeidsavklaringspenger",
        "_id": "231929a2-e140-4f13-b5e2-456644c278d0",
        "brevtype": "Vedtak - Opphør ved Dødsfall"
    },
    {
        "brevtittel": "NAV har innvilget deg arbeidsavklaringspenger (AAP)",
        "_id": "25d71e61-639f-4c0a-89ec-490c02f72847",
        "brevtype": "Vedtak - Innvilgelse AAP"
    },
    {
        "brevtittel": "Dine rettigheter og muligheter til å klage",
        "_id": "53f5b0b5-a44a-4330-8385-5610cf38a0fe",
        "brevtype": "Vedlegg - Dine rettigheter og muligheter til å klage"
    },
    {
        "brevtittel": "Dine rettigheter og plikter på AAP",
        "_id": "73a28a10-e719-4fa0-bbac-5894dff8ea3e",
        "brevtype": "Vedlegg - Orientering om rettigheter og plikter på AAP"
    },
    {
        "brevtittel": "NAV har forlenget dine arbeidsavklaringspenger (AAP)",
        "_id": "82414c9a-e0a9-46d5-8273-5ba5ffdf5dc7",
        "brevtype": "Vedtak - Forlengelse AAP"
    },
    {
        "brevtittel": "NAV har avsluttet dine arbeidsavklaringspenger (AAP)",
        "_id": "ad84769b-820d-4546-9f15-b3be05440fee",
        "brevtype": "Vedtak - Opphør av AAP"
    },
    {
        "brevtittel": "NAV har gjenopptatt arbeidsavklaringspengene (AAP) dine",
        "_id": "b18c9dd5-ad5d-42d8-977b-0bad55c952f9",
        "brevtype": "Vedtak - Gjenopptak AAP"
    },
    {
        "brevtittel": "Vi har gjort en endring i AAP-saken din",
        "_id": "c2bb123d-7efa-4117-8151-26f33ff74f8c",
        "brevtype": "Vedtak - Endring AAP (revurdering)"
    },
    {
        "brevtittel": "NAV har gjenopptatt arbeidsavklaringspengene (AAP) dine",
        "_id": "drafts.b18c9dd5-ad5d-42d8-977b-0bad55c952f9",
        "brevtype": "Vedtak - Gjenopptak AAP"
    },
    {
        "brevtittel": "NAV stanser utbetalingen av arbeidsavklaringspenger",
        "_id": "f5be76be-af18-4f56-adc6-24d96aab2051",
        "brevtype": "Vedtak - Stans av AAP"
    }
]
"""

@Language("json")
private const val BREVMAL_RESPONSE = """
{
    "brevtittel": "NAV avslutter utbetalingen av arbeidsavklaringspenger",
    "_id": "231929a2-e140-4f13-b5e2-456644c278d0",
    "brevtype": "Vedtak - Opphør ved Dødsfall",
    "innhold": [
        {
            "kanRedigeres": true,
            "innhold": [
                {
                    "_type": "content",
                    "style": "normal",
                    "_key": "f674ca8d268e",
                    "markDefs": [],
                    "children": [
                        {
                            "_type": "span",
                            "marks": [],
                            "text": "Hva det gjelder: Vi har fått beskjed om at [NAVN] er død.",
                            "_key": "89ece9d24c8a0"
                        }
                    ]
                }
            ],
            "overskrift": null,
            "niva": null,
            "_type": "standardtekst",
            "_id": "a85538a0-e191-4f95-bef6-379924893894",
            "hjelpetekst": [
                {
                    "markDefs": [],
                    "children": [
                        {
                            "_type": "span",
                            "marks": [],
                            "text": "Hva var problemstillingen som skulle vurderes i denne saken? ",
                            "_key": "e4505ec6c76c"
                        }
                    ],
                    "_type": "contentUtenVariabler",
                    "style": "normal",
                    "_key": "2d8b9c10ac2a"
                }
            ]
        },
        {
            "_type": "standardtekst",
            "_id": "4ab1162d-f3ec-40ad-84db-8c6de4eb01ba",
            "kanRedigeres": true,
            "innhold": [
                {
                    "_type": "content",
                    "style": "normal",
                    "_key": "d67b66931407",
                    "markDefs": [],
                    "children": [
                        {
                            "text": "Vi sender dette brevet for å informere om at vi avslutter utbetalingen av AAP fra <sluttDato>.",
                            "_key": "79916545f72f0",
                            "_type": "span",
                            "marks": [
                                "strong"
                            ]
                        }
                    ]
                }
            ],
            "overskrift": "Vedtak",
            "niva": "H2",
            "hjelpetekst": [
                {
                    "_type": "contentUtenVariabler",
                    "style": "normal",
                    "_key": "9b042730c71b",
                    "markDefs": [],
                    "children": [
                        {
                            "marks": [],
                            "text": "Vi starter med det som er viktigst for leseren – ",
                            "_key": "4b83fafaef73",
                            "_type": "span"
                        },
                        {
                            "_type": "span",
                            "marks": [
                                "strong"
                            ],
                            "text": "konklusjonen i saken.",
                            "_key": "0b0d32a38809"
                        }
                    ]
                },
                {
                    "style": "normal",
                    "_key": "45faf823d503",
                    "markDefs": [],
                    "children": [
                        {
                            "_type": "span",
                            "marks": [],
                            "text": "Vi setter dette i ",
                            "_key": "44c981a9abd3"
                        },
                        {
                            "marks": [
                                "strong"
                            ],
                            "text": "fet ",
                            "_key": "ac4c26aa99a3",
                            "_type": "span"
                        },
                        {
                            "marks": [],
                            "text": "skrift. ",
                            "_key": "ed4981b58f02",
                            "_type": "span"
                        }
                    ],
                    "_type": "contentUtenVariabler"
                }
            ]
        },
        {
            "niva": "H2",
            "hjelpetekst": [
                {
                    "children": [
                        {
                            "_type": "span",
                            "marks": [],
                            "text": "Vi skal nå beskrive vurderingene vi har gjort i saken – både det som talte for og det som talte mot avgjørelsen vår. Dette står i forvaltningsloven § 25 første og tredje ledd.",
                            "_key": "1b00f11ed35c"
                        }
                    ],
                    "_type": "contentUtenVariabler",
                    "style": "normal",
                    "_key": "c5f834a9f993",
                    "markDefs": []
                },
                {
                    "markDefs": [],
                    "children": [
                        {
                            "_type": "span",
                            "marks": [],
                            "text": "Det skal være mulig for mottaker å forstå:",
                            "_key": "23168274868b"
                        }
                    ],
                    "_type": "contentUtenVariabler",
                    "style": "normal",
                    "_key": "1feb4ca2a3bc"
                },
                {
                    "_type": "contentUtenVariabler",
                    "style": "normal",
                    "_key": "96ae677c81d9",
                    "listItem": "number",
                    "markDefs": [],
                    "children": [
                        {
                            "marks": [],
                            "text": "Hvilke vurderinger og avveiinger som er gjort. Hva som var ",
                            "_key": "74424a6833b3",
                            "_type": "span"
                        },
                        {
                            "_type": "span",
                            "marks": [
                                "strong"
                            ],
                            "text": "utslagsgivende",
                            "_key": "d64482ff7416"
                        },
                        {
                            "_key": "fa653c6f976b",
                            "_type": "span",
                            "marks": [],
                            "text": " for konklusjonen i saken. "
                        }
                    ],
                    "level": 1
                },
                {
                    "_key": "e2df566876d9",
                    "listItem": "number",
                    "markDefs": [],
                    "children": [
                        {
                            "marks": [],
                            "text": "Hvilke paragrafer og ",
                            "_key": "fb06c588064c",
                            "_type": "span"
                        },
                        {
                            "_type": "span",
                            "marks": [
                                "strong"
                            ],
                            "text": "regler",
                            "_key": "672048c1fe3e"
                        },
                        {
                            "_key": "0abf05b3177b",
                            "_type": "span",
                            "marks": [],
                            "text": " som var avgjørende. Vi drar de enkelte paragrafer og regler inn i den "
                        },
                        {
                            "_type": "span",
                            "marks": [
                                "strong"
                            ],
                            "text": "løpende",
                            "_key": "7668f798959d"
                        },
                        {
                            "_type": "span",
                            "marks": [],
                            "text": " teksten.",
                            "_key": "856429356784"
                        }
                    ],
                    "level": 1,
                    "_type": "contentUtenVariabler",
                    "style": "normal"
                }
            ],
            "kanRedigeres": true,
            "innhold": [
                {
                    "markDefs": [],
                    "children": [
                        {
                            "text": "Når en person dør, blir utbetalinger som den avdøde har fått fra NAV stanset. Les mer på ",
                            "_key": "93c4333235440",
                            "_type": "span",
                            "marks": []
                        },
                        {
                            "_type": "span",
                            "marks": [
                                "underline"
                            ],
                            "text": "nav.no/mistet-noen",
                            "_key": "5c3e4459bf48"
                        },
                        {
                            "_type": "span",
                            "marks": [],
                            "text": ".",
                            "_key": "2cd7bcabd430"
                        }
                    ],
                    "_type": "content",
                    "style": "normal",
                    "_key": "8c01611cac6a"
                }
            ],
            "_type": "standardtekst",
            "_id": "66350042-e827-4a4d-97ae-63bb2763de9d",
            "overskrift": "Vurderingen vår"
        },
        {
            "_type": "systeminnhold",
            "_id": "222d5fd7-fe07-41d5-ba73-b9ee215cbbb4",
            "systemNokkel": "vedtak_vedlegg",
            "overskrift": "I vurderingen vår har vi lagt vekt på denne informasjonen:",
            "niva": "H3"
        },
        {
            "_type": "standardtekst",
            "_id": "d336d454-25b6-4518-b715-07eb59a36331",
            "overskrift": "Har du spørsmål?",
            "niva": "H2",
            "hjelpetekst": null,
            "kanRedigeres": false,
            "innhold": [
                {
                    "style": "normal",
                    "_key": "372e1af3d0a7",
                    "markDefs": [],
                    "children": [
                        {
                            "_type": "span",
                            "marks": [],
                            "text": "Du finner mer informasjon på nav.no/aap. På ",
                            "_key": "28124c31dc07"
                        },
                        {
                            "_type": "span",
                            "marks": [
                                "underline"
                            ],
                            "text": "nav.no/kontakt",
                            "_key": "1b2adf5babed"
                        },
                        {
                            "_type": "span",
                            "marks": [],
                            "text": " kan du chatte eller skrive til oss. Hvis du ikke finner svar på nav.no, kan du ringe oss på telefon ",
                            "_key": "fcfa3a2b55b7"
                        },
                        {
                            "_ref": "f8efead5-06a4-4c2c-a016-6e8785da4242",
                            "_type": "inlineElement",
                            "_key": "2c91de7d6b89",
                            "text": "55 55 33 33"
                        },
                        {
                            "_type": "span",
                            "marks": [],
                            "text": ", hverdager kl. 09.00 - 15.00.",
                            "_key": "4086361cad2a"
                        }
                    ],
                    "_type": "content"
                }
            ]
        },
        {
            "innhold": [
                {
                    "_key": "3ee7b587752e",
                    "markDefs": [],
                    "children": [
                        {
                            "marks": [],
                            "text": "Med vennlig hilsen",
                            "_key": "318082d20caf",
                            "_type": "span"
                        }
                    ],
                    "_type": "content",
                    "style": "normal"
                },
                {
                    "_type": "content",
                    "style": "normal",
                    "_key": "ede33b831f37",
                    "markDefs": [],
                    "children": [
                        {
                            "_type": "span",
                            "marks": [],
                            "text": "",
                            "_key": "7a0b96c31c2a"
                        },
                        {
                            "_type": "systemVariabel",
                            "_key": "26599871dbd1",
                            "_ref": "911b767b-939b-4088-bfec-fb358bfee911",
                            "systemVariabel": "signatur_saksbehandler_og_beslutter"
                        },
                        {
                            "_type": "span",
                            "marks": [],
                            "text": "",
                            "_key": "3044390fc959"
                        }
                    ]
                }
            ],
            "_type": "standardtekst",
            "_id": "884f9600-8e88-4943-8577-547554adb869",
            "overskrift": null,
            "niva": null,
            "hjelpetekst": null,
            "kanRedigeres": false
        }
    ]
}
"""

/*
    @Test
    fun `exposes route for finding brevmal by id`() {
        Sanity().use { sanity ->
            val config = Config(SanityConfig("token", sanity.host))
            testApplication {
                application { server(config) }
                val response =
                    httpClient.get("/brev/1") {
                        contentType(ContentType.Application.Json)
                        accept(ContentType.Application.Json)
                    }
                assertEquals(Brevmal("result", listOf()), response.body<Brevmal>())
            }
        }
    }
    private class Sanity : AutoCloseable {
        private val server = embeddedServer(Netty, port = 0) { fake() }.start()
        val host: String
            get() = "http://localhost:${server.port()}"

        fun Application.fake() {
            install(ContentNegotiation) {
                jackson {
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                }
            }
            routing { get("/$VERSION/data/query/$DATASET") { call.respond(Brevmal("result", listOf())) } }
        }

        override fun close() = server.stop(0L, 0L)
    }

    private val ApplicationTestBuilder.httpClient: HttpClient
        get() = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) { jackson() }
        }

    fun NettyApplicationEngine.port() =
        runBlocking { resolvedConnectors() }.first { it.type == ConnectorType.HTTP }.port
*/

