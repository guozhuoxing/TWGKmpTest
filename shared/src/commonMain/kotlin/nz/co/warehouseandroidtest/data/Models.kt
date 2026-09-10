package nz.co.warehouseandroidtest.data

import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
    val total: Int = 0,
    val products: List<Product> = emptyList()
)

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

@Serializable
data class PriceInfo(
    val price: Double = 0.0
)

@Serializable
data class ProductDetailResponse(
    val product: Product? = null
)

@Serializable
data class LoginResponse(
    val token: String? = null
)
