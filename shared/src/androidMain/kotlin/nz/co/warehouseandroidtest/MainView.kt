package nz.co.warehouseandroidtest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import nz.co.warehouseandroidtest.api.WarehouseApi
import nz.co.warehouseandroidtest.repository.WarehouseRepository
import nz.co.warehouseandroidtest.ui.App
import nz.co.warehouseandroidtest.viewmodel.PlatformProductDetailViewModel
import nz.co.warehouseandroidtest.viewmodel.PlatformSearchViewModel

@Composable
fun MainView() {
    val api = remember { WarehouseApi() }
    val repository = remember { WarehouseRepository(api) }
    val searchViewModel: PlatformSearchViewModel = viewModel { PlatformSearchViewModel(repository) }
    val productDetailViewModel: PlatformProductDetailViewModel = viewModel { PlatformProductDetailViewModel(repository) }

    App(
        searchViewModel = searchViewModel,
        productDetailViewModel = productDetailViewModel
    )
}
