package fumi.day.literalmemo.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import fumi.day.literalmemo.BuildConfig
import fumi.day.literalmemo.data.prefs.AppFont
import fumi.day.literalmemo.ui.theme.LocalAppTheme
import fumi.day.literalmemo.ui.theme.parseColor
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val userPrefs by viewModel.userPrefs.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncResult by viewModel.syncResult.collectAsState()
    val appTheme = LocalAppTheme.current

    var showColorPicker by remember { mutableStateOf<ColorPickerTarget?>(null) }
    var showGitHubDialog by remember { mutableStateOf(false) }

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
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = { Text("Settings") }
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppearanceCard(
                userPrefs = userPrefs,
                onFontChange = viewModel::setFont,
                onFontSizeChange = viewModel::setFontSize,
                onColorClick = { target -> showColorPicker = target },
                onFabOnLeftChange = viewModel::setFabOnLeft
            )

            GitHubSyncCard(
                userPrefs = userPrefs,
                isSyncing = isSyncing,
                accentColor = appTheme.accentColor,
                onConnectClick = { showGitHubDialog = true },
                onSyncNowClick = viewModel::syncNow,
                onEditClick = { showGitHubDialog = true },
                onDisconnectClick = viewModel::disconnectGitHub
            )

            Text(
                text = "Literal Memo v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    showColorPicker?.let { target ->
        ColorPickerDialog(
            initialColor = when (target) {
                ColorPickerTarget.BACKGROUND -> parseColor(userPrefs.backgroundColorHex)
                ColorPickerTarget.TEXT -> parseColor(userPrefs.textColorHex)
                ColorPickerTarget.ACCENT -> parseColor(userPrefs.accentColorHex)
            },
            onColorSelected = { color ->
                val hex = colorToHex(color)
                when (target) {
                    ColorPickerTarget.BACKGROUND -> viewModel.setBackgroundColor(hex)
                    ColorPickerTarget.TEXT -> viewModel.setTextColor(hex)
                    ColorPickerTarget.ACCENT -> viewModel.setAccentColor(hex)
                }
                showColorPicker = null
            },
            onDismiss = { showColorPicker = null }
        )
    }

    if (showGitHubDialog) {
        GitHubSettingsDialog(
            initialToken = userPrefs.gitHubToken,
            initialRepo = userPrefs.gitHubRepo,
            onSave = { token, repo ->
                viewModel.saveGitHubConfig(token, repo)
                showGitHubDialog = false
            },
            onDismiss = { showGitHubDialog = false }
        )
    }

    // Sync progress dialog
    if (isSyncing) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Syncing...") },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text("Downloading and uploading files...")
                }
            },
            confirmButton = { }
        )
    }

    // Sync result dialog
    syncResult?.let { result ->
        if (!isSyncing) {
            AlertDialog(
                onDismissRequest = { viewModel.clearSyncResult() },
                title = { Text(if (result.errors.isEmpty()) "Sync Complete" else "Sync Error") },
                text = {
                    if (result.errors.isEmpty()) {
                        Text("Downloaded ${result.downloaded} files\nUploaded ${result.uploaded} files")
                    } else {
                        Text(result.errors.joinToString("\n"))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearSyncResult() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

enum class ColorPickerTarget {
    BACKGROUND, TEXT, ACCENT
}

@Composable
private fun AppearanceCard(
    userPrefs: fumi.day.literalmemo.data.prefs.UserPrefs,
    onFontChange: (AppFont) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onColorClick: (ColorPickerTarget) -> Unit,
    onFabOnLeftChange: (Boolean) -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppFont.entries.forEach { font ->
                    FilterChip(
                        selected = userPrefs.font == font,
                        onClick = { onFontChange(font) },
                        label = {
                            Text(
                                when (font) {
                                    AppFont.DEFAULT -> "Default"
                                    AppFont.SERIF -> "Serif"
                                    AppFont.MONOSPACE -> "Mono"
                                    AppFont.SCOPE_ONE -> "Scope"
                                }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Size", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = userPrefs.fontSize,
                    onValueChange = onFontSizeChange,
                    valueRange = 12f..24f,
                    steps = 5,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                Text(
                    text = "${userPrefs.fontSize.roundToInt()}sp",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ColorDot(
                    label = "BG",
                    color = parseColor(userPrefs.backgroundColorHex),
                    onClick = { onColorClick(ColorPickerTarget.BACKGROUND) }
                )
                ColorDot(
                    label = "Text",
                    color = parseColor(userPrefs.textColorHex),
                    onClick = { onColorClick(ColorPickerTarget.TEXT) }
                )
                ColorDot(
                    label = "Accent",
                    color = parseColor(userPrefs.accentColorHex),
                    onClick = { onColorClick(ColorPickerTarget.ACCENT) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Controls on left", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = userPrefs.fabOnLeft,
                    onCheckedChange = onFabOnLeftChange
                )
            }
        }
    }
}

@Composable
private fun ColorDot(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val displayColor = if (color == Color.Unspecified) {
        MaterialTheme.colorScheme.surface
    } else {
        color
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(displayColor)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var brightness by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(initialColor) {
        if (initialColor != Color.Unspecified) {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
            hue = hsv[0]
            saturation = hsv[1]
            brightness = hsv[2]
        }
    }

    val currentColor = Color.hsv(hue, saturation, brightness)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .pointerInput(hue) {
                            detectTapGestures { offset ->
                                saturation = (offset.x / size.width).coerceIn(0f, 1f)
                                brightness = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                            }
                        }
                        .pointerInput(hue) {
                            detectDragGestures { change, _ ->
                                saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                                brightness = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.White, Color.hsv(hue, 1f, 1f))
                            )
                        )
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black)
                            )
                        )

                        val cursorX = saturation * size.width
                        val cursorY = (1f - brightness) * size.height
                        drawCircle(
                            color = Color.White,
                            radius = 12f,
                            center = Offset(cursorX, cursorY)
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 10f,
                            center = Offset(cursorX, cursorY),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                hue = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                hue = (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val colors = (0..360 step 30).map { Color.hsv(it.toFloat(), 1f, 1f) }
                        drawRect(
                            brush = Brush.horizontalGradient(colors)
                        )

                        val cursorX = hue / 360f * size.width
                        drawCircle(
                            color = Color.White,
                            radius = 14f,
                            center = Offset(cursorX, size.height / 2)
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 12f,
                            center = Offset(cursorX, size.height / 2),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Text(
                        text = colorToHex(currentColor),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(currentColor) }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return String.format("#%06X", 0xFFFFFF and argb)
}

@Composable
private fun GitHubSyncCard(
    userPrefs: fumi.day.literalmemo.data.prefs.UserPrefs,
    isSyncing: Boolean,
    accentColor: Color,
    onConnectClick: () -> Unit,
    onSyncNowClick: () -> Unit,
    onEditClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "GitHub Sync",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!userPrefs.gitHubEnabled) {
                Button(
                    onClick = onConnectClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor
                    )
                ) {
                    Text("Connect GitHub")
                }
            } else {
                Text(
                    text = userPrefs.gitHubRepo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        OutlinedButton(onClick = onSyncNowClick) {
                            Text("Sync Now")
                        }
                    }
                    OutlinedButton(onClick = onEditClick) {
                        Text("Edit")
                    }
                    OutlinedButton(onClick = onDisconnectClick) {
                        Text("Disconnect")
                    }
                }

            }
        }
    }
}

@Composable
private fun GitHubSettingsDialog(
    initialToken: String,
    initialRepo: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var token by remember { mutableStateOf(initialToken) }
    var repo by remember { mutableStateOf(initialRepo) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GitHub Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Personal Access Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = repo,
                    onValueChange = { repo = it },
                    label = { Text("Repository (owner/repo)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(token, repo) },
                enabled = token.isNotBlank() && repo.contains("/")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
