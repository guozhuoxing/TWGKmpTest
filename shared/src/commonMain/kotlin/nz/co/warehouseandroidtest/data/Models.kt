package nz.co.warehouseandroidtest.data

import kotlinx.serialization.Serializable

/**
 * Response payload returned from the warehouse product search endpoint.
 */
@Serializable
data class SearchResult(
    val total: Int = 0,
    val products: List<Product> = emptyList()
)

/**
 * Core product model used throughout the app for list rows and detail screens.
 */
@Serializable
data class Product(
    val productName: String? = null,
    val productBarcode: String? = null,
    val productImageUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val productId: String? = null,
    val priceInfo: PriceInfo? = null,
    val productDescription: String? = null,
    val isClearance: Boolean = false,
    val brandDescription: String? = null
)

/**
 * Price metadata for a product.
 */
@Serializable
data class PriceInfo(
    val price: Double = 0.0
)

/**
 * Response wrapper used by the product detail endpoint.
 */
@Serializable
data class ProductDetailResponse(
    val product: Product? = null
)

/**
 * Authentication response modeled for login flows.
 */
@Serializable
data class LoginResponse(
    val token: String? = null
)
