package no.nav.aap.brev.innhold

import no.nav.aap.brev.kontrakt.BlokkInnhold
import no.nav.aap.brev.kontrakt.Brev

fun Brev.harFaktagrunnlag(): Boolean = alleFaktagrunnlag().isNotEmpty()

fun Brev.alleFaktagrunnlag(): List<BlokkInnhold.Faktagrunnlag> =
    tekstbolker
        .flatMap { it.innhold }
        .flatMap { it.blokker }
        .flatMap { it.innhold }
        .filterIsInstance<BlokkInnhold.Faktagrunnlag>()

fun Brev.kjenteFaktagrunnlag(): List<KjentFaktagrunnlag> =
    alleFaktagrunnlag().mapNotNull { it.kjentFaktagrunnlag() }

fun BlokkInnhold.Faktagrunnlag.kjentFaktagrunnlag(): KjentFaktagrunnlag? =
    KjentFaktagrunnlag.entries.find { it.name == tekniskNavn.uppercase() }

fun Brev.erFullstendig(): Boolean =
    tekstbolker
        .flatMap { it.innhold }
        .all { it.erFullstendig }

fun Brev.kanRedigeres(): Boolean =
    tekstbolker
        .flatMap { it.innhold }
        .any { it.kanRedigeres }

fun Brev.endreBlokkInnhold(endring: (BlokkInnhold) -> BlokkInnhold): Brev {
    return copy(tekstbolker = tekstbolker.map { tekstbolk ->
        tekstbolk.copy(innhold = tekstbolk.innhold.map { innhold ->
            innhold.copy(blokker = innhold.blokker.map { blokk ->
                blokk.copy(innhold = blokk.innhold.map { blokkInnhold ->
                    endring(blokkInnhold)
                })
            })
        })
    })
}
