package com.deepresearch.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/**
 * Simple Markdown renderer for Compose.
 * Handles: headings, bold, italic, code, links, lists, blockquotes, horizontal rules.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val headlineSmallSize = MaterialTheme.typography.headlineSmall.fontSize
    val headlineMediumSize = MaterialTheme.typography.headlineMedium.fontSize
    val headlineLargeSize = MaterialTheme.typography.headlineLarge.fontSize

    val styledText = remember(markdown) {
        buildMarkdownAnnotatedString(markdown, primaryColor, onSurfaceVariant,
            headlineSmallSize, headlineMediumSize, headlineLargeSize)
    }
    Text(
        text = styledText,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = maxLines
    )
}

private fun buildMarkdownAnnotatedString(
    markdown: String,
    primaryColor: Color,
    onSurfaceVariant: Color,
    headlineSmallSize: androidx.compose.ui.unit.TextUnit,
    headlineMediumSize: androidx.compose.ui.unit.TextUnit,
    headlineLargeSize: androidx.compose.ui.unit.TextUnit
): AnnotatedString {
    val builder = AnnotatedString.Builder()

    val lines = markdown.split("\n")
    for (line in lines) {
        when {
            line.startsWith("### ") -> {
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = headlineSmallSize)) {
                    append(line.removePrefix("### "))
                }
                builder.append("\n\n")
            }
            line.startsWith("## ") -> {
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = headlineMediumSize)) {
                    append(line.removePrefix("## "))
                }
                builder.append("\n\n")
            }
            line.startsWith("# ") -> {
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = headlineLargeSize)) {
                    append(line.removePrefix("# "))
                }
                builder.append("\n\n")
            }
            line.trimStart().startsWith("---") || line.trimStart().startsWith("***") || line.trimStart().startsWith("___") -> {
                builder.append("─────────────────────────\n\n")
            }
            line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                val content = line.trimStart().removePrefix("- ").removePrefix("* ")
                builder.append("  \u2022  ")
                processInlineMarkdown(builder, content, primaryColor)
                builder.append("\n")
            }
            line.trimStart().matches(Regex("""^\d+\..*""")) -> {
                val content = line.trimStart().replaceFirst(Regex("""^\d+\.\s*"""), "")
                builder.append(line.trimStart().substringBefore("."))
                builder.append(".  ")
                processInlineMarkdown(builder, content, primaryColor)
                builder.append("\n")
            }
            line.trimStart().startsWith("> ") -> {
                builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = onSurfaceVariant)) {
                    builder.append(line.trimStart().removePrefix("> "))
                }
                builder.append("\n\n")
            }
            line.isBlank() -> {
                builder.append("\n")
            }
            else -> {
                processInlineMarkdown(builder, line, primaryColor)
                builder.append("\n")
            }
        }
    }

    return builder.toAnnotatedString()
}

private fun processInlineMarkdown(
    builder: AnnotatedString.Builder,
    text: String,
    primaryColor: Color
) {
    var remaining = text

    while (remaining.isNotEmpty()) {
        val boldStart = remaining.indexOf("**")
        val italicStart = remaining.indexOf("*")
        val codeStart = remaining.indexOf("`")
        val linkStart = remaining.indexOf("[")

        val nextSpecial = findNextSpecial(remaining, boldStart, italicStart, codeStart, linkStart)

        if (nextSpecial == null) {
            builder.append(remaining)
            break
        }

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
                    builder.withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = primaryColor)) {
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
                    builder.withStyle(SpanStyle(textDecoration = TextDecoration.Underline, color = primaryColor)) {
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
