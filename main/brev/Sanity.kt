package brev

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.LoggerFactory

private val secureLog = LoggerFactory.getLogger("secureLog")

data class SanityConfig(
    val token: String,
    val host: String = "https://948n95rd.api.sanity.io",
)

class Sanity(
    private val config: SanityConfig,
    private val client: HttpClient = HttpClientFactory.create()
) {
    /**
     * Queries lower than 11kB can use this
     */
    suspend fun query(query: GroqQuery): Result<QueryResult> {
        val res = client.get("${config.host}/v2023-19-12/data/query/production?query=$query") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(config.token)
        }
        return try {
            Result.success(res.body<QueryResult>())

        } catch (ex: Exception) {
            Result.failure(RuntimeException(res.bodyAsText(), ex))
        }
    }

    /**
     * Queries larger than 11kB must use this
     */
    suspend fun largeQuery(query: GroqQuery, params: Map<String, String>) {
        val mappedParams = params.map { (k, v) -> """ "$k": "$v" """ }
        val res = client.post("${config.host}/v2023-19-12/data/query/production") {
            bearerAuth(config.token)
            setBody(
                """
                    {
                      "query": "$query",
                      "params": { $mappedParams }
                    }
                """
            )
        }
    }
}


data class QueryResult(
    /**
     * Server-side processing time
     */
    val ms: String,
    /**
     * Submitted query
     */
    val query: String,
    /**
     * Result can be an object, an array or null - depending on the query
     */
    val result: Map<String, Any>?,
)

data class GroqQuery(
    val query: String,
    val variables: List<String> = listOf(),
    val perspective: Perspective = Perspective.raw,
    val explain: Explain = Explain.FALSE,
    val resultSourceMap: Boolean = false,
) {
    private var projection: String? = null
    private var filter: String? = null

    override fun toString(): String =
        StringBuilder("*") // select all
            .append(filter ?: "")
            .append(projection ?: "")
            .toString()

    fun filter(str: String) {
        filter = "[ $str ]"
    }

    fun projection(str: String) {
        projection = "{ $str }"
    }

    fun sorting(): String {
        return ""
    }

    /**
     * Runs the query against the selected perspective:
     * See also [documentation article on Perspectives](https://www.sanity.io/docs/perspectives)
     */
    enum class Perspective {
        /**
         * query runs as if all draft documents and changes were published
         */
        previewDrafts,

        /**
         * query runs as if no draft documents or changes exist
         */
        published,

        /**
         * query runs without distinguishing between drafts and published documents
         */
        raw,
    }

    /**
     * Whether to include the query execution plan as plain text in an explain field, which may be useful when optimizing slow queries.
     * Note that the plan output is only advisory - the contents are not documented and subject to change without warning.
     */
    enum class Explain {
        /**
         * Include explain output in result
         */
        TRUE,

        /**
         * Only return the query plan—do not execute the query
         */
        ONLY,

        /**
         * Do not include explain output
         */
        FALSE,
    }
}

// TODO: Map sanity api response to this if error is used in front-end (brev-editor)
data class ErrorResult(
    val error: Error,
) {
    data class Error(
        /**
         * The executed query
         */
        val query: String,
        /**
         * Human-readable description of the error
         */
        val description: String,
        /**
         * Start-position in the query where the error occured
         */
        val start: Int,
        /**
         * End-position in the query where the error occured
         */
        val end: Int,
        /**
         * A machine-readable symbol, .e.g 'queryParseError'
         */
        val type: String,
    )
}