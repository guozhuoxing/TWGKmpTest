package nz.co.warehouseandroidtest

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
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
        val mockEngine = mockSearchEngine(
            products = listOf("{ \"productName\": \"Milk\", \"productId\": \"123\" }")
        )

        val viewModel = buildViewModel(mockEngine, this)

        viewModel.search("   ")
        assertEquals(SearchUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun search_whenApiReturnsEmptyList_shouldEmitLoadingThenSuccess() = runBlocking {
        val responseGate = CompletableDeferred<Unit>()
        val mockEngine = MockEngine { _ ->
            responseGate.await()
            respondJson(products = emptyList(), total = 0)
        }

        val viewModel = buildViewModel(mockEngine, this)

        viewModel.search("milk")

        awaitUiState(viewModel) { it is SearchUiState.Loading }
        assertTrue(viewModel.uiState.value is SearchUiState.Loading)
        responseGate.complete(Unit)
        awaitUiState(viewModel) { it !is SearchUiState.Loading }

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
            respondJson(products = listOf("{ \"productName\": \"Milk\", \"productId\": \"123\" }"), total = 1)
        }

        val viewModel = buildViewModel(mockEngine, this)

        viewModel.search("milk")

        awaitUiState(viewModel) { it is SearchUiState.Loading }
        assertTrue(viewModel.uiState.value is SearchUiState.Loading)
        responseGate.complete(Unit)
        awaitUiState(viewModel) { it !is SearchUiState.Loading }

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

        val viewModel = buildViewModel(mockEngine, this)

        viewModel.search("milk")

        awaitUiState(viewModel) { it is SearchUiState.Loading }
        assertTrue(viewModel.uiState.value is SearchUiState.Loading)
        responseGate.complete(Unit)
        awaitUiState(viewModel) { it !is SearchUiState.Loading }

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Error, "Expected Error state but was ${state::class.simpleName}")
    }

    @Test
    fun search_whenRepositoryThrowsTimeout_shouldEmitFriendlyErrorMessage() = runBlocking {
        val mockEngine = MockEngine { _ ->
            throw java.net.SocketTimeoutException("Request timed out")
        }

        val viewModel = buildViewModel(mockEngine, this)

        viewModel.search("iphone")
        awaitUiState(viewModel) { it is SearchUiState.Error }

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

        val viewModel = buildViewModel(mockEngine, this)

        viewModel.search("milk")

        awaitUiState(viewModel) { it is SearchUiState.Loading }
        assertTrue(viewModel.uiState.value is SearchUiState.Loading)

        responseGate.complete(Unit)
        awaitUiState(viewModel) { it is SearchUiState.Error }

        val finalState = viewModel.uiState.value
        assertTrue(finalState is SearchUiState.Error)
        assertTrue(finalState !is SearchUiState.Loading)
    }

    @Test
    fun loadMore_withPageCountingLogic_requestsCorrectStartAndLimit() = runBlocking {
        val requestParams = mutableListOf<Pair<Int, Int>>()

        val mockEngine = MockEngine { request ->
            val start = request.url.parameters["Start"]?.toInt() ?: 0
            val limit = request.url.parameters["Limit"]?.toInt() ?: SearchViewModel.PAGE_SIZE
            requestParams.add(start to limit)

            val products = (start until start + limit).map { index ->
                "{ \"productName\": \"Product $index\", \"productId\": \"id_$index\" }"
            }

            respondJson(products = products, total = 100)
        }

        val viewModel = buildViewModel(mockEngine, this)

        viewModel.search("milk")
        awaitUiState(viewModel) { it is SearchUiState.Success }

        viewModel.loadMore()
        awaitUiState(viewModel) { (it as? SearchUiState.Success)?.products?.size == 20 }

        viewModel.loadMore()
        awaitUiState(viewModel) { (it as? SearchUiState.Success)?.products?.size == 30 }

        assertEquals(listOf(0 to 10, 10 to 10, 20 to 10), requestParams)
    }
    
    @Test
    fun loadMore_doesNotTriggerWhenFirstPageIsSmallerThanPageSize() = runBlocking {
        var callCount = 0

        val mockEngine = MockEngine { _ ->
            callCount += 1
            val products = (0 until 5).map { index ->
                "{ \"productName\": \"Product $index\", \"productId\": \"id_$index\" }"
            }.joinToString(",")

            respond(
                content = ByteReadChannel("""
                    {
                        "total": 5,
                        "products": [$products]
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
            while (viewModel.uiState.value !is SearchUiState.Success) {
                delay(10)
            }
        }

        viewModel.loadMore()
        delay(200)

        assertEquals(1, callCount)
    }

    @Test
    fun search_newQuery_resetsPageCounterAndClearsOldData() = runBlocking {
        val requestParams = mutableListOf<String>()
        
        val mockEngine = MockEngine { request ->
            val query = request.url.parameters["Search"] ?: ""
            val start = request.url.parameters["Start"]?.toInt() ?: 0
            requestParams.add("$query:$start")
            
            val products = listOf(
                "{ \"productName\": \"${query}_Product_$start\", \"productId\": \"${query}_id_$start\" }"
            )
            
            respond(
                content = ByteReadChannel("""
                    {
                        "total": 100,
                        "products": [${products.joinToString(",")}]
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
        
        // Search milk
        viewModel.search("milk")
        withTimeout(5000L) {
            while (viewModel.uiState.value !is SearchUiState.Success) {
                delay(10)
            }
        }
        var successState = viewModel.uiState.value as SearchUiState.Success
        assertEquals("milk_Product_0", successState.products[0].productName)
        
        // Search apple (should reset page counter and clear old data)
        viewModel.search("apple")
        withTimeout(5000L) {
            while (viewModel.uiState.value !is SearchUiState.Success) {
                delay(10)
            }
        }
        successState = viewModel.uiState.value as SearchUiState.Success
        assertEquals("apple_Product_0", successState.products[0].productName)
        assertEquals(1, successState.products.size)
        
        // Verify requests: milk with start=0, then apple with start=0
        assertEquals(listOf("milk:0", "apple:0"), requestParams)
    }
    
    @Test
    fun loadMore_withDuplicateProductIds_filtersDuplicatesFromNewPage() = runBlocking {
        var callCount = 0

        val mockEngine = MockEngine { _ ->
            callCount++

            val products = if (callCount == 1) {
                (0 until 10).map { index ->
                    "{ \"productName\": \"Product $index\", \"productId\": \"id_$index\" }"
                }
            } else {
                (8 until 18).map { index ->
                    "{ \"productName\": \"Product $index\", \"productId\": \"id_$index\" }"
                }
            }

            respondJson(products = products, total = 100)
        }

        val viewModel = buildViewModel(mockEngine, this)

        viewModel.search("milk")
        awaitUiState(viewModel) { it is SearchUiState.Success }

        var successState = viewModel.uiState.value as SearchUiState.Success
        assertEquals(10, successState.products.size)

        viewModel.loadMore()
        awaitUiState(viewModel) { (it as? SearchUiState.Success)?.products?.size == 18 }

        successState = viewModel.uiState.value as SearchUiState.Success
        assertEquals(18, successState.products.size)
        val ids = successState.products.mapNotNull { it.productId }
        assertEquals(ids.size, ids.toSet().size)
    }

    private fun buildViewModel(mockEngine: MockEngine, scope: CoroutineScope): SearchViewModel {
        return SearchViewModel(
            WarehouseRepository(WarehouseApi(mockEngine)),
            scope
        )
    }

    private suspend fun awaitUiState(
        viewModel: SearchViewModel,
        timeoutMs: Long = 5000L,
        predicate: (SearchUiState) -> Boolean
    ) {
        withTimeout(timeoutMs) {
            while (!predicate(viewModel.uiState.value)) {
                delay(10)
            }
        }
    }

    private fun mockSearchEngine(
        products: List<String>,
        total: Int = products.size
    ): MockEngine = MockEngine {
        respondJson(products = products, total = total)
    }

    private fun MockRequestHandleScope.respondJson(
        products: List<String>,
        total: Int = products.size
    ) = respond(
        content = ByteReadChannel(
            """
            {
                "total": $total,
                "products": [${products.joinToString(",")}]
            }
            """.trimIndent()
        ),
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json")
    )

}

