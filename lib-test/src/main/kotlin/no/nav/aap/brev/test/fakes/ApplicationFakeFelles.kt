package no.nav.aap.brev.test.fakes

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import no.nav.aap.brev.api.ErrorResponse

fun Application.applicationFakeFelles(navn: String) {
    val log = log
    install(ContentNegotiation) {
        jackson()
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            log.info("$navn :: Ukjent feil ved kall til '${call.request.local.uri}'", cause)
            call.respond(status = HttpStatusCode.InternalServerError, message = ErrorResponse(cause.message))
        }
    }
}
