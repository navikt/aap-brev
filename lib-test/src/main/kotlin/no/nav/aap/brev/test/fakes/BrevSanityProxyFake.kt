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
import no.nav.aap.brev.kontrakt.Brevmal
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Formattering
import no.nav.aap.brev.kontrakt.Innhold
import no.nav.aap.brev.kontrakt.Tekstbolk
import no.nav.aap.brev.test.FileUtils
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
                Brevtype.INNVILGELSE -> brev()
                Brevtype.VEDTAK_11_18 -> brev()
                Brevtype.AVSLAG -> brev()
                Brevtype.VEDTAK_ENDRING -> brev()
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

                Brevtype.FORHÅNDSVARSEL_KLAGE_FORMKRAV -> brev()
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

                Brevtype.FORVALTNINGSMELDING -> brev()
                Brevtype.VEDTAK_11_7 -> brev()
                Brevtype.VEDTAK_11_9 -> brev()
            }
            call.respond(brev)
        }
        get("/api/brevmal") {
            val brevmal = FileUtils.lesFilTilJson<Brevmal>("brevmal.json")
            call.respond(brevmal)
        }

        post("/api/pdf") {
            call.respond(ByteArray(0))
        }
    }
}
