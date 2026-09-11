package nz.co.warehouseandroidtest.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import nz.co.warehouseandroidtest.data.*

/**
 * Access point for warehouse product APIs.
 *
 * This client centralizes HTTP configuration, authentication headers, and the product search/detail
 * requests used by the shared KMP layer.
 */
class WarehouseApi(engine: io.ktor.client.engine.HttpClientEngine? = null) {
    private val client = if (engine != null) {
        HttpClient(engine) {
            configureClient()
        }
    } else {
        HttpClient {
            configureClient()
        }
    }

    private fun HttpClientConfig<*>.configureClient() {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                useAlternativeNames = false
            })
        }
        install(Logging) {
            level = LogLevel.ALL
            logger = object : Logger {
                override fun log(message: String) {
                    println("Ktor: $message")
                }
            }
        }
    }

    private val baseUrl = "https://legacy-apim.twg.co.nz/twgCSharpTest"
    private val subscriptionKey = "89c018e2116048938592463c3a94fc66"

    suspend fun searchProducts(query: String, start: Int = 0, limit: Int = 20): SearchResult {
        return client.get("$baseUrl/Search.json") {
            parameter("Search", query)
            parameter("Start", start)
            parameter("Limit", limit)
            header("Ocp-Apim-Subscription-Key", subscriptionKey)
        }.body()
    }

    suspend fun getProductDetail(productId: String): ProductDetailResponse {
        return client.get("$baseUrl/Product.json") {
            parameter("ProductId", productId)
            header("Ocp-Apim-Subscription-Key", subscriptionKey)
        }.body()
    }
}
