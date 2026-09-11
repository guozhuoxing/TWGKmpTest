package nz.co.warehouseandroidtest.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val WarehouseRed = Color(0xFFB3261E)
private val WarehouseRedDark = Color(0xFF8F1F17)
private val WarehouseRedLight = Color(0xFFFDE8E7)
private val WarehouseCream = Color(0xFFF7F3F2)
private val WarehouseText = Color(0xFF1E1E1E)
private val WarehouseMuted = Color(0xFF6A6A6A)

val WarehouseColors = lightColors(
    primary = WarehouseRed,
    primaryVariant = WarehouseRedDark,
    secondary = WarehouseRed,
    background = WarehouseCream,
    surface = Color.White,
    error = Color(0xFFC62828),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = WarehouseText,
    onSurface = WarehouseText,
    onError = Color.White
)

object WarehouseSpacing {
    val none = 0.dp
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
}

@Composable
fun WarehouseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = WarehouseColors,
        content = content
    )
}

val WarehouseColorPalette = object {
    val red = WarehouseRed
    val redDark = WarehouseRedDark
    val redLight = WarehouseRedLight
    val cream = WarehouseCream
    val text = WarehouseText
    val muted = WarehouseMuted
}
