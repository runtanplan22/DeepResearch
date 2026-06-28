package com.deepresearch.app.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/**
 * Simple Markdown renderer for Compose.
 * Handles: headings, bold, italic, code, links, lists, blockquotes, horizontal rules.
 * Images are not rendered inline (handled separately via Coil).
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE
) {
    val styledText = buildMarkdownAnnotatedString(markdown)
    Text(
        text = styledText,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = maxLines
    )
}

@Composable
fun buildMarkdownAnnotatedString(markdown: String): AnnotatedString {
    val builder = AnnotatedString.Builder()

    // Process line by line
    val lines = markdown.split("\n")
    for (i in lines.indices) {
        val line = lines[i]

        when {
            // Headings
            line.startsWith("### ") -> {
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.headlineSmall.fontSize)) {
                    append(line.removePrefix("### "))
                }
                builder.append("\n\n")
            }
            line.startsWith("## ") -> {
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.headlineMedium.fontSize)) {
                    append(line.removePrefix("## "))
                }
                builder.append("\n\n")
            }
            line.startsWith("# ") -> {
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.headlineLarge.fontSize)) {
                    append(line.removePrefix("# "))
                }
                builder.append("\n\n")
            }
            // Horizontal rule
            line.trimStart().startsWith("---") || line.trimStart().startsWith("***") || line.trimStart().startsWith("___") -> {
                builder.append("─────────────────────────\n\n")
            }
            // Unordered list
            line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                val content = line.trimStart().removePrefix("- ").removePrefix("* ")
                builder.append("  •  ")
                processInlineMarkdown(builder, content)
                builder.append("\n")
            }
            // Ordered list
            line.trimStart().matches(Regex("""^\d+\..*""")) -> {
                val content = line.trimStart().replaceFirst(Regex("""^\d+\.\s*"""), "")
                builder.append(line.trimStart().substringBefore("."))
                builder.append(".  ")
                processInlineMarkdown(builder, content)
                builder.append("\n")
            }
            // Blockquote
            line.trimStart().startsWith("> ") -> {
                builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    builder.append(line.trimStart().removePrefix("> "))
                }
                builder.append("\n\n")
            }
            // Empty line
            line.isBlank() -> {
                builder.append("\n")
            }
            // Regular text
            else -> {
                processInlineMarkdown(builder, line)
                builder.append("\n")
            }
        }
    }

    return builder.toAnnotatedString()
}

/**
 * Process inline markdown: bold, italic, code, links.
 */
private fun processInlineMarkdown(builder: AnnotatedString.Builder, text: String) {
    var remaining = text

    while (remaining.isNotEmpty()) {
        // Find the earliest occurrence of any markdown pattern
        val boldStart = remaining.indexOf("**")
        val italicStart = remaining.indexOf("*")
        val codeStart = remaining.indexOf("`")
        val linkStart = remaining.indexOf("[")

        // Skip if current position is at a bold/italic start that matches
        val nextSpecial = findNextSpecial(remaining, boldStart, italicStart, codeStart, linkStart)

        if (nextSpecial == null) {
            // No more markdown, append remaining text
            builder.append(remaining)
            break
        }

        // Append text before the markdown
        builder.append(remaining.substring(0, nextSpecial.startIndex))

        when (nextSpecial.type) {
            SpecialType.BOLD -> {
                val end = remaining.indexOf("**", nextSpecial.startIndex + 2)
                if (end != -1) {
                    builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(remaining.substring(nextSpecial.startIndex + 2, end))
                    }
                    remaining = remaining.substring(end + 2)
                } else {
                    builder.append("**")
                    remaining = remaining.substring(nextSpecial.startIndex + 2)
                }
            }
            SpecialType.ITALIC -> {
                // Make sure this isn't a bold delimiter
                if (remaining.length > nextSpecial.startIndex + 1 && remaining[nextSpecial.startIndex + 1] == '*') {
                    builder.append("*")
                    remaining = remaining.substring(nextSpecial.startIndex + 1)
                } else {
                    val end = remaining.indexOf("*", nextSpecial.startIndex + 1)
                    if (end != -1) {
                        builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(remaining.substring(nextSpecial.startIndex + 1, end))
                        }
                        remaining = remaining.substring(end + 1)
                    } else {
                        builder.append("*")
                        remaining = remaining.substring(nextSpecial.startIndex + 1)
                    }
                }
            }
            SpecialType.CODE -> {
                val end = remaining.indexOf("`", nextSpecial.startIndex + 1)
                if (end != -1) {
                    builder.withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)) {
                        append(remaining.substring(nextSpecial.startIndex + 1, end))
                    }
                    remaining = remaining.substring(end + 1)
                } else {
                    builder.append("`")
                    remaining = remaining.substring(nextSpecial.startIndex + 1)
                }
            }
            SpecialType.LINK -> {
                val closeBracket = remaining.indexOf("](", nextSpecial.startIndex + 1)
                val endParen = if (closeBracket != -1) remaining.indexOf(")", closeBracket + 2) else -1
                if (closeBracket != -1 && endParen != -1) {
                    val linkText = remaining.substring(nextSpecial.startIndex + 1, closeBracket)
                    val url = remaining.substring(closeBracket + 2, endParen)
                    builder.withStyle(SpanStyle(textDecoration = TextDecoration.Underline, color = MaterialTheme.colorScheme.primary)) {
                        append(linkText)
                    }
                    builder.addStringAnnotation("url", url, builder.length - linkText.length, builder.length)
                    remaining = remaining.substring(endParen + 1)
                } else {
                    builder.append("[")
                    remaining = remaining.substring(nextSpecial.startIndex + 1)
                }
            }
        }
    }
}

private enum class SpecialType { BOLD, ITALIC, CODE, LINK }

private data class SpecialIndex(val type: SpecialType, val startIndex: Int)

private fun findNextSpecial(
    text: String,
    boldStart: Int,
    italicStart: Int,
    codeStart: Int,
    linkStart: Int
): SpecialIndex? {
    val candidates = mutableListOf<SpecialIndex>()

    if (boldStart != -1) candidates.add(SpecialIndex(SpecialType.BOLD, boldStart))
    if (italicStart != -1) candidates.add(SpecialIndex(SpecialType.ITALIC, italicStart))
    if (codeStart != -1) candidates.add(SpecialIndex(SpecialType.CODE, codeStart))
    if (linkStart != -1) candidates.add(SpecialIndex(SpecialType.LINK, linkStart))

    return candidates.minByOrNull { it.startIndex }
}
