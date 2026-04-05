package fumi.day.literalmemo.ui.list

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import fumi.day.literalmemo.domain.model.Memo
import fumi.day.literalmemo.domain.model.firstLine
import fumi.day.literalmemo.ui.theme.LocalAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoListScreen(
    onNavigateToEdit: (String?) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MemoListViewModel = hiltViewModel()
) {
    val memos by viewModel.memos.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val userPrefs by viewModel.userPrefs.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val appTheme = LocalAppTheme.current

    var memoToDelete by remember { mutableStateOf<Memo?>(null) }
    var inputText by remember { mutableStateOf("") }
    val isSearching = searchQuery.isNotBlank()
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler(enabled = isSearching) {
        inputText = ""
        viewModel.clearSearch()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            ) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    navigationIcon = {
                        if (userPrefs.gitHubEnabled) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(onClick = { viewModel.sync() }) {
                                    Icon(Icons.Default.Sync, contentDescription = "Sync")
                                }
                            }
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = if (isSearching) {
                                    "${memos.size} results"
                                } else {
                                    "${memos.size} memos"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (isSearching) {
                                    "searching: $searchQuery"
                                } else {
                                    getSubtitleText(memos, userPrefs.gitHubEnabled, userPrefs.lastSyncedAt, syncError)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.Transparent,
                modifier = Modifier.imePadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.executeSearch(inputText)
                                keyboardController?.hide()
                            }
                        ),
                        decorationBox = { innerTextField ->
                            if (inputText.isEmpty()) {
                                Text(
                                    text = "Search memos...",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }
        },
        floatingActionButtonPosition = if (userPrefs.fabOnLeft) FabPosition.Start else FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEdit(null) },
                containerColor = appTheme.accentColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "New memo")
            }
        }
    ) { paddingValues ->
        if (memos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSearching) {
                        "No memos found."
                    } else {
                        "No memos yet.\nTap + to create one."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                itemsIndexed(
                    items = memos,
                    key = { _, memo -> memo.fileName }
                ) { index, memo ->
                    SwipeableMemoItem(
                        memo = memo,
                        searchQuery = searchQuery,
                        accentColor = appTheme.accentColor,
                        isOdd = index % 2 == 0,
                        onClick = { onNavigateToEdit(memo.fileName) },
                        onSwipeToDelete = { memoToDelete = memo }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    memoToDelete?.let { memo ->
        AlertDialog(
            onDismissRequest = { memoToDelete = null },
            title = { Text("Delete?") },
            text = { Text("This memo will be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMemo(memo.fileName)
                        memoToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { memoToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableMemoItem(
    memo: Memo,
    searchQuery: String,
    accentColor: Color,
    isOdd: Boolean,
    onClick: () -> Unit,
    onSwipeToDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onSwipeToDelete()
                false
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    else -> Color.Transparent
                },
                label = "swipe_color"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
            )
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        MemoItem(
            memo = memo,
            searchQuery = searchQuery,
            accentColor = accentColor,
            isOdd = isOdd,
            onClick = onClick
        )
    }
}

@Composable
private fun MemoItem(
    memo: Memo,
    searchQuery: String,
    accentColor: Color,
    isOdd: Boolean,
    onClick: () -> Unit
) {
    val isSearching = searchQuery.isNotBlank()
    val appTheme = LocalAppTheme.current
    val lineHeight = (appTheme.fontSize * 1.8f).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isOdd) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = memo.firstLine(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = appTheme.fontFamily
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatDate(memo.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSearching) {
                Spacer(modifier = Modifier.height(4.dp))
                HighlightedSnippet(
                    content = memo.content,
                    query = searchQuery,
                    accentColor = accentColor
                )
            }
        }
    }
}

@Composable
private fun HighlightedSnippet(
    content: String,
    query: String,
    accentColor: Color
) {
    val appTheme = LocalAppTheme.current
    val snippetLength = 60
    val index = content.indexOf(query, ignoreCase = true)
    if (index == -1) return

    val start = (index - snippetLength / 2).coerceAtLeast(0)
    val end = (index + query.length + snippetLength / 2).coerceAtMost(content.length)
    val snippet = content.substring(start, end)
        .replace('\n', ' ')
        .let { if (start > 0) "...$it" else it }
        .let { if (end < content.length) "$it..." else it }

    val queryStartInSnippet = snippet.indexOf(query, ignoreCase = true)
    if (queryStartInSnippet == -1) return

    val annotatedString = buildAnnotatedString {
        append(snippet.substring(0, queryStartInSnippet))
        withStyle(
            style = SpanStyle(
                fontWeight = FontWeight.Bold,
                background = accentColor.copy(alpha = 0.3f)
            )
        ) {
            append(snippet.substring(queryStartInSnippet, queryStartInSnippet + query.length))
        }
        append(snippet.substring(queryStartInSnippet + query.length))
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = appTheme.fontFamily
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

private fun formatDate(timestamp: Long): String {
    return dateFormatter.format(Date(timestamp))
}

private fun getSubtitleText(memos: List<Memo>, gitHubEnabled: Boolean, lastSyncedAt: Long?, syncError: String?): String {
    return when {
        memos.isEmpty() -> "no memos yet"
        gitHubEnabled && syncError != null -> "Sync failed: $syncError"
        gitHubEnabled && lastSyncedAt != null -> "synced ${formatDate(lastSyncedAt)}"
        else -> "updated ${formatDate(memos.maxOf { it.updatedAt })}"
    }
}
