package no.nav.aap.brev.test

import no.nav.aap.brev.kontrakt.Brevmal
import no.nav.aap.brev.kontrakt.Brevmal.BlockChildren
import no.nav.aap.brev.kontrakt.Brevmal.Delmal
import no.nav.aap.brev.kontrakt.Brevmal.DelmalValg
import no.nav.aap.brev.kontrakt.Brevmal.TeksteditorElement
import no.nav.aap.brev.kontrakt.Brevmal.ValgAlternativ
import java.util.UUID.randomUUID

class BrevmalBuilder() {
    var _id = randomUUID().toString()
    var overskrift = "Overskrift"
    var journalposttittel = "Journalposttittel"
    var kanSendesAutomatisk = false
    var delmaler = mutableListOf<DelmalValg>()

    fun delmal(init: DelmalBuilder.() -> Unit): DelmalValg {
        val builder = DelmalBuilder()
        builder.init()
        val delmalValg =
            DelmalValg(
                _key = builder._key,
                delmal = Delmal(
                    _id = builder._id,
                    overskrift = builder.overskrift,
                    teksteditor = builder.teksteditor,
                ),
                obligatorisk = builder.obligatorisk
            )
        delmaler.add(delmalValg)
        return delmalValg
    }

    companion object {
        fun builder(init: BrevmalBuilder.() -> Unit): Brevmal {
            val builder = BrevmalBuilder()
            builder.init()
            return Brevmal(
                _id = builder._id,
                overskrift = builder.overskrift,
                journalposttittel = builder.journalposttittel,
                kanSendesAutomatisk = builder.kanSendesAutomatisk,
                delmaler = builder.delmaler,
            )
        }

    }
}


class DelmalBuilder() {
    var _id = randomUUID().toString()
    var _key = randomUUID().toString()
    var obligatorisk = false
    var overskrift = "Overskrift"
    var teksteditor = mutableListOf<TeksteditorElement>()

    fun faktagrunnlag(tekniskNavn: String) {
        teksteditor.add(
            TeksteditorElement.Block(
                _key = randomUUID().toString(),
                listOf(
                    byggFaktagrunnlag(tekniskNavn)
                )
            )
        )
    }

    fun valg(init: ValgBuilder.() -> Unit): TeksteditorElement.Valg {
        val builder = ValgBuilder()
        builder.init()
        val valg = TeksteditorElement.Valg(
            _key = builder._key,
            obligatorisk = builder.obligatorisk,
            valg = builder.valg()
        )
        teksteditor.add(valg)
        return valg
    }

    fun periodetekst(faktagrunnlag: List<String>): TeksteditorElement.Periodetekst {
        val periodetekst = TeksteditorElement.Periodetekst(
            _key = randomUUID().toString(),
            periodetekst = byggTeksteditor(faktagrunnlag)
        )
        teksteditor.add(periodetekst)
        return periodetekst
    }

    fun betingetTekst(kategorier: List<String>, faktagrunnlag: List<String>): TeksteditorElement.BetingetTekst {
        val betingetTekst = TeksteditorElement.BetingetTekst(
            _key = randomUUID().toString(),
            kategorier = kategorier.map { byggKategori(it) },
            tekst = byggTeksteditor(faktagrunnlag)
        )
        teksteditor.add(betingetTekst)
        return betingetTekst
    }

    fun fritekst(): TeksteditorElement.Fritekst {
        val fritekst = TeksteditorElement.Fritekst(_key = randomUUID().toString())
        teksteditor.add(fritekst)
        return fritekst
    }
}

class ValgBuilder() {
    var _id = randomUUID().toString()
    var _key = randomUUID().toString()
    var obligatorisk = false
    val alternativer = mutableListOf<ValgAlternativ>()
    fun valg(): Brevmal.ValgAlternativer {
        return Brevmal.ValgAlternativer(_id, alternativer)
    }

    fun alternativ(kategori: String, faktagrunnlag: List<String> = emptyList()): ValgAlternativ.KategorisertTekst {
        val valgAlternativ = ValgAlternativ.KategorisertTekst(
            _key = randomUUID().toString(),
            tekst = byggTeksteditor(faktagrunnlag),
            kategori = byggKategori(kategori)
        )
        alternativer.add(valgAlternativ)
        return valgAlternativ
    }

    fun fritekst(): ValgAlternativ.Fritekst {
        val fritekst = ValgAlternativ.Fritekst(_key = randomUUID().toString())
        alternativer.add(fritekst)
        return fritekst
    }
}

private fun byggTeksteditor(faktagrunnlag: List<String>): Brevmal.Teksteditor {
    return Brevmal.Teksteditor(
        _id = randomUUID().toString(),
        teksteditor = listOf(TeksteditorElement.Block(_key = randomUUID().toString(), buildList {
            add(BlockChildren.Span(_key = randomUUID().toString()))
            faktagrunnlag.forEach {
                add(
                    BlockChildren.Faktagrunnlag(
                        _key = randomUUID().toString(),
                        _id = randomUUID().toString(),
                        datatype = "",
                        tekniskNavn = it,
                        visningsnavn = it
                    )
                )
            }
        }))
    )
}

private fun byggFaktagrunnlag(tekniskNavn: String): BlockChildren.Faktagrunnlag {
    return BlockChildren.Faktagrunnlag(
        _key = randomUUID().toString(),
        _id = randomUUID().toString(),
        datatype = "",
        tekniskNavn = tekniskNavn,
        visningsnavn = tekniskNavn
    )
}

private fun byggKategori(navn: String): Brevmal.Kategori {
    return Brevmal.Kategori(
        _id = randomUUID().toString(),
        visningsnavn = navn,
        tekniskNavn = navn
    )
}
