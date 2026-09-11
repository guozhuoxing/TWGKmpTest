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
        println("SearchViewModel: Searching for $query")
        if (query.isBlank()) return

        activeScope.launch(dispatcher) {
            try {
                _uiState.value = SearchUiState.Loading
                val result = repository.searchProducts(query)
                println("SearchViewModel: Found ${result.products.size} products")
                _uiState.value = SearchUiState.Success(result.products)
            } catch (e: CancellationException) {
                // Ignore cancellations from an in-flight request or scope shutdown.
            } catch (e: java.util.concurrent.CancellationException) {
                // Ignore Java cancellation exceptions from underlying request infrastructure.
            } catch (e: Exception) {
                println("SearchViewModel: Error searching: ${e.message}")
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
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val products: List<Product>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}
