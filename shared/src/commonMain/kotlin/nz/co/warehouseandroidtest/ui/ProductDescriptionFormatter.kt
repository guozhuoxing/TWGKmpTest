package nz.co.warehouseandroidtest.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

// ProductDescriptionFormatter converts backend product descriptions into a Compose-friendly
// AnnotatedString so the detail screen can render both plain text and HTML-rich product text.
// It detects HTML, normalizes spacing, decodes common HTML entities, and applies bold/list styling.
private val htmlTagRegex = Regex("<[^>]+>")
private val htmlEntityRegex = Regex("&[A-Za-z#0-9]+;")
private val htmlTokenRegex = Regex("(?s)<[^>]+>|[^<]+")
private val whitespaceRegex = Regex("\\s+")

// Formats the raw description from the backend.
// If the value is empty, we show a fallback message.
// If it looks like HTML, we parse and render it as readable text instead of raw markup.
internal fun formatProductDescription(description: String?): AnnotatedString {
    val value = description?.trim().orEmpty()
    if (value.isEmpty()) return AnnotatedString("No description available.")
    return if (looksLikeHtml(value)) value.toAnnotatedHtmlDescription() else AnnotatedString(value)
}

internal fun looksLikeHtml(value: String): Boolean {
    return htmlTagRegex.containsMatchIn(value) || htmlEntityRegex.containsMatchIn(value)
}

private fun String.toAnnotatedHtmlDescription(): AnnotatedString = buildAnnotatedString {
    var boldDepth = 0

    htmlTokenRegex.findAll(this@toAnnotatedHtmlDescription).forEach { match ->
        val token = match.value
        if (token.startsWith("<")) {
            when (token.lowercase()) {
                "<b>", "<strong>" -> boldDepth += 1
                "</b>", "</strong>" -> boldDepth = (boldDepth - 1).coerceAtLeast(0)
                "<br>", "<br/>", "<br />" -> appendLineBreak()
                "<p>", "<div>", "<ul>" -> appendParagraphBreak()
                "</p>", "</div>", "</ul>" -> appendParagraphBreak()
                "<li>" -> {
                    appendLineBreak()
                    append("• ")
                }
                "</li>" -> appendLineBreak()
            }
        } else {
            appendNormalizedHtmlText(
                text = token.decodeHtmlEntities(),
                isBold = boldDepth > 0
            )
        }
    }
}

private fun AnnotatedString.Builder.appendNormalizedHtmlText(text: String, isBold: Boolean) {
    val normalized = text.replace(whitespaceRegex, " ").trim()
    if (normalized.isEmpty()) return

    if (needsLeadingSpace(normalized)) {
        append(" ")
    }

    if (isBold) {
        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
        append(normalized)
        pop()
    } else {
        append(normalized)
    }
}

private fun AnnotatedString.Builder.appendLineBreak() {
    if (length == 0) return
    if (lastCharOrNull() != '\n') append("\n")
}

private fun AnnotatedString.Builder.appendParagraphBreak() {
    if (length == 0) return
    if (lastCharOrNull() == '\n') return
    append("\n\n")
}

private fun AnnotatedString.Builder.needsLeadingSpace(nextText: String): Boolean {
    if (length == 0) return false

    val lastChar = lastCharOrNull() ?: return false
    if (lastChar.isWhitespace() || lastChar == '\n') return false

    val nextFirstChar = nextText.first()
    return nextFirstChar !in setOf('.', ',', '!', '?', ';', ':', ')')
}

private fun AnnotatedString.Builder.lastCharOrNull(): Char? {
    if (length == 0) return null
    return toAnnotatedString().text[length - 1]
}

private fun String.decodeHtmlEntities(): String {
    return this
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
}
