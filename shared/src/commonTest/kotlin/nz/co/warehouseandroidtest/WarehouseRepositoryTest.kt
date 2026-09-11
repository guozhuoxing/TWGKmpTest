package nz.co.warehouseandroidtest

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.test.runTest
import nz.co.warehouseandroidtest.api.WarehouseApi
import nz.co.warehouseandroidtest.repository.WarehouseRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Ensures the repository correctly maps API responses into domain models.
 */
class WarehouseRepositoryTest {

    @Test
    fun testSearchProductsSuccess() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel("""
                    {
                        "total": 1,
                        "products": [
                            {
                                "productName": "Milk",
                                "productId": "123",
                                "priceInfo": { "price": 4.5 }
                            }
                        ]
                    }
                """.trimIndent()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val api = WarehouseApi(mockEngine)
        val repository = WarehouseRepository(api)
        
        val result = repository.searchProducts("milk")
        
        assertEquals(1, result.total)
        assertEquals("Milk", result.products[0].productName)
        assertEquals("123", result.products[0].productId)
    }

    @Test
    fun testGetProductDetailSuccess() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel("""
                    {
                        "product": {
                            "productName": "Milk 2L",
                            "productId": "123",
                            "productDescription": "Fresh Milk"
                        }
                    }
                """.trimIndent()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val api = WarehouseApi(mockEngine)
        val repository = WarehouseRepository(api)
        
        val product = repository.getProductDetail("123")
        
        assertNotNull(product)
        assertEquals("Milk 2L", product.productName)
        assertEquals("Fresh Milk", product.productDescription)
    }
}
