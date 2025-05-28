import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SolrResponse(
    val responseHeader: ResponseHeader,
    val response: Response
)
@Serializable
data class ResponseHeader(
    val status: Int,
    val QTime: Int,
    val params: Params
)

@Serializable
data class Params(
    val q: String,
    val fl: List<String>,
    val yq: String,
    val start: String,
    val fq: String,
    val sort: String,
    val rows: String,
    val wt: String,
    val params: String,
)

@Serializable
data class Response(
    val numFound: Int,
    val start: Int,
    val maxScore: Double? = null,
    val numFoundExact: Boolean,
    val docs: List<Doc>
)

@Serializable
data class Doc(
    val name_text_en: String,
    val code_string: String,
    val categoryName_text_en_mv : List<String>,
    val score: Double,
    @SerialName("[features]") val features: String
)
