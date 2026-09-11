package nz.co.warehouseandroidtest.repository

import nz.co.warehouseandroidtest.api.WarehouseApi
import nz.co.warehouseandroidtest.data.Product
import nz.co.warehouseandroidtest.data.SearchResult

/**
 * Repository layer that exposes product domain operations.
 *
 * It keeps the UI and ViewModels independent from the underlying HTTP client by wrapping API calls
 * behind high-level methods such as product search and detail lookup.
 */
class WarehouseRepository(private val api: WarehouseApi) {
    suspend fun searchProducts(query: String, start: Int = 0): SearchResult {
        return api.searchProducts(query, start)
    }

    suspend fun getProductDetail(productId: String): Product? {
        return api.getProductDetail(productId).product
    }
}
