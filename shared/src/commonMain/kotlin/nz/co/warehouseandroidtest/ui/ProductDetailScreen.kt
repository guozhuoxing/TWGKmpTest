package nz.co.warehouseandroidtest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import nz.co.warehouseandroidtest.viewmodel.ProductDetailUiState
import nz.co.warehouseandroidtest.viewmodel.ProductDetailViewModelContract

@Composable
private fun DefaultProductImage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No Image",
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.45f),
            style = MaterialTheme.typography.caption
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
        topBar = {
            TopAppBar(
                title = { Text("Product Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (val state = uiState) {
                is ProductDetailUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is ProductDetailUiState.Success -> {
                    val product = state.product
                    val imageUrl = product.imageUrls.firstOrNull() ?: product.productImageUrl
                    Column {
                        if (!imageUrl.isNullOrEmpty()) {
                            KamelImage(
                                resource = asyncPainterResource(data = imageUrl),
                                contentDescription = product.productName,
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentScale = ContentScale.Fit,
                                onFailure = {
                                    DefaultProductImage(modifier = Modifier.fillMaxWidth().height(200.dp))
                                }
                            )
                        } else {
                            DefaultProductImage(modifier = Modifier.fillMaxWidth().height(200.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = product.productName ?: "", style = MaterialTheme.typography.h4)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Price: $${product.priceInfo?.price ?: 0.0}", style = MaterialTheme.typography.h5)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = product.productDescription ?: "No description available.", style = MaterialTheme.typography.body1)
                        if (product.isClearance) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "CLEARANCE", color = MaterialTheme.colors.error, style = MaterialTheme.typography.button)
                        }
                    }
                }
                is ProductDetailUiState.Error -> Text("Error: ${state.message}", color = MaterialTheme.colors.error)
                else -> {}
            }
        }
    }
}
