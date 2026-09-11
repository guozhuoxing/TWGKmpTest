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
 * Handles the product detail screen state and delegates loading to the repository.
 *
 * The view model exposes a single UI state stream so the screen can render loading, success, and
 * error states consistently.
 */
interface ProductDetailViewModelContract {
    val uiState: StateFlow<ProductDetailUiState>
    fun loadProductDetail(productId: String)
}

/**
 * Shared ViewModel for loading and exposing product detail state.
 *
 * This class is intentionally platform-neutral so the same logic can be reused by Android and iOS
 * entry points while still allowing each platform to provide its own coroutine scope lifecycle.
 */
class ProductDetailViewModel(
    private val repository: WarehouseRepository,
    private val scope: CoroutineScope? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ProductDetailViewModelContract {
    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Idle)
    override val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()
    private val activeScope = scope ?: CoroutineScope(SupervisorJob() + dispatcher)

    fun clear() {
        if (scope == null) {
            activeScope.cancel()
        }
    }

    override fun loadProductDetail(productId: String) {
        activeScope.launch(dispatcher) {
            try {
                _uiState.value = ProductDetailUiState.Loading
                val product = repository.getProductDetail(productId)
                if (product != null) {
                    _uiState.value = ProductDetailUiState.Success(product)
                } else {
                    _uiState.value = ProductDetailUiState.Error("Product not found")
                }
            } catch (e: CancellationException) {
                // Ignore cancellations from an in-flight request.
            } catch (e: Exception) {
                _uiState.value = ProductDetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

/**
 * UI states for the product detail flow.
 *
 * It models the screen lifecycle from initial idle to final success or error rendering.
 */
sealed class ProductDetailUiState {
    /** Initial state before the product detail request is started. */
    object Idle : ProductDetailUiState()

    /** Indicates that detail data is currently being requested. */
    object Loading : ProductDetailUiState()

    /** Successful detail response for the selected product. */
    data class Success(val product: Product) : ProductDetailUiState()

    /** Error state when the detail request fails or the product is not found. */
    data class Error(val message: String) : ProductDetailUiState()
}
