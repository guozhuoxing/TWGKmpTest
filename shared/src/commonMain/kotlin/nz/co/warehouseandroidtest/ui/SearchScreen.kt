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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import nz.co.warehouseandroidtest.data.Product
import nz.co.warehouseandroidtest.viewmodel.SearchUiState
import nz.co.warehouseandroidtest.viewmodel.SearchViewModelContract

@Composable
fun SearchScreen(viewModel: SearchViewModelContract, onProductClick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search products...") }
            )
            Button(onClick = { viewModel.search(query) }, modifier = Modifier.padding(start = 8.dp)) {
                Text("Search")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val state = uiState) {
            is SearchUiState.Loading -> SearchLoadingView(modifier = Modifier.fillMaxSize())
            is SearchUiState.Success -> {
                if (state.products.isEmpty()) {
                    Text("No products found for this search.", modifier = Modifier.padding(top = 16.dp))
                } else {
                    LazyColumn {
                        items(state.products) { product ->
                            ProductItem(product, onProductClick)
                        }
                    }
                }
            }
            is SearchUiState.Error -> Text("Error: ${state.message}", color = MaterialTheme.colors.error)
            else -> Text("Enter a search term to begin.")
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
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text("Loading products...")
        }
    }
}

@Composable
private fun DefaultProductImage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
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
fun ProductItem(product: Product, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { product.productId?.let { onClick(it) } },
        elevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val imageUrl = product.imageUrls.firstOrNull() ?: product.productImageUrl
            val imageModifier = Modifier.size(64.dp)
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
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = product.productName ?: "Unknown", style = MaterialTheme.typography.h6)
                Text(text = "Price: $${product.priceInfo?.price ?: 0.0}", style = MaterialTheme.typography.body1)
            }
        }
    }
}
