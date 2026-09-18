package no.nav.aap.brev.unleash

import io.getunleash.DefaultUnleash
import io.getunleash.util.UnleashConfig
import no.nav.aap.komponenter.config.requiredConfigForKey

object UnleashGatewayImpl : UnleashGateway {
    private val unleash by lazy {
        DefaultUnleash(
            UnleashConfig
                .builder()
                .appName(requiredConfigForKey("nais.app.name"))
                .unleashAPI("${requiredConfigForKey("unleash.server.api.url")}/api")
                .apiKey(requiredConfigForKey("unleash.server.api.token"))
                .build()
        )
    }

    override fun isEnabled(featureToggle: FeatureToggle): Boolean {
        return unleash.isEnabled(featureToggle.key())
    }

}
