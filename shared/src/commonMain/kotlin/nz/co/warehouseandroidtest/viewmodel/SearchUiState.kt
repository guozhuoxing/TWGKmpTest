package nz.co.warehouseandroidtest.viewmodel

import nz.co.warehouseandroidtest.data.Product

/**
 * UI state exposed by the search flow.
 *
 * The screen intentionally models each meaningful state separately so the Compose UI can render the
 * correct layout for idle, loading, pagination, success, and error scenarios.
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
