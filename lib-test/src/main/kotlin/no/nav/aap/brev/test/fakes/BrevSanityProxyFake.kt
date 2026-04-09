package no.nav.aap.brev.test.fakes

import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.nav.aap.brev.innhold.KjentFaktagrunnlag
import no.nav.aap.brev.kontrakt.Blokk
import no.nav.aap.brev.kontrakt.BlokkInnhold.Faktagrunnlag
import no.nav.aap.brev.kontrakt.BlokkInnhold.FormattertTekst
import no.nav.aap.brev.kontrakt.BlokkType
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Formattering
import no.nav.aap.brev.kontrakt.Innhold
import no.nav.aap.brev.kontrakt.Tekstbolk
import no.nav.aap.brev.test.BrevmalBuilder
import java.util.UUID

fun brev(
    medFaktagrunnlag: List<String> = emptyList(),
    kanSendesAutomatisk: Boolean = false,
    kanOverstyreBrevtittel: Boolean = false,
    kanRedigeres: Boolean = true,
    erFullstendig: Boolean = false,
): Brev {
    return Brev(
        kanSendesAutomatisk = kanSendesAutomatisk,
        overskrift = "Overskrift - Brev",
        kanOverstyreBrevtittel = kanOverstyreBrevtittel,
        journalpostTittel = "Journalpost - tittel",
        tekstbolker = listOf(
            Tekstbolk(
                id = UUID.randomUUID(),
                overskrift = "Overskrift - Tekstbolk", innhold = listOf(
                    Innhold(
                        id = UUID.randomUUID(),
                        overskrift = "Overskrift - Innhold",
                        blokker = listOf(
                            Blokk(
                                id = UUID.randomUUID(),
                                innhold = listOf(
                                    FormattertTekst(
                                        id = UUID.randomUUID(),
                                        tekst = "Formattert",
                                        formattering = listOf(
                                            Formattering.UNDERSTREK,
                                            Formattering.KURSIV,
                                            Formattering.FET
                                        )
                                    )
                                ).plus(medFaktagrunnlag.map {
                                    Faktagrunnlag(
                                        id = UUID.randomUUID(),
                                        visningsnavn = it,
                                        tekniskNavn = it,
                                    )
                                }),
                                type = BlokkType.AVSNITT
                            )
                        ),
                        kanRedigeres = kanRedigeres,
                        erFullstendig = erFullstendig
                    )
                )
            )
        )
    )
}

fun Application.brevSanityProxyFake() {
    applicationFakeFelles("brev-sanity-proxy")
    routing {
        get("/api/mal") {
            val brevtype = Brevtype.valueOf(checkNotNull(call.queryParameters.get("brevtype")))
            // Gir ulike konfigurasjoner av brev for forskjellige test-scenario
            val brev = when (brevtype) {
                Brevtype.KLAGE_AVVIST -> brev(
                    kanSendesAutomatisk = true,
                    kanRedigeres = false,
                    medFaktagrunnlag = listOf("fakta"),
                    erFullstendig = true,
                )

                Brevtype.KLAGE_OPPRETTHOLDELSE -> brev(
                    kanSendesAutomatisk = true,
                    kanRedigeres = true,
                    erFullstendig = true,
                )

                Brevtype.KLAGE_TRUKKET -> brev(
                    kanSendesAutomatisk = true,
                    erFullstendig = false,
                )

                Brevtype.VARSEL_OM_BESTILLING -> brev(
                    medFaktagrunnlag = emptyList(),
                    kanSendesAutomatisk = true,
                    kanRedigeres = false,
                    erFullstendig = true
                )

                Brevtype.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT -> brev(
                    medFaktagrunnlag = listOf(KjentFaktagrunnlag.FRIST_DATO_11_7.name),
                    kanRedigeres = false,
                    erFullstendig = true
                )

                Brevtype.INNVILGELSE,
                Brevtype.VEDTAK_UTVID_VEDTAKSLENGDE,
                Brevtype.VEDTAK_11_17,
                Brevtype.VEDTAK_11_18,
                Brevtype.AVSLAG,
                Brevtype.VEDTAK_ENDRING,
                Brevtype.BARNETILLEGG_SATS_REGULERING,
                Brevtype.KLAGE_MOTTATT,
                Brevtype.FORVALTNINGSMELDING,
                Brevtype.VEDTAK_11_7,
                Brevtype.VEDTAK_11_9,
                Brevtype.VEDTAK_11_23_SJETTE_LEDD,
                Brevtype.OMGJØRING_VEDTAK_11_9,
                Brevtype.FORHÅNDSVARSEL_KLAGE_FORMKRAV,
                Brevtype.STANS_AV_YTELSE,
                Brevtype.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_MEDLEMSKAP,
                Brevtype.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_3,
                Brevtype.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_4,
                Brevtype.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_12,
                Brevtype.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_26,
                Brevtype.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_27,
                    -> brev()
            }
            call.respond(brev)
        }
        get("/api/brevmal") {
            val brevtype = Brevtype.valueOf(checkNotNull(call.queryParameters.get("brevtype")))
            val brevmal = when (brevtype) {
                Brevtype.FORVALTNINGSMELDING,
                Brevtype.VARSEL_OM_BESTILLING -> BrevmalBuilder.builder {
                    kanSendesAutomatisk = true
                    delmal {
                        obligatorisk = true
                    }
                }

                else -> {
                    BrevmalBuilder.builder {
                        kanSendesAutomatisk = false
                        delmal {
                            obligatorisk = false
                        }
                        delmal {
                            faktagrunnlag(KjentFaktagrunnlag.AAP_FOM_DATO.name)
                            valg {
                                obligatorisk = true
                                alternativ("kategori_1", listOf(KjentFaktagrunnlag.BEREGNINGSTIDSPUNKT.name))
                                alternativ("kategori_2", emptyList())
                            }
                            betingetTekst(
                                listOf("kategori_1"), listOf(KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_1_AARSTALL.name)
                            )
                            fritekst()
                        }
                    }
                }
            }
            call.respond(brevmal)
        }

        post("/api/pdf") {
            call.respond(ByteArray(0))
        }
    }
}
