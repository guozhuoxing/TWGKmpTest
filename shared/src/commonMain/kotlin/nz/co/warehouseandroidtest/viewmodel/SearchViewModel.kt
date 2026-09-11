package nz.co.warehouseandroidtest.viewmodel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nz.co.warehouseandroidtest.data.Product
import nz.co.warehouseandroidtest.logging.AppLogger
import nz.co.warehouseandroidtest.repository.WarehouseRepository

/**
 * Handles search state for the product search screen.
 *
 * It exposes UI state updates based on the repository response and keeps the screen logic free from
 * network details.
 */
interface SearchViewModelContract {
    val uiState: StateFlow<SearchUiState>
    fun search(query: String)
}

/**
 * Shared search ViewModel for the product lookup flow.
 *
 * It keeps the repository interaction and state transitions in one place so the UI layer can remain
 * focused on rendering and user interaction. The optional scope is provided by platform adapters when
 * the app is running under Android lifecycle-aware ViewModels.
 */
class SearchViewModel(
    private val repository: WarehouseRepository,
    private val scope: CoroutineScope? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : SearchViewModelContract {
    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    override val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private val activeScope = scope ?: CoroutineScope(SupervisorJob() + dispatcher)

    fun clear() {
        if (scope == null) {
            activeScope.cancel()
        }
    }

    override fun search(query: String) {
        AppLogger.debug("SearchViewModel: Searching for $query")
        if (query.isBlank()) return

        activeScope.launch(dispatcher) {
            try {
                _uiState.value = SearchUiState.Loading
                val result = repository.searchProducts(query)
                AppLogger.debug("SearchViewModel: Found ${result.products.size} products")
                _uiState.value = SearchUiState.Success(result.products)
            } catch (e: CancellationException) {
                // Ignore cancellations from an in-flight request or scope shutdown.
            } catch (e: Exception) {
                AppLogger.error("SearchViewModel: Error searching: ${e.message}")
                _uiState.value = SearchUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

/**
 * UI states exposed by the search flow.
 *
 * The ViewModel transitions through Idle -> Loading -> Success/Error depending on repository result.
 */
sealed class SearchUiState {
    /** Initial state before any search request is started. */
    object Idle : SearchUiState()

    /** Indicates that the current query is being fetched from the repository. */
    object Loading : SearchUiState()

    /** Successful search response containing matching products. */
    data class Success(val products: List<Product>) : SearchUiState()

    /** Search failure state with a human-readable error message. */
    data class Error(val message: String) : SearchUiState()
}
