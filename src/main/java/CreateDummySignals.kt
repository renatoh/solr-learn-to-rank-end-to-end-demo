import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class Signal(val searchTerm: String, val productCode: String)
@Serializable
data class WeightSearchTermProductCode(val searchTerm: String, val productCode: String, val weight: Double, val features: List<Feature>)
@Serializable
data class Feature(val name: String, val value: Double)

@Serializable
data class Person(val gender: String, val name: String)
fun main() {
    val totalSignals = 1000 * 1000
    val rows = 30
    
    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    val searchResults: Map<String, List<Pair<String,String>>> = powerToolSearchTerms.associateWith { searchTerm ->
        val solrQuery = getPowerToolsSearchQuery(searchTerm, rows)
        val jsonString = httpGet(solrQuery)
        val solrResponse = json.decodeFromString<SolrResponse>(jsonString,)
        solrResponse.response.docs.map { Pair(it.code_string,it.features) }
    }
    println("total search result:" + searchResults.values.sumOf { it.size })

    searchResults.entries.forEach { println(it.key +"->"+it.value.map { it.first }) }

    val allWeights: List<WeightSearchTermProductCode> = searchResults.entries.flatMap { (key, value) ->
        value.map { searchResult ->
            val weight = signalWeights.get(key)?.get(searchResult.first) ?: 0.5
            WeightSearchTermProductCode(key, searchResult.first, weight, parseFeatures(searchResult.second))
        }
    }

    val sumOfAllWeights = allWeights.sumOf { it.weight }
    
    val allWeightsJson = json.encodeToString(allWeights)
    println(allWeightsJson)
    
    val signalNumberMultiplier = (totalSignals / sumOfAllWeights).toDouble()
    
/*    allWeights.forEach {
        println("searchTerm: ${it.searchTerm}, productCode: ${it.productCode}, weight: ${it.weight}, features: ${it.features}")
    }*/
    
    
    println("Number of all weights:${allWeights.size}")

/*    val allSignals = allWeights.flatMap { weightSearchTermProduct ->
        val numberOfSignals = (weightSearchTermProduct.weight * signalNumberMultiplier).roundToInt()
        (0 until numberOfSignals).map { i ->
            Signal(weightSearchTermProduct.searchTerm, weightSearchTermProduct.productCode)
        }
    }.shuffled()*/

//    println("total signals: " + allSignals.size)
}

//parse features string categoryNameMatch=0.27647737,nameMatch=0.77347434,priceFeature=81.0,originalScore=135.35802
fun parseFeatures(features: String): List<Feature> {
    return features.split(",").map { feature ->
        val parts = feature.split("=")
        if (parts.size == 2) {
            Feature(parts[0], parts[1].toDouble())
        } else {
            throw IllegalArgumentException("Invalid feature format: $feature")
        }
    }
}


fun httpGet(url: String): String {

    disableSslVerification()

    val connection = URL(url).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 5000
    connection.readTimeout = 5000

    return try {
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        stream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }   
}

