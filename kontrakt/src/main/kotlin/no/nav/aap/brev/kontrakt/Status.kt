package no.nav.aap.brev.kontrakt

enum class Status {
    /**
     * Initiell prosessering, som å hente data fra Sanity,
     * er utført, og brevet kan redigeres av saksbehandler.
     */
    UNDER_ARBEID,

    /**
     * Brevet er ferdigstilt enten ved manuell ferdigstilling, eller
     * automatisk, spesifisert i bestillingen. Brevet er eller vil bli
     * journalført og distribuert automatisk i denne statusen.
     */
    FERDIGSTILT,

    /**
     * Brevbestillingen er manuelt avbrutt.
     */
    AVBRUTT,
}
