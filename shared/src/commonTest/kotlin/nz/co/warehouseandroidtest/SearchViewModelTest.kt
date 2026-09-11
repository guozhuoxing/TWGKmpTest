package nz.co.warehouseandroidtest

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import nz.co.warehouseandroidtest.api.WarehouseApi
import nz.co.warehouseandroidtest.repository.WarehouseRepository
import nz.co.warehouseandroidtest.viewmodel.SearchUiState
import nz.co.warehouseandroidtest.viewmodel.SearchViewModel
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun search_whenQueryIsBlank_shouldNotTriggerRequest() = runTest(testDispatcher) {
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("""
                    {
                        "total": 1,
                        "products": [
                            {
                                "productName": "Milk",
                                "productId": "123"
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
        val viewModel = SearchViewModel(repository, this, testDispatcher)

        viewModel.search("   ")

        assertEquals(SearchUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun search_whenApiReturnsEmptyList_shouldEmitSuccessWithEmptyProducts() = runTest(testDispatcher) {
        val mockEngine = MockEngine { _ ->
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

        val api = WarehouseApi(mockEngine)
        val repository = WarehouseRepository(api)
        val viewModel = SearchViewModel(repository, this, testDispatcher)

        viewModel.search("milk")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Success, "Expected Success state but was ${state::class.simpleName}")
        val successState = state as SearchUiState.Success
        assertEquals(0, successState.products.size)
    }

    @Test
    fun testSearchFlowSuccess() = runTest(testDispatcher) {
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("""
                    {
                        "total": 1,
                        "products": [
                            {
                                "productName": "Milk",
                                "productId": "123"
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
        val viewModel = SearchViewModel(repository, this, testDispatcher)

        assertEquals(SearchUiState.Idle, viewModel.uiState.value)

        viewModel.search("milk")

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Success, "Expected Success state but was ${state::class.simpleName}")
        val successState = state as SearchUiState.Success
        assertEquals(1, successState.products.size)
        assertEquals("Milk", successState.products[0].productName)
    }

    @Test
    fun testSearchFlowError() = runTest(testDispatcher) {
        val mockEngine = MockEngine { request ->
            respond(
                content = "Error",
                status = HttpStatusCode.InternalServerError
            )
        }

        val api = WarehouseApi(mockEngine)
        val repository = WarehouseRepository(api)
        val viewModel = SearchViewModel(repository, this, testDispatcher)

        viewModel.search("milk")
        
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Error, "Expected Error state but was ${state::class.simpleName}")
    }
}
