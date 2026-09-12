package nz.co.warehouseandroidtest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
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
import nz.co.warehouseandroidtest.data.Product
import nz.co.warehouseandroidtest.ui.theme.WarehouseSpacing
import nz.co.warehouseandroidtest.viewmodel.SearchUiState
import nz.co.warehouseandroidtest.viewmodel.SearchViewModel
import nz.co.warehouseandroidtest.viewmodel.SearchViewModelContract

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SearchScreen(viewModel: SearchViewModelContract, onProductClick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initLogin()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(WarehouseSpacing.md)
    ) {
        SearchBar(
            query = query,
            onQueryChange = { query = it },
            onSearch = { viewModel.search(query) }
        )

        Spacer(modifier = Modifier.height(WarehouseSpacing.md))

        when (val state = uiState) {
            is SearchUiState.Loading -> {
                LoadingState(modifier = Modifier.fillMaxSize())
            }
            is SearchUiState.LoadingMore, is SearchUiState.Success -> {
                val products = when (state) {
                    is SearchUiState.LoadingMore -> state.products
                    is SearchUiState.Success -> state.products
                    else -> emptyList()
                }
                val isLoading = state is SearchUiState.LoadingMore

                if (products.isEmpty()) {
                    EmptyState()
                } else {
                    ProductList(
                        products = products,
                        isLoading = isLoading,
                        onLoadMore = { viewModel.loadMore() },
                        onProductClick = onProductClick
                    )
                }
            }
            is SearchUiState.Error -> SearchErrorView(message = state.message)
            else -> EmptyState(message = "Enter a search term to begin.")
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
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
            onClick = onSearch,
            modifier = Modifier.padding(start = WarehouseSpacing.xs),
            shape = RoundedCornerShape(WarehouseSpacing.md),
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
        ) {
            Text("Search", color = MaterialTheme.colors.onPrimary)
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
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
private fun EmptyState(message: String = "No products found for this search.") {
    Text(
        text = message,
        modifier = Modifier.padding(top = WarehouseSpacing.md),
        color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
    )
}

@Composable
private fun ProductList(
    products: List<Product>,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    onProductClick: (String) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, isLoading) {
        snapshotFlow {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = products.size + (if (isLoading) 1 else 0)
            lastVisibleIndex to totalItems
        }
            .collect { (lastIndex, total) ->
                if (lastIndex >= total - 2 && !isLoading && products.isNotEmpty()) {
                    onLoadMore()
                }
            }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(WarehouseSpacing.xs),
        state = listState
    ) {
        items(products.size) { index ->
            ProductItem(products[index], onProductClick)
        }
        if (isLoading) {
            item(key = "loading_footer") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = WarehouseSpacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colors.primary)
                }
            }
        }
    }
}

@Composable
private fun SearchErrorView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = WarehouseSpacing.md),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(WarehouseSpacing.md),
            color = MaterialTheme.colors.surface,
            elevation = WarehouseSpacing.xxs
        ) {
            Column(
                modifier = Modifier.padding(WarehouseSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Something went wrong",
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onSurface
                )
                Spacer(modifier = Modifier.height(WarehouseSpacing.xs))
                Text(
                    text = message,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.body2
                )
            }
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
        Icon(
            imageVector = Icons.Filled.Image,
            contentDescription = "Placeholder image",
            tint = MaterialTheme.colors.primary.copy(alpha = 0.65f),
            modifier = Modifier.size(28.dp)
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
                    onLoading = {
                        DefaultProductImage(modifier = imageModifier)
                    },
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
