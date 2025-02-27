package no.nav.aap.brev.test.fakes

import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.FaktagrunnlagType
import no.nav.aap.brev.kontrakt.Blokk
import no.nav.aap.brev.kontrakt.BlokkInnhold.Faktagrunnlag
import no.nav.aap.brev.kontrakt.BlokkInnhold.FormattertTekst
import no.nav.aap.brev.kontrakt.BlokkType
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Formattering
import no.nav.aap.brev.kontrakt.Innhold
import no.nav.aap.brev.kontrakt.Tekstbolk
import java.util.UUID
import kotlin.Boolean
import kotlin.String

fun brev(
    medFaktagrunnlag: List<String> = emptyList(),
    kanRedigeres: Boolean = true,
    erFullstendig: Boolean = false,
): Brev {
    return Brev(
        overskrift = "Overskrift - Brev",
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
                Brevtype.AVSLAG -> brev()
                Brevtype.VARSEL_OM_BESTILLING -> brev(
                    medFaktagrunnlag = emptyList(),
                    kanRedigeres = false,
                    erFullstendig = true
                )

                Brevtype.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT -> brev(
                    medFaktagrunnlag = listOf(FaktagrunnlagType.FRIST_DATO_11_7.verdi),
                    kanRedigeres = false,
                    erFullstendig = true
                )

                Brevtype.FORVALTNINGSMELDING -> brev()
            }
            call.respond(brev)
        }
    }
}
