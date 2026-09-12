package nz.co.warehouseandroidtest.viewmodel

import nz.co.warehouseandroidtest.data.Product

/**
 * UI states for the product detail flow.
 *
 * It models the screen lifecycle from initial idle to final success or error rendering.
 */
sealed interface ProductDetailUiState {
    /** Initial state before the product detail request is started. */
    data object Idle : ProductDetailUiState

    /** Indicates that detail data is currently being requested. */
    data object Loading : ProductDetailUiState

    /** Successful detail response for the selected product. */
    data class Success(val product: Product) : ProductDetailUiState

    /** Error state when the detail request fails or the product is not found. */
    data class Error(val message: String) : ProductDetailUiState
}
