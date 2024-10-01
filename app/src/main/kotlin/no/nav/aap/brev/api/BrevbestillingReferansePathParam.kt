package no.nav.aap.brev.api

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.brev.domene.BrevbestillingReferanse
import java.util.UUID

data class BrevbestillingReferansePathParam(@PathParam("referanse") val referanse: UUID) {
    val brevbestillingReferanse: BrevbestillingReferanse = BrevbestillingReferanse(referanse)
}