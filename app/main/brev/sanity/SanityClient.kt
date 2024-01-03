package brev.sanity

import brev.HttpClientFactory
import brev.SanityConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.nio.charset.Charset
import java.net.URLEncoder

internal const val VERSION = "v2023-19-12"
internal const val DATASET = "production"
internal const val PERSPECTIVE = "published"

class SanityClient(
    private val config: SanityConfig,
    private val client: HttpClient = HttpClientFactory.create()
) {
    suspend fun brevmal(id: String): Result<Map<String, Any>> =
        query(encode(GROQ.FIRST_BREVMAL_BY_ID + "&${GROQ.ID}=$id")) // &$ID=$id
            .map { it.result }

    suspend fun brevmaler(): Result<Map<String, Any>> =
        query(encode(GROQ.ALLE_BREVMALER))
            .map { it.result }

    /** Queries lower than 11kB */
    private suspend fun query(query: String): Result<Response> =
        runCatching {
            client.get("${config.host}/$VERSION/data/query/$DATASET?perspective=$PERSPECTIVE&query=$query") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(config.token)
            }.body()
        }
}

//@JvmInline
//value
data class Response(
    // val ms: String,
    // val query: String,
    val result: Map<String, Any>,
)

private fun encode(str: String): String = URLEncoder.encode(str, Charset.forName("UTF-8"))

private object GROQ {
    const val ALLE_BREVMALER = """
        "*[_type=='brev']{
          brevtittel,
          _id,
          brevtype
        }"
    """

    // workaround to escape kotlins reserved $ sign
    const val ID = "\$ID"

    // groq variables are submitted as url-params prefixed with '$', e.g. $id=123
    const val FIRST_BREVMAL_BY_ID = """
        "*[_id == "$ID"][0]{
          brevtittel,
          innhold[] -> {
            _type,
            _id,
            _type == 'systeminnhold' => {
              systemNokkel,
              overskrift,
              "niva": niva->.level
            },
            _type == 'standardtekst' => {
              overskrift,
              "niva": niva->.level,
              hjelpetekst,
              kanRedigeres,
                innhold[]{
                _type == 'content' => {
                  ...,
                  children[] {
                    ...,
                    _type == 'systemVariabel' => {
                      ...,
                      "systemVariabel": @->.tekniskNavn
                    },
                    _type == 'inlineElement' => {
                      ...,
                      "text": @->.tekst
                    },
                  }
                }
              }
            }
          }
        }"
    """
}
