package no.nav.aap.brev.organisasjon

data class Enhet(val enhetsNummer: String, val navn: String, val type: EnhetsType)

enum class EnhetsType {
    LOKAL, ARBEID_OG_YTELSE, ANNET
}