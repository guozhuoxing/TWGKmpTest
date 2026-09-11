package nz.co.warehouseandroidtest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import nz.co.warehouseandroidtest.api.WarehouseApi
import nz.co.warehouseandroidtest.repository.WarehouseRepository
import nz.co.warehouseandroidtest.ui.App
import nz.co.warehouseandroidtest.viewmodel.PlatformProductDetailViewModel
import nz.co.warehouseandroidtest.viewmodel.PlatformSearchViewModel

/**
 * Android-specific entry point for the shared app UI.
 *
 * This is the platform adapter that wires the platform ViewModels to the shared
 * Compose UI. The real business logic stays in the shared module, while this file
 * is responsible for creating the Android runtime dependencies (API + repository).
 */
@Composable
fun MainView() {
    // Android runtime dependencies are created here so the shared UI can consume
    // them without directly depending on platform-specific implementations.
    val api = remember { WarehouseApi() }
    val repository = remember { WarehouseRepository(api) }
    val searchViewModel: PlatformSearchViewModel = viewModel { PlatformSearchViewModel(repository) }
    val productDetailViewModel: PlatformProductDetailViewModel = viewModel { PlatformProductDetailViewModel(repository) }

    App(
        searchViewModel = searchViewModel,
        productDetailViewModel = productDetailViewModel
    )
}
