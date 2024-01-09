import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import java.net.URLEncoder
import java.nio.charset.Charset

internal const val VERSION = "v2022-03-07"
internal const val DATASET = "production"
//internal const val PERSPECTIVE = "published"

internal class SanityClient(
    private val config: SanityConfig,
    private val client: HttpClient = defaultClient(),
) {
    suspend fun brevmal(id: String): Result<SanityModel.Brevmal> {
        val query = GROQ.BREVMAL
        val variables = GROQ.variables("ID" to id)

        return runCatching {
            get("${config.host}/$VERSION/data/query/$DATASET?query=$query&$variables")
                .body<SanityModel.SingleResponse>()
                .result!!
        }
    }

    suspend fun brevmaler(): Result<List<SanityModel.Brevmal>> {
        val query = GROQ.ALLE_BREVMALER

        return runCatching {
            get("${config.host}/$VERSION/data/query/$DATASET?query=$query")
                .body<SanityModel.Response>()
                .result!!
        }
    }

    suspend fun get(queryURL: String): HttpResponse =
        client.get(queryURL) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(config.token)
        }

    companion object {
        fun defaultClient() = HttpClient(CIO) {
            install(Logging) {
                level = LogLevel.BODY
                logger = object : Logger {
                    override fun log(message: String) = SECURE_LOGGER.info(message)
                }
            }
            install(ContentNegotiation) {
                jackson {
                    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    registerModule(JavaTimeModule())
                }
            }
            install(HttpRequestRetry) {

            }
            install(HttpTimeout) {
                requestTimeoutMillis = 25_000
                connectTimeoutMillis = 5_000
            }
        }
    }
}

object SanityModel {

    data class SingleResponse(
        val result: Brevmal? = null,
        val error: Map<String, Any>? = null,
    )

    data class Response(
        val result: List<Brevmal>? = null,
        val error: Map<String, Any>? = null,
    )

    data class Brevmal(
        val brevtittel: String,
        val _id: String,
        val brevtype: String,
        val innhold: List<Innhold>? = null,
    )

    data class Innhold(
        val _id: String,
        val _type: Type,
        val overskrift: String? = null,
        val niva: Header? = null,
        val systemNokkel: String? = null,
        val innhold: List<PortableText>? = null,
        val kanRedigeres: Boolean? = null,
        val hjelpetekst: List<PortableText>? = null,
    ) {
        enum class Header { H1, H2, H3 }
        enum class Type { systeminnhold, standardtekst }
    }

    data class PortableText(
        val _type: Type,
        val _key: String,
        val children: List<Child>,
        val markDefs: List<Map<String, Any>>? = null,
        val listItem: String? = null,
        val style: Element? = null,
        val level: Int? = null,
    ) {
        data class Child(
            val _type: Type,
            val _key: String,
            val _ref: String? = null,
            val text: String? = null,
            val marks: List<Mark>? = null,
            val systemVariabel: String? = null,
        ) {
            enum class Type { span, inlineElement, systemVariabel }
            enum class Mark { strong, em, underline }
        }

        enum class Type { content, contentUtenVariabler }
        enum class Element { normal }
    }
}


private object GROQ {
    private const val ALLE_BREVMALER_QUERY = """
        *[_type=='brev']{
          brevtittel,
          _id,
          brevtype
        }
    """

    private const val ID = "\$ID"

    private const val FIRST_BREVMAL_BY_ID_QUERY = """
        *[_id == $ID][0]{
          brevtittel,
          _id,
          brevtype,
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
        }
    """

    private fun encode(str: String): String = URLEncoder.encode(str, Charset.forName("UTF-8"))

    val ALLE_BREVMALER = encode(ALLE_BREVMALER_QUERY)
    val BREVMAL = encode(FIRST_BREVMAL_BY_ID_QUERY)

    fun variables(vararg variables: Pair<String, String>): String =
        variables.joinToString("&") { (key, value) ->
            encode("\$" + key) + """="$value""""
        }
}

internal object Domain {
    fun from(brev: SanityModel.Brevmal): Brevmal = Brevmal.from(brev)

