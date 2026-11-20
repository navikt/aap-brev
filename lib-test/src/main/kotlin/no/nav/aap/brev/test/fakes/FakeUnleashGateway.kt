package no.nav.aap.brev.test.fakes

import no.nav.aap.brev.unleash.FeatureToggle
import no.nav.aap.brev.unleash.UnleashGateway
import java.util.concurrent.ConcurrentHashMap

class FakeUnleashGateway(vararg initiallyEnabled: FeatureToggle) : UnleashGateway {

    private val enabled = ConcurrentHashMap.newKeySet<FeatureToggle>().apply { addAll(initiallyEnabled) }

    override fun isEnabled(featureToggle: FeatureToggle): Boolean = featureToggle in enabled

    fun enable(vararg featureToggles: FeatureToggle) {
        enabled.addAll(featureToggles)
    }
}
