package fumi.day.literalmemo.domain

/** The text and cursor position after a list marker was carried over or dropped. */
data class Continuation(val text: String, val cursor: Int)

/**
 * `- item`, `* item`, `+ item`, `1. item`, `1) item`, each optionally followed by a `[ ]` box.
 * Indentation, the marker's own spacing and the ordered-list delimiter are all captured so the
 * continued line looks exactly like the one above it.
 */
private val LIST_ITEM = Regex("""^([ \t]*)(?:([-*+])|(\d+)([.)]))([ \t]+)(\[[ xX]][ \t]+)?(.*)$""")

/**
 * Continues a markdown list when the newline at [cursor] was just typed.
 *
 * The line above is repeated as an empty item — an ordered marker counts up, a checked box comes
 * back unchecked. Pressing enter on an item that has no content instead drops the marker, which is
 * how a list is left. Returns null when the line above is not a list item, leaving the plain
 * newline alone.
 */
fun continueListOnNewline(text: String, cursor: Int): Continuation? {
    if (cursor <= 0 || cursor > text.length || text[cursor - 1] != '\n') return null

    val lineStart = if (cursor >= 2) text.lastIndexOf('\n', cursor - 2) + 1 else 0
    val match = LIST_ITEM.matchEntire(text.substring(lineStart, cursor - 1)) ?: return null

    val (indent, bullet, number, delimiter, spacing, checkbox, content) = match.destructured

    // An item left empty means the list is over: take the marker and the newline back out.
    if (content.isBlank()) {
        return Continuation(text.removeRange(lineStart, cursor), lineStart)
    }

    val marker = buildString {
        append(indent)
        if (bullet.isNotEmpty()) append(bullet) else append(number.toInt() + 1).append(delimiter)
        append(spacing)
        if (checkbox.isNotEmpty()) append("[ ] ")
    }
    return Continuation(
        text.substring(0, cursor) + marker + text.substring(cursor),
        cursor + marker.length
    )
}
