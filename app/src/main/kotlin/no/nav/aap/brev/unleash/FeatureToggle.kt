package no.nav.aap.brev.unleash
interface FeatureToggle {
    fun key(): String
}

enum class BrevFeature : FeatureToggle {
    // Eksempel på feature toggle. Kan fjernes når det legges til nye.
    // Se: https://aap-unleash-web.iap.nav.cloud.nais.io/projects/default
    BrevTest,
    ;

    override fun key(): String = name
}
