package fumi.day.literalmemo.domain.model

data class Memo(
    val fileName: String,   // e.g., 20260321_143000.md
    val content: String,
    val updatedAt: Long     // file's lastModified timestamp
)

fun Memo.firstLine(): String =
    content.lines().firstOrNull { it.isNotBlank() }?.trim() ?: "(empty memo)"

