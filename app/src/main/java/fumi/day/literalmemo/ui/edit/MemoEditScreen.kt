package fumi.day.literalmemo.ui.edit

import android.graphics.Typeface
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.hilt.navigation.compose.hiltViewModel
import fumi.day.literalmemo.R
import fumi.day.literalmemo.data.prefs.AppFont
import fumi.day.literalmemo.ui.theme.LocalAppTheme
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: MemoEditViewModel = hiltViewModel()
) {
    val content by viewModel.content.collectAsState()
    val isPreviewMode by viewModel.isPreviewMode.collectAsState()
    val currentFileName by viewModel.currentFileName.collectAsState()
    val userPrefs by viewModel.userPrefs.collectAsState()
    val appTheme = LocalAppTheme.current

    var textFieldValue by remember { mutableStateOf(TextFieldValue(content)) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(content) {
        if (textFieldValue.text != content) {
            textFieldValue = TextFieldValue(content, TextRange(content.length))
        }
    }

    val backgroundColor = if (appTheme.backgroundColor != Color.Unspecified) {
        appTheme.backgroundColor
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (appTheme.textColor != Color.Unspecified) {
        appTheme.textColor
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.save()
        }
    }

    BackHandler {
        viewModel.save()
        onNavigateBack()
    }

    LaunchedEffect(isPreviewMode) {
        if (!isPreviewMode) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.save()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { },
                actions = {
                    TextButton(onClick = { viewModel.togglePreviewMode() }) {
                        Text(if (isPreviewMode) "Edit" else "Preview")
                    }
                    if (currentFileName != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(backgroundColor)
            ) {
                if (isPreviewMode) {
                    MarkdownPreview(
                        content = content,
                        textColor = textColor,
                        backgroundColor = backgroundColor,
                        fontSize = appTheme.fontSize,
                        fontFamily = userPrefs.font,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it
                            viewModel.updateContent(it.text)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            color = textColor,
                            fontSize = appTheme.fontSize.sp,
                            fontFamily = appTheme.fontFamily
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Box {
                                if (textFieldValue.text.isEmpty()) {
                                    Text(
                                        text = "Start writing...",
                                        style = TextStyle(
                                            color = textColor.copy(alpha = 0.5f),
                                            fontSize = appTheme.fontSize.sp,
                                            fontFamily = appTheme.fontFamily
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }

            if (!isPreviewMode) {
                MarkdownToolbar(
                    onActionClick = { action ->
                        textFieldValue = applyToolbarAction(textFieldValue, action)
                        viewModel.updateContent(textFieldValue.text)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete?") },
            text = { Text("This memo will be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteMemo(onNavigateBack)
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun applyToolbarAction(textFieldValue: TextFieldValue, action: ToolbarAction): TextFieldValue {
    val text = textFieldValue.text
    val selection = textFieldValue.selection
    val start = selection.min
    val end = selection.max
    val selectedText = text.substring(start, end)

    return if (action.isLinePrefix) {
        val lineStart = text.lastIndexOf('\n', start - 1) + 1
        val newText = text.substring(0, lineStart) + action.prefix + text.substring(lineStart)
        val newCursorPos = start + action.prefix.length
        TextFieldValue(newText, TextRange(newCursorPos))
    } else {
        val newText = text.substring(0, start) + action.prefix + selectedText + action.suffix + text.substring(end)
        val newCursorPos = if (selectedText.isEmpty()) {
            start + action.prefix.length
        } else {
            start + action.prefix.length + selectedText.length + action.suffix.length
        }
        TextFieldValue(newText, TextRange(newCursorPos))
    }
}

@Composable
private fun MarkdownPreview(
    content: String,
    textColor: Color,
    backgroundColor: Color,
    fontSize: Float,
    fontFamily: AppFont,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val textColorInt = textColor.toArgb()
    val backgroundColorInt = backgroundColor.toArgb()

    val scopeOneTypeface = remember(context) {
        ResourcesCompat.getFont(context, R.font.scopeone)
    }

    val markwon = remember(context, textColorInt, backgroundColorInt) {
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(
                textColorInt,
                textColorInt,
                backgroundColorInt
            ))
            .build()
    }

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(textColorInt)
                textSize = fontSize
                setPadding(48, 48, 48, 48)
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { textView ->
            textView.setTextColor(textColorInt)
            textView.textSize = fontSize
            textView.typeface = when (fontFamily) {
                AppFont.SERIF -> Typeface.SERIF
                AppFont.MONOSPACE -> Typeface.MONOSPACE
                AppFont.DEFAULT -> Typeface.DEFAULT
                AppFont.SCOPE_ONE -> scopeOneTypeface
            }
            markwon.setMarkdown(textView, content)
        },
        modifier = modifier.verticalScroll(rememberScrollState())
    )
}
