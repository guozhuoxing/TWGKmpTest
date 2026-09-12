package nz.co.warehouseandroidtest.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.HttpTimeout
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
 * The HTTP configuration is intentionally separated from the business logic so the shared layer does
 * not depend on specific endpoint values or platform environment details. The portable app code can
 * inject or override these values through ApiConfig when needed.
 */
class WarehouseApi(
    val engine: io.ktor.client.engine.HttpClientEngine? = null,
    val config: ApiConfig = ApiConfig()
) {
    private var twlToken: String? = null

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

        if (config.enableLogging) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = config.timeoutMillis
            connectTimeoutMillis = config.timeoutMillis
            socketTimeoutMillis = config.timeoutMillis
        }
    }

    suspend fun login(device: String) {
        val response = client.get("${config.baseUrl}/Login.json") {
            header("Authorization", "Guest")
            header("X-TWL-Device", device)
            header("Ocp-Apim-Subscription-Key", config.subscriptionKey)
        }
        twlToken = response.headers["X-TWL-Token"]
    }

    suspend fun searchProducts(query: String, start: Int = 0, limit: Int = 20): SearchResult {
        return client.get("${config.baseUrl}/Search.json") {
            parameter("Search", query)
            parameter("Start", start)
            parameter("Limit", limit)
            header("Ocp-Apim-Subscription-Key", config.subscriptionKey)
            twlToken?.let { header("X-TWL-Token", it) }
        }.body()
    }

    suspend fun getProductDetail(productId: String): ProductDetailResponse {
        return client.get("${config.baseUrl}/Product.json") {
            parameter("ProductId", productId)
            header("Ocp-Apim-Subscription-Key", config.subscriptionKey)
            twlToken?.let { header("X-TWL-Token", it) }
        }.body()
    }
}
