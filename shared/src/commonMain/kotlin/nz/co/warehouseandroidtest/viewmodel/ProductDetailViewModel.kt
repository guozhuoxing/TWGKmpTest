package nz.co.warehouseandroidtest.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nz.co.warehouseandroidtest.data.Product
import nz.co.warehouseandroidtest.repository.WarehouseRepository

class ProductDetailViewModel(private val repository: WarehouseRepository, private val scope: CoroutineScope) {
    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Idle)
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    fun loadProductDetail(productId: String) {
        scope.launch(Dispatchers.Default) {
            _uiState.value = ProductDetailUiState.Loading
            try {
                val product = repository.getProductDetail(productId)
                if (product != null) {
                    _uiState.value = ProductDetailUiState.Success(product)
                } else {
                    _uiState.value = ProductDetailUiState.Error("Product not found")
                }
            } catch (e: Exception) {
                _uiState.value = ProductDetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class ProductDetailUiState {
    object Idle : ProductDetailUiState()
    object Loading : ProductDetailUiState()
    data class Success(val product: Product) : ProductDetailUiState()
    data class Error(val message: String) : ProductDetailUiState()
}