//todo remove inStockkFlag from sort and adjust signalWeights
private fun getElectornicsSearchQuery(searchTerm: String, rows: Int): String {
    return """
        https://localhost:8983/solr/master_powertools_Product_default/select?params={
        fl=code_string,name_de_text&
        fq=(catalogId:"electronicsProductCatalog"+AND+catalogVersion:"Online")&
        yq=_query_:"\{\!multiMaxScore\+tie%3D0.0\}\(\(keywords_text_en\:$searchTerm\^20.0\)\+OR\+\(manufacturerName_text\:$searchTerm\^40.0\)\+OR\+\(description_text_en\:$searchTerm\^25.0\)\+OR\+\(categoryName_text_en_mv\:$searchTerm\^20.0\)\+OR\+\(name_text_en\:$searchTerm\^50.0\)\)\+OR\+\(\(code_string\:$searchTerm\~\)\+OR\+\(keywords_text_en\:$searchTerm\~\^10.0\)\+OR\+\(manufacturerName_text\:$searchTerm\~\^20.0\)\+OR\+\(description_text_en\:$searchTerm\~\^10.0\)\+OR\+\(categoryName_text_en_mv\:$searchTerm\~\^10.0\)\+OR\+\(ean_string\:$searchTerm\~\)\+OR\+\(name_text_en\:$searchTerm\~\^25.0\)\)\+OR\+\(\(code_string\:\"$searchTerm\"\^90.0\)\+OR\+\(keywords_text_en\:\"$searchTerm\"\^40.0\)\+OR\+\(manufacturerName_text\:\"$searchTerm\"\^80.0\)\+OR\+\(description_text_en\:\"$searchTerm\"\^50.0\)\+OR\+\(categoryName_text_en_mv\:\"$searchTerm\"\^40.0\)\+OR\+\(ean_string\:\"$searchTerm\"\^100.0\)\+OR\+\(name_text_en\:\"$searchTerm\"\^100.0\)\)"&
        wt=json&
        start=0&
        sort=inStockFlag_boolean+desc,score+desc&
        rows=$rows&
        &fl=score,code_string,name_text_de&
        q={!boost}(%2B{!lucene+v%3D${'$'}yq})&} 
""".trimIndent().replace("\n", "")
}
private fun getPowerToolsSearchQuery(searchTerm: String, rows: Int): String {
    return """
https://localhost:8983/solr/master_powertools_Product_default/select?params=&
fl=score,*&
  fq=(catalogId:"powertoolsProductCatalog"+AND+catalogVersion:"Online")&
  yq=_query_:"\{\!multiMaxScore\+tie%3D0.0\}\(\(keywords_text_en\:$searchTerm\^20.0\)\+OR\+\(manufacturerName_text\:$searchTerm\^40.0\)\+OR\+\(description_text_en\:$searchTerm\^25.0\)\+OR\+\(categoryName_text_en_mv\:$searchTerm\^20.0\)\+OR\+\(name_text_en\:$searchTerm\^50.0\)\)\+OR\+\(\(code_string\:$searchTerm\~\)\+OR\+\(keywords_text_en\:$searchTerm\~\^10.0\)\+OR\+\(manufacturerName_text\:$searchTerm\~\^20.0\)\+OR\+\(description_text_en\:$searchTerm\~1\^10.0\)\+OR\+\(categoryName_text_en_mv\:$searchTerm\~\^10.0\)\+OR\+\(ean_string\:$searchTerm\~\)\+OR\+\(name_text_en\:$searchTerm\~\^25.0\)\)\+OR\+\(\(code_string\:\"$searchTerm\"\^90.0\)\+OR\+\(keywords_text_en\:\"$searchTerm\"\^40.0\)\+OR\+\(manufacturerName_text\:\"$searchTerm\"\^80.0\)\+OR\+\(description_text_en\:\"$searchTerm\"\^50.0\)\+OR\+\(categoryName_text_en_mv\:\"$searchTerm\"\^40.0\)\+OR\+\(ean_string\:\"$searchTerm\"\^100.0\)\+OR\+\(name_text_en\:\"$searchTerm\"\^100.0\)\)"&
  wt=json&
  start=0&
  fl=*,score,[features%20efi.text=$searchTerm]&
  sort=score+desc,code_string+desc&
  rows=$rows&
  q={!boost}(%2B{!lucene+v%3D${'$'}yq})
""".trimIndent().replace("\n", "").replace(" ", "")
}

