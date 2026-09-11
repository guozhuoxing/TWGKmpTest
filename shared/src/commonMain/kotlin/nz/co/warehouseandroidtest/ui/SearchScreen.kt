package nz.co.warehouseandroidtest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import nz.co.warehouseandroidtest.data.Product
import nz.co.warehouseandroidtest.ui.theme.WarehouseSpacing
import nz.co.warehouseandroidtest.viewmodel.SearchUiState
import nz.co.warehouseandroidtest.viewmodel.SearchViewModelContract

@Composable
fun SearchScreen(viewModel: SearchViewModelContract, onProductClick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(WarehouseSpacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(WarehouseSpacing.md),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colors.primary,
                    textColor = MaterialTheme.colors.onSurface
                ),
                placeholder = { Text("Search products...") }
            )
            Button(
                onClick = { viewModel.search(query) },
                modifier = Modifier.padding(start = WarehouseSpacing.xs),
                shape = RoundedCornerShape(WarehouseSpacing.md),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
            ) {
                Text("Search", color = MaterialTheme.colors.onPrimary)
            }
        }

        Spacer(modifier = Modifier.height(WarehouseSpacing.md))

        when (val state = uiState) {
            is SearchUiState.Loading -> SearchLoadingView(modifier = Modifier.fillMaxSize())
            is SearchUiState.Success -> {
                if (state.products.isEmpty()) {
                    Text(
                        text = "No products found for this search.",
                        modifier = Modifier.padding(top = WarehouseSpacing.md),
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.75f)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(WarehouseSpacing.xs)) {
                        items(state.products) { product ->
                            ProductItem(product, onProductClick)
                        }
                    }
                }
            }
            is SearchUiState.Error -> Text("Error: ${state.message}", color = MaterialTheme.colors.error)
            else -> Text("Enter a search term to begin.", color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun SearchLoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colors.primary)
            Spacer(modifier = Modifier.height(WarehouseSpacing.sm))
            Text("Loading products...", color = MaterialTheme.colors.onBackground)
        }
    }
}

@Composable
private fun DefaultProductImage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(WarehouseSpacing.sm))
            .background(MaterialTheme.colors.primary.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No Image",
            color = MaterialTheme.colors.primary.copy(alpha = 0.65f),
            style = MaterialTheme.typography.caption
        )
    }
}

@Composable
fun ProductItem(product: Product, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { product.productId?.let { onClick(it) } },
        elevation = WarehouseSpacing.xxs,
        shape = RoundedCornerShape(WarehouseSpacing.md),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Row(modifier = Modifier.padding(WarehouseSpacing.md), verticalAlignment = Alignment.CenterVertically) {
            val imageUrl = product.imageUrls.firstOrNull() ?: product.productImageUrl
            val imageModifier = Modifier.size(72.dp)
            if (!imageUrl.isNullOrEmpty()) {
                KamelImage(
                    resource = asyncPainterResource(data = imageUrl),
                    contentDescription = product.productName,
                    modifier = imageModifier,
                    contentScale = ContentScale.Fit,
                    onFailure = {
                        DefaultProductImage(modifier = imageModifier)
                    }
                )
            } else {
                DefaultProductImage(modifier = imageModifier)
            }
            Spacer(modifier = Modifier.width(WarehouseSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.productName ?: "Unknown",
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onSurface
                )
                Text(
                    text = "Price: $${product.priceInfo?.price ?: 0.0}",
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.primaryVariant
                )
            }
        }
    }
}
