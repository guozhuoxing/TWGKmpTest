package nz.co.warehouseandroidtest.api

/**
 * Centralized runtime configuration for API access.
 *
 * Keeping the endpoint, subscription key, and client behavior in one config object prevents the
 * business and UI layers from being tightly coupled to concrete HTTP settings.
 */
data class ApiConfig(
    val baseUrl: String = "https://legacy-apim.twg.co.nz/twgCSharpTest",
    val subscriptionKey: String = "89c018e2116048938592463c3a94fc66",
    val timeoutMillis: Long = 30_000L,
    val enableLogging: Boolean = false
)
