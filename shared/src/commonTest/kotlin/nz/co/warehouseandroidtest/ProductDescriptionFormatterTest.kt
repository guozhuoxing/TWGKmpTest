package nz.co.warehouseandroidtest

import nz.co.warehouseandroidtest.ui.formatProductDescription
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductDescriptionFormatterTest {

    @Test
    fun formatProductDescription_whenPlainText_keepsOriginalText() {
        val result = formatProductDescription("Fresh milk")

        assertEquals("Fresh milk", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun formatProductDescription_whenHtml_rendersReadableTextAndBoldSpans() {
        val result = formatProductDescription(
            "<b>Important</b><br><br><ul><li>One</li><li>Two</li></ul>Home &amp; Garden"
        )

        assertEquals("Important\n• One\n• Two\nHome & Garden", result.text)
        assertTrue(result.spanStyles.isNotEmpty())
    }
}
