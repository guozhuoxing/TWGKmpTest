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
import nz.co.warehouseandroidtest.viewmodel.SearchUiState
import nz.co.warehouseandroidtest.viewmodel.SearchViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Behavior tests for the search ViewModel.
 *
 * These use Google-style test naming and assertions, while staying compatible with this KMP/Ktor
 * project’s injected dispatcher design and real HTTP callback scheduling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @Test
    fun search_whenQueryIsBlank_shouldNotTriggerRequest() = runBlocking {
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("""
                    {
                        "total": 1,
                        "products": [
                            { "productName": "Milk", "productId": "123" }
                        ]
                    }
                """.trimIndent()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val viewModel = SearchViewModel(
            WarehouseRepository(WarehouseApi(mockEngine)),
            this
        )

        viewModel.search("   ")
        assertEquals(SearchUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun search_whenApiReturnsEmptyList_shouldEmitLoadingThenSuccess() = runBlocking {
        val responseGate = CompletableDeferred<Unit>()
        val mockEngine = MockEngine { _ ->
            responseGate.await()
            respond(
                content = ByteReadChannel("""
                    {
                        "total": 0,
                        "products": []
                    }
                """.trimIndent()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val viewModel = SearchViewModel(
            WarehouseRepository(WarehouseApi(mockEngine)),
            this
        )

        viewModel.search("milk")

        withTimeout(5000L) {
            while (viewModel.uiState.value !is SearchUiState.Loading) {
                delay(10)
            }
            assertTrue(viewModel.uiState.value is SearchUiState.Loading)
            responseGate.complete(Unit)
            while (viewModel.uiState.value is SearchUiState.Loading) {
                delay(10)
            }
        }

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Success, "Expected Success state but was ${state::class.simpleName}")
        val successState = state as SearchUiState.Success
        assertEquals(0, successState.products.size)
    }

    @Test
    fun search_whenApiReturnsSuccess_shouldEmitLoadingThenSuccess() = runBlocking {
        val responseGate = CompletableDeferred<Unit>()
        val mockEngine = MockEngine { _ ->
            responseGate.await()
            respond(
                content = ByteReadChannel("""
                    {
                        "total": 1,
                        "products": [
                            { "productName": "Milk", "productId": "123" }
                        ]
                    }
                """.trimIndent()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val viewModel = SearchViewModel(
            WarehouseRepository(WarehouseApi(mockEngine)),
            this
        )

        viewModel.search("milk")

        withTimeout(5000L) {
            while (viewModel.uiState.value !is SearchUiState.Loading) {
                delay(10)
            }
            assertTrue(viewModel.uiState.value is SearchUiState.Loading)
            responseGate.complete(Unit)
            while (viewModel.uiState.value is SearchUiState.Loading) {
                delay(10)
            }
        }

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Success, "Expected Success state but was ${state::class.simpleName}")
        val successState = state as SearchUiState.Success
        assertEquals(1, successState.products.size)
        assertEquals("Milk", successState.products[0].productName)
    }

    @Test
    fun search_whenApiFails_shouldEmitLoadingThenError() = runBlocking {
        val responseGate = CompletableDeferred<Unit>()
        val mockEngine = MockEngine { _ ->
            responseGate.await()
            respond(
                content = "Error",
                status = HttpStatusCode.InternalServerError
            )
        }

        val viewModel = SearchViewModel(
            WarehouseRepository(WarehouseApi(mockEngine)),
            this
        )

        viewModel.search("milk")

        withTimeout(5000L) {
            while (viewModel.uiState.value !is SearchUiState.Loading) {
                delay(10)
            }
            assertTrue(viewModel.uiState.value is SearchUiState.Loading)
            responseGate.complete(Unit)
            while (viewModel.uiState.value is SearchUiState.Loading) {
                delay(10)
            }
        }

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Error, "Expected Error state but was ${state::class.simpleName}")
    }

    @Test
    fun search_whenRepositoryThrowsTimeout_shouldEmitFriendlyErrorMessage() = runBlocking {
        val mockEngine = MockEngine { _ ->
            throw RuntimeException("Request timed out")
        }

        val viewModel = SearchViewModel(
            WarehouseRepository(WarehouseApi(mockEngine)),
            this
        )

        viewModel.search("iphone")

        withTimeout(5000L) {
            while (viewModel.uiState.value !is SearchUiState.Error) {
                delay(10)
            }
        }

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Error, "Expected Error state but was ${state::class.simpleName}")
        assertEquals(
            "The request timed out. Please try again in a moment.",
            (state as SearchUiState.Error).message
        )
    }

    @Test
    fun search_error_showsError_and_loadingCleared() = runBlocking {
        val responseGate = CompletableDeferred<Unit>()
        val mockEngine = MockEngine { _ ->
            responseGate.await()
            respond(
                content = "Error",
                status = HttpStatusCode.InternalServerError
            )
        }

        val viewModel = SearchViewModel(
            WarehouseRepository(WarehouseApi(mockEngine)),
            this
        )

        viewModel.search("milk")

        // Wait until Loading is emitted
        withTimeout(5000L) {
            while (viewModel.uiState.value !is SearchUiState.Loading) {
                delay(10)
            }
            assertTrue(viewModel.uiState.value is SearchUiState.Loading)
        }

        // Trigger the failing response and wait for Error
        responseGate.complete(Unit)

        withTimeout(5000L) {
            while (viewModel.uiState.value !is SearchUiState.Error) {
                delay(10)
            }
        }

        val finalState = viewModel.uiState.value
        assertTrue(finalState is SearchUiState.Error)
        // Ensure loading is no longer the current state
        assertTrue(finalState !is SearchUiState.Loading)
    }

}
