package no.nav.aap.brev.bestilling

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName

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
        val overskrift: String,
        val teksteditor: List<TeksteditorElement>
    ) : Document

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_type", visible = true)
    sealed interface TeksteditorElement : ArrayElement {

        @JsonTypeName("block")
        data class Block(
            override val _key: String,
            val children: List<BlockChildren>
        ) : TeksteditorElement

        @JsonTypeName("valgRef")
        data class Valg(
            override val _key: String,
            val obligatorisk: Boolean,
            val valg: ValgAlternativer
        ) : TeksteditorElement

        @JsonTypeName("periodetekstRef")
        data class Periodetekst(
            // grupper her også?

            // constraint på at fom- og/eller tom-dato må finnes? Hvis det gjøres her bør deserialisering prøves
            // før lagring av json fra Sanity slik at det kan feile tidlig.

            override val _key: String,
            val periodetekst: Teksteditor
        ) : TeksteditorElement

        @JsonTypeName("betingetTekstRef")
        data class BetingetTekst(
            override val _key: String,
            val kategorier: List<Kategori>,
            val tekst: Teksteditor,
        ) : TeksteditorElement

        @JsonTypeName("fritekst")
        data class Fritekst(override val _key: String) : TeksteditorElement
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
        val teksteditor: List<TeksteditorElement.Block>
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