private fun getApparelSearchQuery(searchTerm: String, rows: Int): String {
    return """
        https://localhost:8983/solr/master_apparel-uk_Product_default/select?
        params=
        fl=score,*&
        fq=(catalogId:"apparelProductCatalog"+AND+catalogVersion:"Online")&
        fq={!tag%3Dfc}{!collapse+field%3DbaseProductCode_string+sort%3D${'$'}sort+nullPolicy%3Dcollapse}&
        spellcheck.q=$searchTerm&
        expand.rows=1000&
        yq=_query_:"\{\!multiMaxScore\+tie%3D0.0\}\(\(keywords_text_en\:$searchTerm\^20.0\)\+OR\+\(manufacturerName_text\:$searchTerm\^40.0\)\+OR\+\(categoryName_text_en_mv\:$searchTerm\^20.0\)\+OR\+\(name_text_en\:$searchTerm\^50.0\)\)\+OR\+\(\(code_string\:$searchTerm\~\)\+OR\+\(keywords_text_en\:$searchTerm\~\^10.0\)\+OR\+\(manufacturerName_text\:$searchTerm\~\^20.0\)\+OR\+\(categoryName_text_en_mv\:$searchTerm\~\^10.0\)\+OR\+\(ean_string\:$searchTerm\~\)\+OR\+\(name_text_en\:$searchTerm\~\^25.0\)\)\+OR\+\(\(code_string\:\"$searchTerm\"\^90.0\)\+OR\+\(keywords_text_en\:\"$searchTerm\"\^40.0\)\+OR\+\(manufacturerName_text\:\"$searchTerm\"\^80.0\)\+OR\+\(categoryName_text_en_mv\:\"$searchTerm\"\^40.0\)\+OR\+\(ean_string\:\"$searchTerm\"\^100.0\)\+OR\+\(name_text_en\:\"$searchTerm\"\^100.0\)\)"&
        start=0&
        sort=inStockFlag_boolean+desc,score+desc&
        rows=20&
        version=2&
        hl.snippets=3&
        q={!boost}(%2B{!lucene+v%3D${'$'}yq})&
        expand.field=baseProductCode_string&
        expand=true&
        wt=json&
        }
         
""".trimIndent().replace("\n", "")
}


fun disableSslVerification() {
    try {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        val sc = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)

        // Disable hostname verification
        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

val electronicsSearchTerms = listOf(
    "camera",
    "battery",
    "adapter",
    "remote",
    "lense",
    "sony",
    "ef",
    "memory",
    "eos",
    "cleaner",
    "hybrid",
    "color",
    "kodak",
    "canon",
    "compact",
    "camcorder",
    "professional",
    "cyber",
    "samsung",
    "web",
    "circular",
    "power",
    "cable",
    "hdr",
)
val powerToolSearchTerms = listOf(
   "drill",
    "grinder",
    "Nail",
    "Hammer",
    "Bosch",
    "Screwdriver",
    "Safety",
    "Boot",
    "Sander",
    "Screwdriver",
    "Tools",
    "Grinder",
    "Bosch",
    "PSB",
    "Keystone",
    "Medisana",
    "saw",
    "disc",
    "footware",
    "jigsaw",
    "cable",
    "kit",
    "psr",
    "hitachi",
    "skill",
    "jigsaw",
    "einhell",
    
)

val signalWeightsElectronics = mapOf(
    "camera" to mapOf(
        "553637" to 1.5,
        "1934794" to 1.5,
        "726510" to 0.5,
        "300938" to 0.5
    ),
    "battery" to mapOf(
        "824259" to 0.5,
        "3708646" to 1.5,
        "861175" to 1.5
    ),
    "adapter" to mapOf(
        "2938457" to 1.5,
    ),
    "remote" to mapOf(
        "2278102" to .5,
        "1934795" to 1.5,
    )
)

val signalWeights = mapOf(
    "drill" to mapOf(
        "3887483" to 0.15,
        "3887477" to 0.2,
        "3881075" to 0.75,
        "3881063" to 0.8
    ),
    "hammer" to mapOf(
        "3887493" to 0.85,
        "3887475" to 0.85,
        "3887514" to 0.9,
        "3884636" to 0.8
    ),
    "Sander" to mapOf(
        "3884599" to 0.6,
        "2116266" to 0.7,
        "2116274" to 0.3,
    ),
    "hitachi" to mapOf(
        "3921095" to 0.2,
        "3887124" to 0.7,
        "3887120" to 0.9,
    ),
    "footware" to mapOf(
        "88117000" to 0.9,
        "33031000" to 0.95,
        "3881021" to 0.3,
    ),
    "grinder" to mapOf(
        "3887530" to 0.1,
        "3881023" to 0.8,
        "4567180" to 0.9,
    )
)





