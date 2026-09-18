package no.nav.aap.brev.unleash

import no.nav.aap.komponenter.gateway.Gateway

interface UnleashGateway : Gateway {
    fun isEnabled(featureToggle: FeatureToggle): Boolean
}
