package nz.co.warehouseandroidtest.viewmodel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
    fun retryLastSearch()
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
    private var lastQuery: String = ""

    fun clear() {
        if (scope == null) {
            activeScope.cancel()
        }
    }

    override fun search(query: String) {
        val trimmedQuery = query.trim()
        AppLogger.debug("SearchViewModel: Searching for $trimmedQuery")
        if (trimmedQuery.isBlank()) return

        lastQuery = trimmedQuery
        activeScope.launch(dispatcher) {
            try {
                _uiState.value = SearchUiState.Loading
                val result = performSearchWithRetry(trimmedQuery)
                AppLogger.debug("SearchViewModel: Found ${result.products.size} products")
                _uiState.value = SearchUiState.Success(result.products)
            } catch (e: CancellationException) {
                // Ignore cancellations from an in-flight request or scope shutdown.
            } catch (e: Exception) {
                repository.resetClient()
                val friendlyMessage = formatErrorMessage(e)
                AppLogger.error("SearchViewModel: Error searching: ${e.message}")
                _uiState.value = SearchUiState.Error(friendlyMessage)
            }
        }
    }

    private suspend fun performSearchWithRetry(query: String, maxRetries: Int = 2): nz.co.warehouseandroidtest.data.SearchResult {
        var attempt = 0
        var lastError: Throwable? = null

        while (attempt <= maxRetries) {
            try {
                return repository.searchProducts(query)
            } catch (error: Throwable) {
                lastError = error
                if (attempt == maxRetries || !isRetryableError(error)) {
                    throw error
                }
                val backoffMs = 500L * (attempt + 1)
                AppLogger.debug("SearchViewModel: Retry search for '$query' in ${backoffMs}ms after error: ${error.message}")
                delay(backoffMs)
                attempt += 1
            }
        }

        throw lastError ?: IllegalStateException("Search failed without a root cause")
    }

    private fun isRetryableError(error: Throwable): Boolean {
        val message = error.message.orEmpty()
        return message.contains("timeout", ignoreCase = true) ||
            message.contains("timed out", ignoreCase = true) ||
            message.contains("connect", ignoreCase = true) ||
            message.contains("network", ignoreCase = true) ||
            message.contains("unreachable", ignoreCase = true) ||
            message.contains("temporar", ignoreCase = true) ||
            message.contains("refused", ignoreCase = true) ||
            message.contains("reset", ignoreCase = true)
    }

    override fun retryLastSearch() {
        if (lastQuery.isBlank()) return
        search(lastQuery)
    }

    private fun formatErrorMessage(error: Throwable): String {
        val rawMessage = error.message.orEmpty()
        return when {
            rawMessage.contains("timeout", ignoreCase = true) ||
                rawMessage.contains("timed out", ignoreCase = true) ->
                "The request timed out. Please try again in a moment."
            rawMessage.contains("connect", ignoreCase = true) ||
                rawMessage.contains("unreachable", ignoreCase = true) ||
                rawMessage.contains("network", ignoreCase = true) ->
                "Network connection is unavailable. Please check your connection and try again."
            rawMessage.isBlank() -> "Something went wrong while searching. Please try again."
            else -> "We couldn't load products right now. Please try again."
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
