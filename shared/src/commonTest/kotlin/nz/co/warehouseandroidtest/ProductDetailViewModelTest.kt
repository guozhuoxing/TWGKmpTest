package nz.co.warehouseandroidtest

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import nz.co.warehouseandroidtest.api.WarehouseApi
import nz.co.warehouseandroidtest.repository.WarehouseRepository
import nz.co.warehouseandroidtest.viewmodel.ProductDetailUiState
import nz.co.warehouseandroidtest.viewmodel.ProductDetailViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Behavior tests for the product-detail ViewModel.
 *
 * These keep the Google-style test names and assertions while staying compatible with the project’s
 * injected-dispatcher and Ktor MockEngine behavior.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModelTest {

    @Test
    fun loadProductDetail_whenProductExists_shouldEmitLoadingThenSuccess() = runBlocking {
        val responseGate = CompletableDeferred<Unit>()
        val mockEngine = MockEngine {
            responseGate.await()
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

        val viewModel = ProductDetailViewModel(
            WarehouseRepository(WarehouseApi(mockEngine)),
            this
        )

        viewModel.loadProductDetail("123")

        withTimeout(5000L) {
            while (viewModel.uiState.value !is ProductDetailUiState.Loading) {
                delay(10)
            }
            assertEquals(ProductDetailUiState.Loading, viewModel.uiState.value)
            responseGate.complete(Unit)
            while (viewModel.uiState.value is ProductDetailUiState.Loading) {
                delay(10)
            }
        }

        val state = viewModel.uiState.value
        assertTrue(state is ProductDetailUiState.Success, "Expected Success state but was ${state::class.simpleName}")
        val successState = state as ProductDetailUiState.Success
        assertEquals("Milk 2L", successState.product.productName)
        assertEquals("Fresh Milk", successState.product.productDescription)
    }

    @Test
    fun loadProductDetail_whenProductIsMissing_shouldEmitLoadingThenError() = runBlocking {
        val responseGate = CompletableDeferred<Unit>()
        val mockEngine = MockEngine {
            responseGate.await()
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

        val viewModel = ProductDetailViewModel(
            WarehouseRepository(WarehouseApi(mockEngine)),
            this
        )

        viewModel.loadProductDetail("missing")

        withTimeout(5000L) {
            while (viewModel.uiState.value !is ProductDetailUiState.Loading) {
                delay(10)
            }
            assertEquals(ProductDetailUiState.Loading, viewModel.uiState.value)
            responseGate.complete(Unit)
            while (viewModel.uiState.value is ProductDetailUiState.Loading) {
                delay(10)
            }
        }

        val state = viewModel.uiState.value
        assertTrue(state is ProductDetailUiState.Error, "Expected Error state but was ${state::class.simpleName}")
        assertEquals("Product not found", (state as ProductDetailUiState.Error).message)
    }

    @Test
    fun loadProductDetail_whenRequestFails_shouldEmitLoadingThenError() = runBlocking {
        val responseGate = CompletableDeferred<Unit>()
        val mockEngine = MockEngine {
            responseGate.await()
            respond(
                content = "Error",
                status = HttpStatusCode.InternalServerError
            )
        }

        val viewModel = ProductDetailViewModel(
            WarehouseRepository(WarehouseApi(mockEngine)),
            this
        )

        viewModel.loadProductDetail("123")

        withTimeout(5000L) {
            while (viewModel.uiState.value !is ProductDetailUiState.Loading) {
                delay(10)
            }
            assertEquals(ProductDetailUiState.Loading, viewModel.uiState.value)
            responseGate.complete(Unit)
            while (viewModel.uiState.value is ProductDetailUiState.Loading) {
                delay(10)
            }
        }

        val state = viewModel.uiState.value
        assertTrue(state is ProductDetailUiState.Error, "Expected Error state but was ${state::class.simpleName}")
    }
}
