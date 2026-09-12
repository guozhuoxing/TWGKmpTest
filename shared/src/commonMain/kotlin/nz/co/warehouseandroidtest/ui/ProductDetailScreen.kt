package nz.co.warehouseandroidtest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import nz.co.warehouseandroidtest.ui.theme.WarehouseSpacing
import nz.co.warehouseandroidtest.viewmodel.ProductDetailUiState
import nz.co.warehouseandroidtest.viewmodel.ProductDetailViewModelContract

@Composable
private fun DefaultProductImage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(WarehouseSpacing.md))
            .background(MaterialTheme.colors.primary.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Image,
            contentDescription = "Placeholder image",
            tint = MaterialTheme.colors.primary.copy(alpha = 0.65f),
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
fun ProductDetailScreen(viewModel: ProductDetailViewModelContract, productId: String, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(productId) {
        viewModel.loadProductDetail(productId)
    }

    Scaffold(
        backgroundColor = MaterialTheme.colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Product Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = Color.White
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(WarehouseSpacing.md)) {
            when (val state = uiState) {
                is ProductDetailUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colors.primary)
                is ProductDetailUiState.Success -> {
                    val product = state.product
                    val imageUrl = product.imageUrls.firstOrNull() ?: product.productImageUrl
                    Column {
                        if (!imageUrl.isNullOrEmpty()) {
                            KamelImage(
                                resource = asyncPainterResource(data = imageUrl),
                                contentDescription = product.productName,
                                modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(WarehouseSpacing.lg)),
                                contentScale = ContentScale.Fit,
                                onLoading = {
                                    DefaultProductImage(modifier = Modifier.fillMaxWidth().height(220.dp))
                                },
                                onFailure = {
                                    DefaultProductImage(modifier = Modifier.fillMaxWidth().height(220.dp))
                                }
                            )
                        } else {
                            DefaultProductImage(modifier = Modifier.fillMaxWidth().height(220.dp))
                        }
                        Spacer(modifier = Modifier.height(WarehouseSpacing.md))
                        Text(text = product.productName ?: "", style = MaterialTheme.typography.h4, color = MaterialTheme.colors.onBackground)
                        Spacer(modifier = Modifier.height(WarehouseSpacing.xs))
                        Text(text = "Price: $${product.priceInfo?.price ?: 0.0}", style = MaterialTheme.typography.h5, color = MaterialTheme.colors.primaryVariant)
                        Spacer(modifier = Modifier.height(WarehouseSpacing.md))
                        Text(text = product.productDescription ?: "No description available.", style = MaterialTheme.typography.body1, color = MaterialTheme.colors.onBackground)
                        if (product.isClearance) {
                            Spacer(modifier = Modifier.height(WarehouseSpacing.sm))
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colors.primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "CLEARANCE",
                                    color = MaterialTheme.colors.primary,
                                    style = MaterialTheme.typography.button,
                                    modifier = Modifier.padding(horizontal = WarehouseSpacing.sm, vertical = WarehouseSpacing.xs)
                                )
                            }
                        }
                    }
                }
                is ProductDetailUiState.Error -> Text("Error: ${state.message}", color = MaterialTheme.colors.error)
                else -> {}
            }
        }
    }
}
