import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

//"query_id","user","type","target","signal_time"
data class UserSignal(
    val queryId: String,
    val user: String,
    val type: String,
    val target: String,
    val time: LocalDateTime
)

data class Resulteight(val searchTerm: String, val productId: String, val weight: Double)

data class QueryTermProduct(val searchTerm: String, val productId: String)

var signalFile = "/Users/renato/private-projects/aipoweredsearch/retrotech/signals.csv"

val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSS")


var signalTypeWeight = mapOf(
    "click" to 1.0,
    "add-to-cart" to 1.2,
    "purchase" to 1.4,
)

fun main() {
    
    val userSignals = parseCSVFile(signalFile)
    println("number of signals: ${userSignals.size}")

    val (queries, interactions) = userSignals.partition { it.type == "query" }

    val queryMap = queries.associateBy { it.queryId }
    val resultMap = mutableMapOf<QueryTermProduct, Double>()

    interactions.map { interaction ->
        queryMap[interaction.queryId]?.let {
            val queryTermProduct = QueryTermProduct(it.target, interaction.target)
            resultMap[queryTermProduct] = (resultMap[queryTermProduct] ?: 0.0) + (signalTypeWeight[interaction.type] ?: 0.0)
        }
    }
    println("finished")
}

fun parseCSVFile(filePath: String): List<UserSignal> {
    return File(filePath).readLines()
        .drop(1)
        .map { it.replace("\"", "") }
        .map { it.split(",") }
        .map { UserSignal(it[0], it[1], it[2], it[3], LocalDateTime.parse(it[4], formatter)) }
}
