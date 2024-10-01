package no.nav.aap.brev.domene

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.UUID

@JvmInline
value class BrevbestillingReferanse(@PathParam("referanse") val referanse: UUID)