package com.example.appfinanceiro.feature.assistant

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.network.isAssistantError

/**
 * Presentation-only support for bold and simple lists; this is not an HTML or Markdown engine.
 * The original message remains untouched, and no links or executable annotations are created.
 */
fun assistantMessageText(message: AssistantMessage): AnnotatedString {
    if (message.role != "assistant" || isAssistantError(message.errorCode)) {
        return AnnotatedString(message.content)
    }

    val lines = message.content.replace("\r\n", "\n").replace('\r', '\n').split('\n')
    return buildAnnotatedString {
        lines.forEachIndexed { index, line ->
            if (index > 0) append('\n')
            val paragraphStart = length
            appendAssistantBold(line)
            if (isSimpleAssistantListItem(line) && length > paragraphStart) {
                addStyle(
                    ParagraphStyle(
                        textIndent = TextIndent(restLine = 24.sp),
                        lineHeight = 1.6.em
                    ),
                    paragraphStart,
                    length
                )
            }
        }
    }
}

private data class AssistantBoldRange(val opening: Int, val closing: Int)

/** One linear scan pairs clear, unescaped ** delimiters on a single line. */
private fun AnnotatedString.Builder.appendAssistantBold(line: String) {
    val ranges = mutableListOf<AssistantBoldRange>()
    var opening = -1
    var index = 0
    var backslashes = 0
    while (index < line.length) {
        if (line[index] == '\\') {
            backslashes++
            index++
            continue
        }
        if (line[index] != '*') {
            backslashes = 0
            index++
            continue
        }

        val runStart = index
        while (index < line.length && line[index] == '*') index++
        val runLength = index - runStart
        val escaped = backslashes % 2 != 0
        backslashes = 0
        if (runLength > 2) {
            // Unsupported runs must not become hidden formatting markers inside another pair.
            opening = -1
            continue
        }
        if (runLength != 2 || escaped) continue

        val before = line.getOrNull(runStart - 1)
        val after = line.getOrNull(index)
        val canOpen = after != null && !after.isWhitespace() &&
            (before == null || !before.isLetterOrDigit())
        val canClose = before != null && !before.isWhitespace() &&
            (after == null || !after.isLetterOrDigit())
        if (opening >= 0 && canClose && runStart > opening + 2) {
            ranges += AssistantBoldRange(opening, runStart)
            opening = -1
        } else if (canOpen) {
            opening = runStart
        }
    }

    var copiedUntil = 0
    ranges.forEach { range ->
        append(line.substring(copiedUntil, range.opening))
        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
        append(line.substring(range.opening + 2, range.closing))
        pop()
        copiedUntil = range.closing + 2
    }
    append(line.substring(copiedUntil))
}

private fun isSimpleAssistantListItem(line: String): Boolean {
    val start = line.indexOfFirst { !it.isWhitespace() }
    if (start < 0) return false
    var markerEnd = start
    if (line[start] == '-' || line[start] == '*') {
        markerEnd++
    } else {
        while (markerEnd < line.length && line[markerEnd] in '0'..'9') markerEnd++
        if (markerEnd == start || markerEnd >= line.length ||
            (line[markerEnd] != '.' && line[markerEnd] != ')')) return false
        markerEnd++
    }
    if (markerEnd >= line.length || !line[markerEnd].isWhitespace()) return false
    val body = line.substring(markerEnd).trimStart().removePrefix("**")
    if (body.isBlank()) return false
    // A minus before an amount belongs to the value, not to a list. Keep every marker visible.
    if (line[start] == '-' && (body.first() in '0'..'9' || body.startsWith("R$", true))) return false
    return true
}
