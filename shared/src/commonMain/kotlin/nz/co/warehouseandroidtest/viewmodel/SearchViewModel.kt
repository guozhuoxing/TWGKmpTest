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
    fun loadMore()
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
    // Stops endless pagination once the backend returns fewer than one full page.
    private var hasMorePages = true

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
        hasMorePages = true
        isSearchInFlight = true
        _uiState.value = SearchUiState.Loading
        activeScope.launch(dispatcher) {
            try {
                val start = currentPage * PAGE_SIZE
                val requestedLimit = PAGE_SIZE
                val result = fetchProductsPage(trimmedQuery, start = start, limit = requestedLimit)
                AppLogger.debug("SearchViewModel: Page $currentPage - requested start=$start, limit=$requestedLimit, got ${result.products.size} items, total=${result.total}")
                hasMorePages = result.total > start + result.products.size
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

    override fun loadMore() {
        if (lastQuery.isBlank() || isSearchInFlight || isLoadMoreInFlight || !hasMorePages) return

        val currentProducts = currentProductsFromState()
        if (currentProducts.isEmpty()) return

        isLoadMoreInFlight = true
        activeScope.launch(dispatcher) {
            try {
                _uiState.value = SearchUiState.LoadingMore(currentProducts)
                currentPage += 1
                val start = currentPage * PAGE_SIZE
                val requestedLimit = PAGE_SIZE
                val result = fetchProductsPage(lastQuery, start = start, limit = requestedLimit)
                AppLogger.debug("SearchViewModel: Page $currentPage - requested start=$start, limit=$requestedLimit, got ${result.products.size} items, total=${result.total}")

                val merged = currentProducts + result.products
                hasMorePages = result.total > start + result.products.size
                _uiState.value = SearchUiState.Success(merged)
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

    // Read the list currently displayed on screen so pagination can append the next page without
    // losing the existing results when the state is either Success or LoadingMore.
    private fun currentProductsFromState(): List<Product> = when (val state = _uiState.value) {
        is SearchUiState.Success -> state.products
        is SearchUiState.LoadingMore -> state.products
        else -> emptyList()
    }

    private suspend fun fetchProductsPage(
        query: String,
        start: Int = 0,
        limit: Int = PAGE_SIZE,
        maxRetries: Int = 2
    ): nz.co.warehouseandroidtest.data.SearchResult {
        var attempt = 0
        var lastError: Throwable? = null

        while (attempt <= maxRetries) {
            try {
                return repository.searchProducts(query, start, limit)
            } catch (error: Throwable) {
                lastError = error
                if (attempt == maxRetries || !isTransientRequestError(error)) {
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

    private fun isTransientRequestError(error: Throwable): Boolean {
        if (error is CancellationException) return false
        return error::class.simpleName in setOf(
            "SocketTimeoutException",
            "TimeoutException"
        )
    }

    private fun formatErrorMessage(error: Throwable): String {
        return when (error::class.simpleName) {
            "SocketTimeoutException",
            "TimeoutException" -> "The request timed out. Please try again in a moment."
            else -> "We couldn't load products right now. Please try again."
        }
    }
}

