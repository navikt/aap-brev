package no.nav.aap.brev.bestilling

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.brev.bestilling.Brevmal.TeksteditorElement.DelmalTeksteditorElement

data class Brevmal(
    override val _id: String,
    val overskrift: String,
    val journalposttittel: String,
    val kanSendesAutomatisk: Boolean,
    val delmaler: List<DelmalValg>
) : Document {

    data class DelmalValg(
        override val _key: String,
        val delmal: Delmal,
        val obligatorisk: Boolean
    ) : ArrayElement

    data class Delmal(
        override val _id: String,
        val overskrift: String?,
        val teksteditor: List<DelmalTeksteditorElement>
    ) : Document

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_type", visible = true)
    sealed interface TeksteditorElement : ArrayElement {

        sealed interface EnkelTeksteditorElement : TeksteditorElement
        sealed interface DelmalTeksteditorElement : TeksteditorElement

        @JsonTypeName("block")
        data class Block(
            override val _key: String,
            val children: List<BlockChildren>
        ) : EnkelTeksteditorElement, DelmalTeksteditorElement

        @JsonTypeName("valgRef")
        data class Valg(
            override val _key: String,
            val obligatorisk: Boolean,
            val valg: ValgAlternativer
        ) : DelmalTeksteditorElement

        @JsonTypeName("betingetTekstRef")
        data class BetingetTekst(
            override val _key: String,
            val kategorier: List<Kategori>?,
            val tekst: Teksteditor,
        ) : DelmalTeksteditorElement

        @JsonTypeName("fritekst")
        data class Fritekst(override val _key: String) : DelmalTeksteditorElement

        @JsonTypeName("tabell")
        data class Tabell(
            override val _key: String,
            val tekniskNavn: String,
            val kolonner: List<Kolonne>,
        ) : EnkelTeksteditorElement, DelmalTeksteditorElement {
            data class Kolonne(
                val overskrift: String,
                val tekniskNavn: String,
            )
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_type", visible = true)
    sealed interface BlockChildren : ArrayElement {

        @JsonTypeName("span")
        data class Span(override val _key: String) : BlockChildren

        @JsonTypeName("faktagrunnlag")
        data class Faktagrunnlag(
            override val _key: String,
            override val _id: String,
            val datatype: String,
            val tekniskNavn: String,
            val visningsnavn: String
        ) : Document, BlockChildren
    }

    data class ValgAlternativer(
        override val _id: String,
        val alternativer: List<ValgAlternativ>
    ) : Document

    data class Teksteditor(
        override val _id: String,
        val teksteditor: List<TeksteditorElement.EnkelTeksteditorElement>
    ) : Document

    data class Kategori(
        override val _id: String,
        val visningsnavn: String,
        val tekniskNavn: String
    ) : Document

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_type", visible = true)
    sealed interface ValgAlternativ : ArrayElement {

        @JsonTypeName("fritekst")
        data class Fritekst(override val _key: String) : ValgAlternativ

        @JsonTypeName("kategorisertTekstRef")
        data class KategorisertTekst(
            override val _key: String,
            val tekst: Teksteditor,
            val kategori: Kategori?,
        ) : ValgAlternativ
    }
}

// Dokument i Sanity
sealed interface Document {
    val _id: String
}

// Array-element i Sanity
sealed interface ArrayElement {
    val _key: String
}