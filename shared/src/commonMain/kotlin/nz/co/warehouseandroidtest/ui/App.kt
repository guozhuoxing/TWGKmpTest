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
import nz.co.warehouseandroidtest.viewmodel.SearchViewModel

@Composable
fun App() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    
    val api = remember { WarehouseApi() }
    val repository = remember { WarehouseRepository(api) }
    
    MaterialTheme {
        NavHost(navController = navController, startDestination = "search") {
            composable("search") {
                val viewModel = remember { SearchViewModel(repository, scope) }
                SearchScreen(
                    viewModel = viewModel,
                    onProductClick = { productId ->
                        navController.navigate("detail/$productId")
                    }
                )
            }
            composable("detail/{productId}") { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: ""
                val viewModel = remember { ProductDetailViewModel(repository, scope) }
                ProductDetailScreen(
                    viewModel = viewModel,
                    productId = productId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
