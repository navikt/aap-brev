package no.nav.aap.brev

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import no.nav.aap.brev.exception.ValideringsfeilException
import no.nav.aap.komponenter.httpklient.exception.ApiErrorCode
import no.nav.aap.komponenter.httpklient.exception.ApiException
import no.nav.aap.komponenter.httpklient.exception.InternfeilException
import org.slf4j.LoggerFactory
import java.net.http.HttpTimeoutException

object StatusPagesConfigHelper {
    fun setup(): StatusPagesConfig.() -> Unit = {
        exception<Throwable> { call, cause ->
            val logger = LoggerFactory.getLogger(javaClass)

            when (cause) {
                /**
                 * TODO:
                 *   Bytt ut bruk av ValideringsfeilException med ApiException
                 *   - UgyldigForespørselException hvis det er noe bruker kan gjøre for å rette opp i feilen
                 *   - InternfeilException hvis det er en systemfeil / uhåndtert feil
                 **/
                is ValideringsfeilException -> {
                    logger.warn(cause.message, cause)
                    call.respondWithError(
                        ApiException(
                            status = HttpStatusCode.BadRequest,
                            message = cause.message ?: "Valideringsfeil",
                            code = ApiErrorCode.UGYLDIG_FORESPØRSEL
                        )
                    )
                }

                is HttpTimeoutException -> {
                    logger.warn((cause.cause?.message ?: cause.message), cause)
                    call.respondWithError(
                        ApiException(
                            status = HttpStatusCode.RequestTimeout,
                            message = "Forespørselen tok for lang tid. Prøv igjen om litt."
                        )
                    )
                }

                is InternfeilException -> {
                    logger.error(cause.cause?.message ?: cause.message)
                    call.respondWithError(cause)
                }

                is ApiException -> {
                    logger.warn(cause.message, cause)
                    call.respondWithError(cause)
                }

                else -> {
                    logger.error(
                        "Ukjent feil ved kall til '{}'. Type: ${cause.javaClass}. Message: ${cause.message}",
                        call.request.local.uri,
                        cause
                    )
                    call.respondWithError(InternfeilException("En ukjent feil oppsto"))
                }
            }
        }

        status(HttpStatusCode.NotFound) { call, _ ->
            call.application.log.error("Fikk kall mot endepunkt som ikke finnes: ${call.request.local.uri}")

            call.respondWithError(
                ApiException(
                    status = HttpStatusCode.NotFound,
                    message = "Kunne ikke nå endepunkt: ${call.request.local.uri}",
                    code = ApiErrorCode.ENDEPUNKT_IKKE_FUNNET
                )
            )
        }
    }

    private suspend fun ApplicationCall.respondWithError(exception: ApiException) {
        respond(
            exception.status,
            exception.tilApiErrorResponse()
        )
    }
}