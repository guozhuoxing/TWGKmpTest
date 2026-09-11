package nz.co.warehouseandroidtest.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import nz.co.warehouseandroidtest.api.WarehouseApi
import nz.co.warehouseandroidtest.repository.WarehouseRepository
import nz.co.warehouseandroidtest.viewmodel.ProductDetailViewModel
import nz.co.warehouseandroidtest.viewmodel.ProductDetailViewModelContract
import nz.co.warehouseandroidtest.viewmodel.SearchViewModel
import nz.co.warehouseandroidtest.viewmodel.SearchViewModelContract

@Composable
fun App(
    searchViewModel: SearchViewModelContract? = null,
    productDetailViewModel: ProductDetailViewModelContract? = null
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val api = remember { WarehouseApi() }
    val repository = remember { WarehouseRepository(api) }
    val resolvedSearchViewModel = searchViewModel ?: remember { SearchViewModel(repository, scope) }
    val resolvedProductDetailViewModel = productDetailViewModel ?: remember { ProductDetailViewModel(repository, scope) }

    MaterialTheme {
        NavHost(navController = navController, startDestination = "search") {
            composable("search") {
                SearchScreen(
                    viewModel = resolvedSearchViewModel,
                    onProductClick = { productId ->
                        navController.navigate("detail/$productId")
                    }
                )
            }
            composable("detail/{productId}") { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: ""
                ProductDetailScreen(
                    viewModel = resolvedProductDetailViewModel,
                    productId = productId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
