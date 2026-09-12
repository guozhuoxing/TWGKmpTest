package nz.co.warehouseandroidtest.viewmodel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nz.co.warehouseandroidtest.getPlatformName
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
    fun refresh()
    fun loadMore()
    fun retryLastSearch()
    fun initLogin()
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
    companion object {
        const val PAGE_SIZE = 10
    }

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    override val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    // Shared coroutine scope for all async work in this ViewModel.
    private val activeScope = scope ?: CoroutineScope(SupervisorJob() + dispatcher)
    // Tracks the last search term so refresh/load-more can reuse the same query.
    private var lastQuery: String = ""
    // Login is triggered only once per ViewModel lifetime.
    private var hasLoggedIn = false
    // Prevents overlapping searches.
    private var isSearchInFlight = false
    // Prevents overlapping pagination requests.
    private var isLoadMoreInFlight = false
    // Current page index used to calculate the correct offset for the next page.
    private var currentPage = 0

    override fun initLogin() {
        if (hasLoggedIn) return
        activeScope.launch(dispatcher) {
            try {
                AppLogger.debug("SearchViewModel: Initiating login token retrieval")
                repository.login(getPlatformName())
                hasLoggedIn = true
                AppLogger.debug("SearchViewModel: Login successful, token retained")
            } catch (e: Exception) {
                AppLogger.error("SearchViewModel: Login failed: ${e.message}")
            }
        }
    }

    override fun search(query: String) {
        val trimmedQuery = query.trim()
        AppLogger.debug("SearchViewModel: Searching for $trimmedQuery")
        if (trimmedQuery.isBlank() || isSearchInFlight) return

        lastQuery = trimmedQuery
        currentPage = 0
        isSearchInFlight = true
        _uiState.value = SearchUiState.Loading
        activeScope.launch(dispatcher) {
            try {
                val start = currentPage * PAGE_SIZE
                val result = performSearchWithRetry(trimmedQuery, start = start, limit = PAGE_SIZE)
                AppLogger.debug("SearchViewModel: Page $currentPage - requested start=$start, limit=$PAGE_SIZE, got ${result.products.size} items")
                _uiState.value = SearchUiState.Success(result.products)
            } catch (e: Exception) {
                repository.resetClient()
                val friendlyMessage = formatErrorMessage(e)
                AppLogger.error("SearchViewModel: Error searching: ${e.message}")
                _uiState.value = SearchUiState.Error(friendlyMessage)
            } finally {
                isSearchInFlight = false
            }
        }
    }

    override fun refresh() {
        if (lastQuery.isBlank()) return
        search(lastQuery)
    }

    override fun loadMore() {
        if (lastQuery.isBlank() || isSearchInFlight || isLoadMoreInFlight) return

        val currentProducts = when (val state = _uiState.value) {
            is SearchUiState.Success -> state.products
            is SearchUiState.LoadingMore -> state.products
            else -> emptyList()
        }
        if (currentProducts.isEmpty()) return

        isLoadMoreInFlight = true
        activeScope.launch(dispatcher) {
            try {
                _uiState.value = SearchUiState.LoadingMore(currentProducts)
                currentPage += 1
                val start = currentPage * PAGE_SIZE
                val result = performSearchWithRetry(lastQuery, start = start, limit = PAGE_SIZE)
                AppLogger.debug("SearchViewModel: Page $currentPage - requested start=$start, limit=$PAGE_SIZE, got ${result.products.size} items")
                
                // Remove duplicates by productId
                val currentIds = currentProducts.mapNotNull { it.productId }.toSet()
                val newProducts = result.products.filter { it.productId !in currentIds }
                val merged = currentProducts + newProducts
                
                _uiState.value = SearchUiState.Success(merged)
            } catch (e: CancellationException) {
                // Ignore cancellations from an in-flight request or scope shutdown.
            } catch (e: Exception) {
                repository.resetClient()
                val friendlyMessage = formatErrorMessage(e)
                AppLogger.error("SearchViewModel: Error loading more: ${e.message}")
                _uiState.value = SearchUiState.Error(friendlyMessage)
            } finally {
                isLoadMoreInFlight = false
            }
        }
    }

    private suspend fun performSearchWithRetry(query: String, start: Int = 0, limit: Int = PAGE_SIZE, maxRetries: Int = 2): nz.co.warehouseandroidtest.data.SearchResult {
        var attempt = 0
        var lastError: Throwable? = null

        while (attempt <= maxRetries) {
            try {
                return repository.searchProducts(query, start, limit)
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
 * UI state exposed by the search flow.
 *
 * The screen intentionally models each meaningful state separately so the Compose UI can render the
 * correct layout for idle, loading, empty, pagination, success, and error scenarios.
 */
sealed interface SearchUiState {
    /** Initial state before any search request is started. */
    data object Idle : SearchUiState

    /** Indicates that the current query is being fetched from the repository. */
    data object Loading : SearchUiState

    /** Indicates that more items are being appended to an existing result list. */
    data class LoadingMore(val products: List<Product>) : SearchUiState

    /** Successful search response containing matching products. */
    data class Success(val products: List<Product>) : SearchUiState

    /** Search failure state with a human-readable error message. */
    data class Error(val message: String) : SearchUiState
}