    data class Brevmal(
        val id: String,
        val tittel: String,
        val systeminnhold: List<Systeminnhold>?,
        val standardtekster: List<Standardtekst>?,
    ) {
        companion object {
            fun from(brev: SanityModel.Brevmal): Brevmal {
                return Brevmal(
                    id = brev._id,
                    tittel = brev.brevtittel,
                    systeminnhold = brev.innhold?.mapNotNull(Systeminnhold::from),
                    standardtekster = brev.innhold?.mapNotNull(Standardtekst::from),
                )
            }
        }
    }

    data class Systeminnhold(
        val id: String,
        val systemnøkkel: String,
    ) {
        companion object {
            fun from(innhold: SanityModel.Innhold): Systeminnhold? {
                if (innhold._type != SanityModel.Innhold.Type.systeminnhold) {
                    return null
                }

                return Systeminnhold(
                    id = innhold._id,
                    systemnøkkel = requireNotNull(innhold.systemNokkel),
                )
            }
        }
    }

    data class Standardtekst(
        val id: String,
        val innhold: List<TextContent>,
        val overskriftsnivå: Header?,
        val overskrift: String?,
        val redigerbar: Boolean,
        val hjelpetekst: List<TextContent>,
    ) {
        companion object {
            fun from(innhold: SanityModel.Innhold): Standardtekst? {
                if (innhold._type != SanityModel.Innhold.Type.standardtekst) {
                    return null
                }

                return Standardtekst(
                    id = innhold._id,
                    innhold = innhold.innhold?.map(TextContent::from) ?: emptyList(),
                    overskriftsnivå = innhold.niva?.let { Header.valueOf(it.name) },
                    overskrift = innhold.overskrift,
                    redigerbar = innhold.kanRedigeres ?: false,
                    hjelpetekst = innhold.hjelpetekst?.map(TextContent::from) ?: emptyList(),
                )
            }
        }
    }

    enum class Header { H1, H2, H3 }

    data class TextContent(
        val spans: List<Span>,
        val inlineElements: List<InlineElement>,
        val systemVariables: List<SystemVariable>,
        val listItem: String?,
        val style: String?, // normal
        val level: Int,
    ) {
        companion object {
            fun from(text: SanityModel.PortableText): TextContent {
                if (text._type != SanityModel.PortableText.Type.content) {
                    PUBLIC_LOGGER.warn(
                        """
                            standardtekst.innhold.type was ${text._type} and has no mapper on its own, 
                            reuses mapper of type 'content'.
                        """.trimIndent()
                    )
                }

                return TextContent(
                    spans = text.children.mapNotNull(Span::from),
                    inlineElements = text.children.mapNotNull(InlineElement::from),
                    systemVariables = text.children.mapNotNull(SystemVariable::from),
                    listItem = text.listItem,
                    style = text.style?.name,
                    level = text.level ?: -1,
                )
            }
        }
    }

    enum class Mark { bold, italic, underline }

    data class Span(
        val text: String,
        val marks: List<Mark>,
    ) {
        companion object {
            fun from(child: SanityModel.PortableText.Child): Span? {
                if (child._type != SanityModel.PortableText.Child.Type.span) {
                    return null
                }

                return Span(
                    text = child.text!!,
                    marks = child.marks?.map {
                        when (it) {
                            SanityModel.PortableText.Child.Mark.strong -> Mark.bold
                            SanityModel.PortableText.Child.Mark.em -> Mark.italic
                            SanityModel.PortableText.Child.Mark.underline -> Mark.underline
                        }
                    } ?: emptyList(),
                )
            }
        }
    }

    data class InlineElement(
        val text: String,
    ) {
        companion object {
            fun from(child: SanityModel.PortableText.Child): InlineElement? {
                if (child._type != SanityModel.PortableText.Child.Type.inlineElement) {
                    return null
                }

                return InlineElement(
                    text = child.text!!,
                )
            }
        }
    }

    data class SystemVariable(
        val _ref: String,
        val systemVariabel: String,
    ) {
        companion object {
            fun from(child: SanityModel.PortableText.Child): SystemVariable? {
                if (child._type != SanityModel.PortableText.Child.Type.systemVariabel) {
                    return null
                }

                return SystemVariable(
                    _ref = child._ref!!,
                    systemVariabel = child.systemVariabel!!,
                )
            }
        }
    }

}
