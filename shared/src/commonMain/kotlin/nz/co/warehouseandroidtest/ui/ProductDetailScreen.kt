package nz.co.warehouseandroidtest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import nz.co.warehouseandroidtest.ui.components.formatProductDescription
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
private fun ProductErrorView(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(WarehouseSpacing.md)
        ) {
            Text(
                text = "Oops! Something went wrong",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onBackground
            )
            Spacer(modifier = Modifier.height(WarehouseSpacing.xs))
            Spacer(modifier = Modifier.height(WarehouseSpacing.md))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(WarehouseSpacing.md),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
            ) {
                Text("Retry", color = Color.White)
            }
        }
    }
}

@Composable
private fun ProductDetailLoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colors.primary)
            Spacer(modifier = Modifier.height(WarehouseSpacing.sm))
            Text(
                text = "Loading product details...",
                color = MaterialTheme.colors.onBackground
            )
        }
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
                is ProductDetailUiState.Success -> {
                    val product = state.product
                    val imageUrl = product.imageUrls.firstOrNull() ?: product.productImageUrl
                    val description = remember(product.productDescription) {
                        formatProductDescription(
                            description = product.productDescription,
                            productId = product.productId
                        )
                    }
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                        Text(text = description, style = MaterialTheme.typography.body1, color = MaterialTheme.colors.onBackground)
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
                is ProductDetailUiState.Error -> ProductErrorView(
                    message = state.message,
                    onRetry = { viewModel.loadProductDetail(productId) }
                )
                ProductDetailUiState.Loading,
                ProductDetailUiState.Idle -> ProductDetailLoadingView()
            }
        }
    }
}
