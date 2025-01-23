package no.nav.aap.brev.innhold

import no.nav.aap.brev.kontrakt.BlokkInnhold
import no.nav.aap.brev.kontrakt.Brev

fun Brev.harFaktagrunnlag(): Boolean = finnFaktagrunnlag().isNotEmpty()

fun Brev.finnFaktagrunnlag(): List<BlokkInnhold.Faktagrunnlag> =
    tekstbolker
        .flatMap { it.innhold }
        .flatMap { it.blokker }
        .flatMap { it.innhold }
        .filterIsInstance<BlokkInnhold.Faktagrunnlag>()

fun Brev.erFullstendig(): Boolean =
    tekstbolker
        .flatMap { it.innhold }
        .all { it.erFullstendig }

fun Brev.kanRedigeres(): Boolean =
    tekstbolker
        .flatMap { it.innhold }
        .any { it.kanRedigeres }
