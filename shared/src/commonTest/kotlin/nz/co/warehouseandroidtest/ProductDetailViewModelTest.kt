package nz.co.warehouseandroidtest

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import nz.co.warehouseandroidtest.api.WarehouseApi
import nz.co.warehouseandroidtest.repository.WarehouseRepository
import nz.co.warehouseandroidtest.viewmodel.ProductDetailUiState
import nz.co.warehouseandroidtest.viewmodel.ProductDetailViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies the product detail ViewModel behaviour under success, missing-product, and failure cases.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun testLoadProductDetailSuccess() = runTest(testDispatcher) {
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(
                    """
                    {
                        "product": {
                            "productName": "Milk 2L",
                            "productId": "123",
                            "productDescription": "Fresh Milk"
                        }
                    }
                    """.trimIndent()
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val api = WarehouseApi(mockEngine)
        val repository = WarehouseRepository(api)
        val viewModel = ProductDetailViewModel(repository, this, testDispatcher)

        viewModel.loadProductDetail("123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProductDetailUiState.Success, "Expected Success state but was ${state::class.simpleName}")
        val successState = state as ProductDetailUiState.Success
        assertEquals("Milk 2L", successState.product.productName)
        assertEquals("Fresh Milk", successState.product.productDescription)
    }

    @Test
    fun testLoadProductDetailWhenProductIsMissing_shouldEmitNotFoundError() = runTest(testDispatcher) {
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(
                    """
                    {
                        "product": null
                    }
                    """.trimIndent()
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val api = WarehouseApi(mockEngine)
        val repository = WarehouseRepository(api)
        val viewModel = ProductDetailViewModel(repository, this, testDispatcher)

        viewModel.loadProductDetail("missing")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProductDetailUiState.Error, "Expected Error state but was ${state::class.simpleName}")
        assertEquals("Product not found", (state as ProductDetailUiState.Error).message)
    }

    @Test
    fun testLoadProductDetailError() = runTest(testDispatcher) {
        val mockEngine = MockEngine { _ ->
            respond(
                content = "Error",
                status = HttpStatusCode.InternalServerError
            )
        }

        val api = WarehouseApi(mockEngine)
        val repository = WarehouseRepository(api)
        val viewModel = ProductDetailViewModel(repository, this, testDispatcher)

        viewModel.loadProductDetail("123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProductDetailUiState.Error, "Expected Error state but was ${state::class.simpleName}")
    }
}
