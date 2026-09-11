package nz.co.warehouseandroidtest.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import nz.co.warehouseandroidtest.repository.WarehouseRepository

class PlatformSearchViewModel(
    private val repository: WarehouseRepository
) : ViewModel(), SearchViewModelContract {
    private val delegate = SearchViewModel(repository, viewModelScope, Dispatchers.Default)

    override val uiState = delegate.uiState

    override fun search(query: String) {
        delegate.search(query)
    }
}

class PlatformProductDetailViewModel(
    private val repository: WarehouseRepository
) : ViewModel(), ProductDetailViewModelContract {
    private val delegate = ProductDetailViewModel(repository, viewModelScope, Dispatchers.Default)

    override val uiState = delegate.uiState

    override fun loadProductDetail(productId: String) {
        delegate.loadProductDetail(productId)
    }
}
