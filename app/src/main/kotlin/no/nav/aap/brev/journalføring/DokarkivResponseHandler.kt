package no.nav.aap.brev.journalføring

import no.nav.aap.komponenter.httpklient.httpclient.error.BadRequestHttpResponsException
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.error.InternalServerErrorHttpResponsException
import no.nav.aap.komponenter.httpklient.httpclient.error.ManglerTilgangException
import no.nav.aap.komponenter.httpklient.httpclient.error.RestResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.error.UhåndtertHttpResponsException
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class DokarkivResponseHandler : RestResponseHandler<InputStream> {

    private val log = LoggerFactory.getLogger(DokarkivResponseHandler::class.java)

    private val defaultResponseHandler = DefaultResponseHandler()

    override fun bodyHandler(): HttpResponse.BodyHandler<InputStream> {
        return defaultResponseHandler.bodyHandler()
    }

    override fun <R> håndter(
        request: HttpRequest,
        response: HttpResponse<InputStream>,
        mapper: (InputStream, HttpHeaders) -> R
    ): R? {
        val status: Int = response.statusCode()
        if (status == HttpURLConnection.HTTP_NO_CONTENT) {
            return null
        }

        if ((status >= HttpURLConnection.HTTP_OK && status < HttpURLConnection.HTTP_MULT_CHOICE) ||
            status == HttpURLConnection.HTTP_CONFLICT
        ) {
            if (status == HttpURLConnection.HTTP_CONFLICT) {
                log.warn("Fikk http status kode ${HttpURLConnection.HTTP_CONFLICT}. Forsøker å tolke response som forventet.")
            }
            return mapper(response.body(), response.headers())
        }

        val responseBody = response.body().bufferedReader().use { it.readText() }

        if (status == HttpURLConnection.HTTP_BAD_REQUEST) {
            throw BadRequestHttpResponsException("$response :: $responseBody")
        }

        if (status >= HttpURLConnection.HTTP_INTERNAL_ERROR && status < HttpURLConnection.HTTP_GATEWAY_TIMEOUT) {
            throw InternalServerErrorHttpResponsException("$response :: $responseBody")
        }

        if (status == HttpURLConnection.HTTP_FORBIDDEN) {
            throw ManglerTilgangException("$response :: $responseBody")
        }

        if (status == HttpURLConnection.HTTP_NOT_FOUND) {
            throw IkkeFunnetException("$response :: $responseBody")
        }

        throw UhåndtertHttpResponsException("Uventet HTTP-responskode $response")
    }
}
