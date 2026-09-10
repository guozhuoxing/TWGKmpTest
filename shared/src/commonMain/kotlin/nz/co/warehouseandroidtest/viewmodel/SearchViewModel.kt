package nz.co.warehouseandroidtest.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nz.co.warehouseandroidtest.data.Product
import nz.co.warehouseandroidtest.repository.WarehouseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class SearchViewModel(private val repository: WarehouseRepository, private val scope: CoroutineScope) {
    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun search(query: String) {
        println("SearchViewModel: Searching for $query")
        if (query.isBlank()) return
        
        scope.launch(Dispatchers.Default) {
            _uiState.value = SearchUiState.Loading
            try {
                val result = repository.searchProducts(query)
                println("SearchViewModel: Found ${result.products.size} products")
                _uiState.value = SearchUiState.Success(result.products)
            } catch (e: Exception) {
                println("SearchViewModel: Error searching: ${e.message}")
                e.printStackTrace()
                _uiState.value = SearchUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val products: List<Product>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}
