package no.nav.aap.brev.kontrakt

enum class Status {
    /**
     * Brevbestillingen er mottatt og registrert. Ikke relevant for V2 API
     */
    REGISTRERT,

    /**
     * Initiell prosessering, som å hente data fra Sanity,
     * er utført, og brevet kan redigeres av saksbehandler.
     */
    UNDER_ARBEID,

    /**
     * Brevet er ferdig redigert av saksbehandler og kan
     * journalføres og distribueres. Eventuelt har bestillingen
     * gått direkte til denne statusen eller fra status `REGISTRERT`,
     * dersom det ikke er behov for manuell redigering av saksbehandler.
     */
    FERDIGSTILT,

    /**
     * Brevbestillingen er manuelt avbrutt
     */
    AVBRUTT,
}
