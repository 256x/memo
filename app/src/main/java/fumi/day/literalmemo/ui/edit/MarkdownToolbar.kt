package fumi.day.literalmemo.ui.edit

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

data class ToolbarAction(
    val label: String,
    val prefix: String,
    val suffix: String = "",
    val isLinePrefix: Boolean = false
)

val markdownActions = listOf(
    ToolbarAction("B", "**", "**"),
    ToolbarAction("I", "_", "_"),
    ToolbarAction("S", "~~", "~~"),
    ToolbarAction("H1", "# ", isLinePrefix = true),
    ToolbarAction("H2", "## ", isLinePrefix = true),
    ToolbarAction("H3", "### ", isLinePrefix = true),
    ToolbarAction("List", "- ", isLinePrefix = true),
    ToolbarAction("1.", "1. ", isLinePrefix = true),
    ToolbarAction("[ ]", "- [ ] ", isLinePrefix = true),
    ToolbarAction(">", "> ", isLinePrefix = true),
    ToolbarAction("`", "`", "`"),
    ToolbarAction("```", "```\n", "\n```"),
    ToolbarAction("---", "---", isLinePrefix = true),
    ToolbarAction("[link]", "[", "](url)")
)

@Composable
fun MarkdownToolbar(
    onActionClick: (ToolbarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = 2.dp
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(markdownActions) { action ->
                FilterChip(
                    selected = false,
                    onClick = { onActionClick(action) },
                    label = {
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}
