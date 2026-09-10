package nz.co.warehouseandroidtest.repository

import nz.co.warehouseandroidtest.api.WarehouseApi
import nz.co.warehouseandroidtest.data.Product
import nz.co.warehouseandroidtest.data.SearchResult

class WarehouseRepository(private val api: WarehouseApi) {
    suspend fun searchProducts(query: String, start: Int = 0): SearchResult {
        return api.searchProducts(query, start)
    }

    suspend fun getProductDetail(productId: String): Product? {
        return api.getProductDetail(productId).product
    }
}
