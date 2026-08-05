package fumi.day.literalmemo.domain

/** Matches the `[ ]` / `[x]` marker of a task list item, capturing the state character. */
private val TASK_MARKER = Regex("""^\s*(?:[-*+]|\d+[.)])\s+\[([ xX])]""")

private const val BACKTICK_FENCE = "```"
private const val TILDE_FENCE = "~~~"

/**
 * Flips the [taskIndex]-th task list checkbox in [markdown], counting in document order so the
 * index lines up with the rendered task list spans. Fenced code blocks are skipped, since the
 * checkboxes they contain are shown as sample text rather than rendered as tasks.
 *
 * Returns null when there is no task at that index, leaving the memo untouched.
 */
fun toggleTaskAt(markdown: String, taskIndex: Int): String? {
    if (taskIndex < 0) return null

    var found = 0
    var inFence = false
    var lineStart = 0

    while (lineStart <= markdown.length) {
        val newline = markdown.indexOf('\n', lineStart)
        val lineEnd = if (newline == -1) markdown.length else newline
        val line = markdown.substring(lineStart, lineEnd)
        val trimmed = line.trimStart()

        when {
            trimmed.startsWith(BACKTICK_FENCE) || trimmed.startsWith(TILDE_FENCE) -> inFence = !inFence

            !inFence -> {
                val marker = TASK_MARKER.find(line)?.groups?.get(1)
                if (marker != null) {
                    if (found == taskIndex) {
                        val at = lineStart + marker.range.first
                        val flipped = if (marker.value.isBlank()) "x" else " "
                        return markdown.substring(0, at) + flipped + markdown.substring(at + 1)
                    }
                    found++
                }
            }
        }

        if (newline == -1) break
        lineStart = newline + 1
    }

    return null
}
