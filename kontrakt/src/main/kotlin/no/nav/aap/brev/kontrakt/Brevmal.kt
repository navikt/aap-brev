package no.nav.aap.brev.kontrakt

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName

data class Brevmal(
    val _id: String,
    val overskrift: String,
    val delmaler: List<ValgtDelmal>
) {

    data class ValgtDelmal(
        val delmal: Delmal,
        val obligatorisk: Boolean
    )

    data class Delmal(
        val _id: String,
        val tittel: String,
        val teksteditor: List<TeksteditorElement>
    )

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_type", visible = true)
    sealed interface TeksteditorElement {

        @JsonTypeName("block")
        data class Block(
            val children: List<BlockChildren>
        ) : TeksteditorElement

        @JsonTypeName("valgRef")
        data class Valg(
            val obligatorisk: Boolean,
            val valg: ValgAlternativer
        ) : TeksteditorElement

        @JsonTypeName("periodetekstRef")
        data class Periodetekst(
            // grupper her også?

            // constraint på at fom- og/eller tom-dato må finnes? Hvis det gjøres her bør deserialisering prøves
            // før lagring av json fra Sanity slik at det kan feile tidlig.

            val periodetekst: Teksteditor
        ) : TeksteditorElement

        @JsonTypeName("betingetTekstRef")
        data class BetingetTekst(
            val grupper: List<Gruppe>,
            val tekst: Teksteditor,
        ) : TeksteditorElement
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_type", visible = true)
    sealed interface BlockChildren {

        @JsonTypeName("span")
        object Span : BlockChildren

        @JsonTypeName("faktagrunnlag")
        data class Faktagrunnlag(
            val _id: String,
            val datatype: String,
            val tekniskNavn: String,
            val visningsnavn: String
        ) : BlockChildren
    }

    data class ValgAlternativer(
        val _id: String,
        val valg: List<ValgAlternativ>
    )

    data class Teksteditor(
        val _id: String,
        val teksteditor: List<TeksteditorElement.Block>
    )

    data class Gruppe(
        val _id: String,
        val gruppenavn: String,
        val tekniskNavn: String
    )

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_type", visible = true)
    sealed interface ValgAlternativ {

        @JsonTypeName("fritekst")
        object Fritekst : ValgAlternativ

        @JsonTypeName("gruppertTekstRef")
        data class GruppertTekst(
            val tekst: Teksteditor,
            val gruppe: Gruppe?,
        ) : ValgAlternativ
    }
}
