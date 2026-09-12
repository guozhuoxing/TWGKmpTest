package nz.co.warehouseandroidtest.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import nz.co.warehouseandroidtest.repository.WarehouseRepository

/**
 * Android implementation of the search screen ViewModel.
 *
 * This class acts as a thin platform adapter: it keeps the shared ViewModel logic in
 * the common module and exposes the Android lifecycle-aware ViewModel API used by
 * the Compose UI.
 */
class PlatformSearchViewModel(
    private val repository: WarehouseRepository
) : ViewModel(), SearchViewModelContract {
    // Delegate holds the shared cross-platform logic while this class handles the
    // Android lifecycle and coroutine scope.
    private val delegate = SearchViewModel(repository, viewModelScope, Dispatchers.Default)

    override val uiState = delegate.uiState

    override fun search(query: String) {
        delegate.search(query)
    }

    override fun refresh() {
        delegate.refresh()
    }

    override fun loadMore() {
        delegate.loadMore()
    }

    override fun retryLastSearch() {
        delegate.retryLastSearch()
    }

    override fun initLogin() {
        delegate.initLogin()
    }
}

/**
 * Android implementation of the product detail ViewModel.
 *
 * Similar to the search view model, this layer bridges the shared business logic to
 * the Android ViewModel environment so the UI can work with a standard lifecycle
 * owner and coroutine scope.
 */
class PlatformProductDetailViewModel(
    private val repository: WarehouseRepository
) : ViewModel(), ProductDetailViewModelContract {
    // The common ViewModel logic is delegated to keep the Android wrapper minimal.
    private val delegate = ProductDetailViewModel(repository, viewModelScope, Dispatchers.Default)

    override val uiState = delegate.uiState

    override fun loadProductDetail(productId: String) {
        delegate.loadProductDetail(productId)
    }
}
